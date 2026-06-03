package com.sa.smart.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.sa.smart.model.Pedido;
import com.sa.smart.repository.PedidoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    // 📋 LISTAR TODOS
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    // 📊 DASHBOARD KPIs (MES)
    public long contarPendentes() {
        return pedidoRepository.countByStatusPedido(1);
    }

    public long contarProducao() {
        return pedidoRepository.countByStatusPedido(2);
    }

    public long contarConcluidos() {
        return pedidoRepository.countByStatusPedido(3);
    }

    // 📊 resumo geral (usado no dashboard)
    public Map<String, Long> dashboard() {
        return Map.of(
            "pendentes", contarPendentes(),
            "emProducao", contarProducao(),
            "concluidos", contarConcluidos()
        );
    }

    // ➕ CRIAR PEDIDO
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

    // 🔵 INICIAR PRODUÇÃO (PENDENTE → PRODUÇÃO)
    public void iniciarProducao(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setStatusPedido(2);
        pedidoRepository.save(pedido);
    }

    // 🟢 CONCLUIR PRODUÇÃO
    public void atualizarStatusParaConcluido(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setStatusPedido(3);
        pedidoRepository.save(pedido);

        registrarNaExpedicao(pedido);
    }

    // ✏️ UPDATE PARCIAL (REFLECTION CONTROLADO)
    public Pedido atualizarParcial(Long id, Map<String, Object> campos) {

        Pedido pedidoAtual = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        campos.forEach((nomeCampo, valorCampo) -> {

            Field field = ReflectionUtils.findField(Pedido.class, nomeCampo);

            if (field != null) {

                field.setAccessible(true);

                // 🔒 evita sobrescrever id e status sem controle
                if (!nomeCampo.equals("idPedido")) {
                    ReflectionUtils.setField(field, pedidoAtual, valorCampo);
                }
            }
        });

        return pedidoRepository.save(pedidoAtual);
    }

    // ❌ EXCLUIR (somente pendentes)
    public void excluir(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (pedido.getStatusPedido() != 1) {
            throw new RuntimeException("Apenas pedidos pendentes podem ser excluídos.");
        }

        pedidoRepository.delete(pedido);
    }

    // 📦 LISTA PARA TABELA DO DASHBOARD (MES)
    public List<Pedido> ultimosPedidos() {
        return pedidoRepository.findTop10ByOrderByTimestampDesc();
    }

    // 🏭 REGISTRO DE EXPEDIÇÃO
    private void registrarNaExpedicao(Pedido pedido) {
        System.out.println("📦 Expedição gerada para OP: " + pedido.getOrdemProducao());
    }
}