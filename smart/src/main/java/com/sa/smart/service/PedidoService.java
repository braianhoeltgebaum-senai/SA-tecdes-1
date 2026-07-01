package com.sa.smart.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import com.sa.smart.dto.BlocoDTO;
import com.sa.smart.dto.LaminaDTO;
import com.sa.smart.dto.PedidoConfigDTO;
import com.sa.smart.dto.PedidoInfoDTO;
import com.sa.smart.enums.EnumCorBloco;
import com.sa.smart.model.Bloco;
import com.sa.smart.model.Estoque;
import com.sa.smart.model.Lamina;
import com.sa.smart.model.Pedido;
import com.sa.smart.repository.EstoqueRepository;
import com.sa.smart.repository.PedidoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            EstoqueRepository estoqueRepository) {
        this.pedidoRepository = pedidoRepository;
        this.estoqueRepository = estoqueRepository;
    }

    // ─── Mapeamento auxiliar ──────────────────────────────────────────────────
    private List<LaminaDTO> toLaminaDTOList(List<Lamina> laminas) {
        return laminas.stream()
                .map(l -> new LaminaDTO(
                        l.getId(),
                        l.getCor(),
                        l.getPadrao(),
                        l.getPosicaoNoBloco(),
                        l.getBloco().getIdBloco()))
                .collect(Collectors.toList());
    }

    // ─── CRUD básico ──────────────────────────────────────────────────────────
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    /**
     * Cria um novo pedido.
     *
     * Para cada bloco, busca a PRIMEIRA posição de estoque que contenha um bloco
     * da cor solicitada (ordenado por posicaoEstoque ASC), evitando reutilizar
     * posições já atribuídas a blocos anteriores do mesmo pedido.
     *
     * Após atribuir a posição ao bloco, marca o estoque como vazio (cor=0)
     * para que o robô saiba que foi o sistema quem reservou aquela saída.
     */
    @Transactional
    public Pedido criarPedido(Pedido pedido) {
        if (pedido.getTipoPedido() == 3 &&
                (pedido.getBlocos() == null || pedido.getBlocos().size() != 3)) {
            throw new RuntimeException("Pedidos triplos exigem exatamente 3 blocos.");
        }

        // Validação da cor da tampa (1-3)
        if (pedido.getCorTampa() == null || pedido.getCorTampa() < 1 || pedido.getCorTampa() > 3) {
            throw new RuntimeException("Cor da tampa inválida. Use 1, 2 ou 3.");
        }

        pedido.setStatusPedido(1); // Pendente

        if (pedido.getBlocos() != null) {
            // Rastreia as posições físicas já alocadas neste pedido para não
            // atribuir a mesma posição a dois blocos (ex.: triplo com 2 pretos)
            Set<Integer> posicoesJaUsadas = new HashSet<>();

            for (Bloco bloco : pedido.getBlocos()) {
                bloco.setPedido(pedido);
                bloco.setCriadoEm(LocalDateTime.now());

                Integer corBloco = bloco.getCorBloco();
                if (corBloco == null || corBloco == 0) {
                    throw new RuntimeException("Cor do bloco não informada ou inválida.");
                }

                // Busca TODAS as posições com a cor pedida (ordenadas pela posição física)
                // e pega a primeira que ainda não foi alocada neste pedido.
                Estoque estoque = estoqueRepository
                        .findByCorOrderByPosicaoEstoqueAsc(corBloco)
                        .stream()
                        .filter(e -> !posicoesJaUsadas.contains(e.getPosicaoEstoque()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "Nenhuma posição de estoque disponível com a cor: " + corBloco
                                        + ". Posições já usadas: " + posicoesJaUsadas));

                System.out.printf("[criarPedido] Bloco cor=%d → estoque id=%d, posicao=%d%n",
                        corBloco, estoque.getId(), estoque.getPosicaoEstoque());

                posicoesJaUsadas.add(estoque.getPosicaoEstoque());
                bloco.setEstoque(estoque);

                // Marca a posição como "vazia" no banco para não ser reatribuída
                // a outro pedido. O robô vai buscar o bloco nessa posição.
                estoque.setCor(0);
                estoqueRepository.save(estoque);
            }
        }

        return pedidoRepository.save(pedido);
    }

    public void atualizarStatusParaConcluido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        pedido.setStatusPedido(3);
        pedidoRepository.save(pedido);
        registrarNaExpedicao(pedido);
    }

    public Pedido atualizarParcial(Long id, Map<String, Object> campos) {
        Pedido pedidoAtual = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        campos.forEach((nomeCampo, valorCampo) -> {
            Field field = ReflectionUtils.findField(Pedido.class, nomeCampo);
            if (field != null) {
                field.setAccessible(true);
                ReflectionUtils.setField(field, pedidoAtual, valorCampo);
            }
        });
        return pedidoRepository.save(pedidoAtual);
    }

    public void excluir(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (pedido.getStatusPedido() != 1) {
            throw new RuntimeException("Apenas pedidos Pendentes podem ser excluídos.");
        }
        pedidoRepository.delete(pedido);
    }

    // ─── Geração de config e info para o CLP ──────────────────────────────────

    /**
     * Gera o DTO de configuração do pedido (cabeçalho + lista de blocos + IP do
     * CLP).
     */
    @Transactional(readOnly = true)
    public PedidoConfigDTO gerarConfig(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        PedidoConfigDTO dto = new PedidoConfigDTO();
        dto.setId(pedido.getIdPedido());
        dto.setOrdemProducao(pedido.getOrdemProducao());
        dto.setTipoPedido(pedido.getTipoPedido());
        dto.setCorTampa(pedido.getCorTampa());
        dto.setStatusPedido(pedido.getStatusPedido());

        List<Bloco> blocos = pedido.getBlocos();
        List<BlocoDTO> blocosDTO = new java.util.ArrayList<>();
        for (int i = 0; i < blocos.size(); i++) {
            Bloco bloco = blocos.get(i);
            EnumCorBloco enumCor = EnumCorBloco.fromCodigo(bloco.getCorBloco());
            BlocoDTO blocoDTO = new BlocoDTO(
                    bloco.getIdBloco(),
                    i + 1,
                    enumCor,
                    toLaminaDTOList(bloco.getLaminas()),
                    bloco.getCriadoEm(),
                    bloco.getEstoque() != null ? bloco.getEstoque().getId() : null,
                    pedido.getIdPedido());
            blocosDTO.add(blocoDTO);
        }
        dto.setBlocos(blocosDTO);
        dto.setIpClp("10.74.241.10");
        return dto;
    }

    /**
     * Gera o DTO de informações do pedido para envio ao CLP.
     *
     * Os campos posicaoEstoqueAndar* são preenchidos com a posição física
     * (posicaoEstoque) do estoque vinculado a cada bloco quando o pedido foi
     * criado.
     */
    @Transactional(readOnly = true)
    public PedidoInfoDTO gerarInfo(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        PedidoInfoDTO dto = new PedidoInfoDTO();
        dto.setNumeroPedido(id.intValue());
        dto.setAndares(pedido.getBlocos().size());
        dto.setPosicaoExpedicao(
                pedido.getPosicaoExpedicao() == null ? 1 : pedido.getPosicaoExpedicao());

        List<Bloco> blocos = pedido.getBlocos();
        for (int i = 0; i < blocos.size() && i < 3; i++) {
            Bloco bloco = blocos.get(i);

            // Valida que o bloco tem estoque vinculado (deve ter sido atribuído em
            // criarPedido)
            if (bloco.getEstoque() == null) {
                throw new RuntimeException(
                        "Bloco " + bloco.getIdBloco() + " não tem posição de estoque vinculada.");
            }

            int posicao = bloco.getEstoque().getPosicaoEstoque();
            System.out.printf("[gerarInfo] Andar %d → bloco=%d, cor=%d, posicaoEstoque=%d%n",
                    i + 1, bloco.getIdBloco(), bloco.getCorBloco(), posicao);

            List<Lamina> laminas = bloco.getLaminas().stream()
                    .sorted(Comparator.comparing(Lamina::getPosicaoNoBloco))
                    .toList();

            preencherAndar(dto, bloco, laminas, i + 1);
        }

        return dto;
    }

    // ─── Preenchimento dos andares ────────────────────────────────────────────
    private void preencherAndar(PedidoInfoDTO dto, Bloco bloco, List<Lamina> laminas, int andar) {
        switch (andar) {
            case 1 -> {
                dto.setCorAndar1(bloco.getCorBloco());
                dto.setPosicaoEstoqueAndar1(bloco.getEstoque().getPosicaoEstoque());
                preencherLaminasAndar1(dto, laminas);
                dto.setProcessamentoAndar1(1);
            }
            case 2 -> {
                dto.setCorAndar2(bloco.getCorBloco());
                dto.setPosicaoEstoqueAndar2(bloco.getEstoque().getPosicaoEstoque());
                preencherLaminasAndar2(dto, laminas);
                dto.setProcessamentoAndar2(1);
            }
            case 3 -> {
                dto.setCorAndar3(bloco.getCorBloco());
                dto.setPosicaoEstoqueAndar3(bloco.getEstoque().getPosicaoEstoque());
                preencherLaminasAndar3(dto, laminas);
                dto.setProcessamentoAndar3(1);
            }
        }
    }

    private void preencherLaminasAndar1(PedidoInfoDTO dto, List<Lamina> laminas) {
        if (laminas.size() > 0) {
            dto.setCorLamina1Andar1(laminas.get(0).getCor());
            dto.setPadraoLamina1Andar1(laminas.get(0).getPadrao());
        }
        if (laminas.size() > 1) {
            dto.setCorLamina2Andar1(laminas.get(1).getCor());
            dto.setPadraoLamina2Andar1(laminas.get(1).getPadrao());
        }
        if (laminas.size() > 2) {
            dto.setCorLamina3Andar1(laminas.get(2).getCor());
            dto.setPadraoLamina3Andar1(laminas.get(2).getPadrao());
        }
    }

    private void preencherLaminasAndar2(PedidoInfoDTO dto, List<Lamina> laminas) {
        if (laminas.size() > 0) {
            dto.setCorLamina1Andar2(laminas.get(0).getCor());
            dto.setPadraoLamina1Andar2(laminas.get(0).getPadrao());
        }
        if (laminas.size() > 1) {
            dto.setCorLamina2Andar2(laminas.get(1).getCor());
            dto.setPadraoLamina2Andar2(laminas.get(1).getPadrao());
        }
        if (laminas.size() > 2) {
            dto.setCorLamina3Andar2(laminas.get(2).getCor());
            dto.setPadraoLamina3Andar2(laminas.get(2).getPadrao());
        }
    }

    private void preencherLaminasAndar3(PedidoInfoDTO dto, List<Lamina> laminas) {
        if (laminas.size() > 0) {
            dto.setCorLamina1Andar3(laminas.get(0).getCor());
            dto.setPadraoLamina1Andar3(laminas.get(0).getPadrao());
        }
        if (laminas.size() > 1) {
            dto.setCorLamina2Andar3(laminas.get(1).getCor());
            dto.setPadraoLamina2Andar3(laminas.get(1).getPadrao());
        }
        if (laminas.size() > 2) {
            dto.setCorLamina3Andar3(laminas.get(2).getCor());
            dto.setPadraoLamina3Andar3(laminas.get(2).getPadrao());
        }
    }

    private void registrarNaExpedicao(Pedido pedido) {
        System.out.println("Gerando registro de expedição para a OP: " + pedido.getOrdemProducao());
    }
}