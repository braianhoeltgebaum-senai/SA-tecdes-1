package com.sa.smart.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Field;

import com.sa.smart.model.Pedido;
import com.sa.smart.repository.PedidoRepository;

@Service
public class PedidoService {


    private PedidoRepository pedidoRepository;


    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    public Pedido criarPedido(Pedido pedido) {
        // Validação de Tipo: Pedido Triplo (tipo 3) deve ter exatamente 3 blocos [cite: 21, 59]
        if (pedido.getTipo_pedido() == 3 && (pedido.getBlocos() == null || pedido.getBlocos().size() != 3)) {
            throw new RuntimeException("Pedidos triplos exigem exatamente 3 blocos."); 
        }

        // Regra da Lâmina: Cada bloco pode ter no máximo 3 lâminas [cite: 27, 61]
        if (pedido.getBlocos() != null) {
            pedido.getBlocos().forEach(bloco -> {
                if (bloco.getLaminas() != null && bloco.getLaminas().size() > 3) {
                    throw new RuntimeException("Cada bloco pode ter no máximo 3 lâminas."); 
                }
            });
        }

        pedido.setStatus_pedido(1); // 1 - Pendente [cite: 20]
        return pedidoRepository.save(pedido);
    }

    public void atualizarStatusParaConcluido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setStatus_pedido(3); // 3 - Concluído [cite: 20, 65]
        pedidoRepository.save(pedido);

        // Ao ser concluído, o sistema deve registrar a saída na expedição 
        registrarNaExpedicao(pedido);
    }

    public Pedido atualizarParcial(Long id, Map<String, Object> campos) {
        Pedido pedidoAtual = pedidoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        // Percorre os campos enviados no JSON para atualizar apenas o que foi solicitado
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

        // Opcional: Impedir a exclusão de pedidos que já estão em produção ou concluídos
        if (pedido.getStatus_pedido() != 1) {
            throw new RuntimeException("Apenas pedidos Pendentes podem ser excluídos para garantir a rastreabilidade.");
        }

        pedidoRepository.delete(pedido);
    }

    private void registrarNaExpedicao(Pedido pedido) {
   
        System.out.println("Gerando registro de expedição para a OP: " + pedido.getOrdem_producao());
    }
}