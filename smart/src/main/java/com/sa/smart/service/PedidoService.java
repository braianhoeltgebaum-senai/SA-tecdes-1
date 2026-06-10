package com.sa.smart.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.sa.smart.dto.BlocoDTO;
import com.sa.smart.dto.PedidoConfigDTO;
import com.sa.smart.dto.PedidoInfoDTO;
import com.sa.smart.model.Bloco;
import com.sa.smart.model.Lamina;
import com.sa.smart.model.Pedido;
import com.sa.smart.repository.BlocoRepository;
import com.sa.smart.repository.EstoqueRepository;
import com.sa.smart.repository.LaminaRepository;
import com.sa.smart.repository.PedidoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final BlocoRepository blocoRepository;
    private final LaminaRepository laminaRepository;
    private final EstoqueRepository estoqueRepository;

    public PedidoService(PedidoRepository pedidoRepository, BlocoRepository blocoRepository,
            LaminaRepository laminaRepository, EstoqueRepository estoqueRepository) {
        this.pedidoRepository = pedidoRepository;
        this.blocoRepository = blocoRepository;
        this.laminaRepository = laminaRepository;
        this.estoqueRepository = estoqueRepository;
    }

    // LISTAR TODOS
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    // DASHBOARD KPIs (MES)
    public long contarPendentes() {
        return pedidoRepository.countByStatusPedido(1);
    }

    public long contarProducao() {
        return pedidoRepository.countByStatusPedido(2);
    }

    public long contarConcluidos() {
        return pedidoRepository.countByStatusPedido(3);
    }

    // resumo geral (usado no dashboard)
    public Map<String, Long> dashboard() {
        return Map.of(
                "pendentes", contarPendentes(),
                "emProducao", contarProducao(),
                "concluidos", contarConcluidos());
    }

    // CRIAR PEDIDO
    public Pedido criarPedido(Pedido pedido) {

        // validação tipo 3
        if (pedido.getTipoPedido() == 3 &&
                (pedido.getBlocos() == null || pedido.getBlocos().size() != 3)) {
            throw new RuntimeException("Pedidos triplos exigem exatamente 3 blocos.");
        }

        pedido.setStatusPedido(1); // PENDENTE
        pedido.setTimestamp(LocalDateTime.now());

        return pedidoRepository.save(pedido);
    }

    // INICIAR PRODUÇÃO (PENDENTE → PRODUÇÃO)
    public void iniciarProducao(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setStatusPedido(2);
        pedidoRepository.save(pedido);
    }

    // CONCLUIR PRODUÇÃO
    public void atualizarStatusParaConcluido(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setStatusPedido(3);
        pedidoRepository.save(pedido);

        registrarNaExpedicao(pedido);
    }

    // UPDATE PARCIAL (REFLECTION CONTROLADO)
    public Pedido atualizarParcial(Long id, Map<String, Object> campos) {

        Pedido pedidoAtual = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        campos.forEach((nomeCampo, valorCampo) -> {

            Field field = ReflectionUtils.findField(Pedido.class, nomeCampo);

            if (field != null) {

                field.setAccessible(true);

                // evita sobrescrever id e status sem controle
                if (!nomeCampo.equals("idPedido")) {
                    ReflectionUtils.setField(field, pedidoAtual, valorCampo);
                }
            }
        });

        return pedidoRepository.save(pedidoAtual);
    }

    // EXCLUIR (somente pendentes)
    public void excluir(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (pedido.getStatusPedido() != 1) {
            throw new RuntimeException("Apenas pedidos pendentes podem ser excluídos.");
        }

        pedidoRepository.delete(pedido);
    }

    // LISTA PARA TABELA DO DASHBOARD (MES)
    public List<Pedido> ultimosPedidos() {
        return pedidoRepository.findTop10ByOrderByTimestampDesc();
    }

    // REGISTRO DE EXPEDIÇÃO
    private void registrarNaExpedicao(Pedido pedido) {
        System.out.println("Expedição gerada para OP: " + pedido.getOrdemProducao());
    }

    public PedidoConfigDTO gerarConfig(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        PedidoConfigDTO dto = new PedidoConfigDTO();

        dto.setId(pedido.getIdPedido());
        dto.setOrdemProducao(pedido.getOrdemProducao());
        dto.setTipoPedido(pedido.getTipoPedido());
        dto.setCorTampa(pedido.getCorTampa());
        dto.setStatusPedido(pedido.getStatusPedido());

        dto.setBlocos(
                pedido.getBlocos()
                        .stream()
                        .map(bloco -> new BlocoDTO(
                                bloco.getIdBloco(),
                                null,
                                bloco.getCriadoEm(),
                                bloco.getEstoque().getId(),
                                pedido.getIdPedido()))
                        .toList());

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
                pedido.getPosicaoExpedicao() == null
                        ? 1
                        : pedido.getPosicaoExpedicao());

        List<Bloco> blocos = pedido.getBlocos();

        for (int i = 0; i < blocos.size() && i < 3; i++) {

            Bloco bloco = blocos.get(i);

            List<Lamina> laminas = bloco.getLaminas()
                    .stream()
                    .sorted(Comparator.comparing(Lamina::getPosicaoNoBloco))
                    .toList();

            preencherAndar(dto, bloco, laminas, i + 1);
        }

        return dto;
    }

    private void preencherAndar(
            PedidoInfoDTO dto,
            Bloco bloco,
            List<Lamina> laminas,
            int andar) {

        switch (andar) {

            case 1 -> {

                dto.setCorAndar1(bloco.getCorBloco());
                dto.setPosicaoEstoqueAndar1(
                        bloco.getEstoque().getPosicao());

                preencherLaminasAndar1(dto, laminas);

                dto.setProcessamentoAndar1(1);
            }

            case 2 -> {

                dto.setCorAndar2(bloco.getCorBloco());
                dto.setPosicaoEstoqueAndar2(
                        bloco.getEstoque().getPosicao());

                preencherLaminasAndar2(dto, laminas);

                dto.setProcessamentoAndar2(1);
            }

            case 3 -> {

                dto.setCorAndar3(bloco.getCorBloco());
                dto.setPosicaoEstoqueAndar3(
                        bloco.getEstoque().getPosicao());

                preencherLaminasAndar3(dto, laminas);

                dto.setProcessamentoAndar3(1);
            }
        }
    }

    private void preencherLaminasAndar1(
            PedidoInfoDTO dto,
            List<Lamina> laminas) {

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

    private void preencherLaminasAndar2(
            PedidoInfoDTO dto,
            List<Lamina> laminas) {

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

    private void preencherLaminasAndar3(
            PedidoInfoDTO dto,
            List<Lamina> laminas) {

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
}