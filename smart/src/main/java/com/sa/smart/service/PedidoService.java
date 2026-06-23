package com.sa.smart.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
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

    // ─── Mapeamento auxiliar ────────────────────────────────────────────────────

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
     * Cria um novo pedido a partir do objeto Pedido recebido.
     * Valida se pedidos triplos têm exatamente 3 blocos,
     * atribui status Pendente (1) e associa cada bloco a uma posição de estoque
     * disponível.
     */
    public Pedido criarPedido(Pedido pedido) {
        if (pedido.getTipoPedido() == 3 &&
                (pedido.getBlocos() == null || pedido.getBlocos().size() != 3)) {
            throw new RuntimeException("Pedidos triplos exigem exatamente 3 blocos.");
        }

        pedido.setStatusPedido(1); // Pendente

        if (pedido.getBlocos() != null) {
            for (Bloco bloco : pedido.getBlocos()) {
                bloco.setPedido(pedido);
                bloco.setCriadoEm(LocalDateTime.now());

                Integer corBloco = bloco.getCorBloco();
                if (corBloco == null) {
                    throw new RuntimeException("Cor do bloco não informada.");
                }

                Estoque estoque = estoqueRepository.findFirstByCorOrderByPosicaoEstoqueAsc(corBloco)
                        .orElseThrow(() -> new RuntimeException(
                                "Nenhuma posição de estoque disponível com a cor: " + corBloco));

                bloco.setEstoque(estoque);
                estoque.setCor(0); // marca como ocupado
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
            // Obtém o andar (i+1) e converte a cor para EnumCorBloco
            EnumCorBloco enumCor = EnumCorBloco.fromCodigo(bloco.getCorBloco());
            BlocoDTO blocoDTO = new BlocoDTO(
                    bloco.getIdBloco(),
                    i + 1, // andar
                    enumCor, // corBloco (Enum)
                    toLaminaDTOList(bloco.getLaminas()), // lâminas
                    bloco.getCriadoEm(),
                    bloco.getEstoque() != null ? bloco.getEstoque().getId() : null,
                    pedido.getIdPedido());
            blocosDTO.add(blocoDTO);
        }
        dto.setBlocos(blocosDTO);
        // IP padrão (pode ser sobrescrito pelo frontend)
        dto.setIpClp("10.74.241.10");

        return dto;
    }

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