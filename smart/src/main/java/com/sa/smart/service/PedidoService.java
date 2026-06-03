package com.sa.smart.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.sa.smart.model.Bloco;
import com.sa.smart.model.Estoque;
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

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    public Pedido criarPedido(Pedido pedido) {

        // REGRA PEDIDO TRIPLO
        if (pedido.getTipoPedido() == 3 &&
                (pedido.getBlocos() == null ||
                        pedido.getBlocos().size() != 3)) {

            throw new RuntimeException(
                    "Pedidos triplos exigem exatamente 3 blocos.");
        }

        pedido.setStatusPedido(1);

        // CONFIGURA CADA BLOCO
        if (pedido.getBlocos() != null) {

            for (Bloco bloco : pedido.getBlocos()) {

                bloco.setPedido(pedido);

                bloco.setCriadoEm(LocalDateTime.now());

                // BUSCA UM ESTOQUE EXISTENTE
                Estoque estoque = estoqueRepository
                        .findById(1L)
                        .orElseThrow(() ->
                                new RuntimeException("Estoque não encontrado"));

                bloco.setEstoque(estoque);
            }
        }

        return pedidoRepository.save(pedido);
    }

    public void atualizarStatusParaConcluido(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado"));

        pedido.setStatusPedido(3);

        pedidoRepository.save(pedido);

        registrarNaExpedicao(pedido);
    }

    public Pedido atualizarParcial(Long id,
            Map<String, Object> campos) {

        Pedido pedidoAtual = pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado"));

        campos.forEach((nomeCampo, valorCampo) -> {

            Field field = ReflectionUtils.findField(
                    Pedido.class,
                    nomeCampo);

            if (field != null) {

                field.setAccessible(true);

                ReflectionUtils.setField(
                        field,
                        pedidoAtual,
                        valorCampo);
            }
        });

        return pedidoRepository.save(pedidoAtual);
    }

    public void excluir(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado"));

        if (pedido.getStatusPedido() != 1) {

            throw new RuntimeException(
                    "Apenas pedidos Pendentes podem ser excluídos.");
        }

        pedidoRepository.delete(pedido);
    }

    private void registrarNaExpedicao(Pedido pedido) {

        System.out.println(
                "Gerando registro de expedição para a OP: "
                        + pedido.getOrdemProducao());
    }
}