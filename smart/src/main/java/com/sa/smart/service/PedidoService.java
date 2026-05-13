package com.sa.smart.service;

import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sa.smart.model.Pedido;
import com.sa.smart.repository.PedidoRepository;



@Service

public class PedidoService {

    @Autowired
     private PedidoRepository pedidoRepository = null;
    
    public PedidoService() {
    }

    
    public Pedido criarPedido(Pedido pedido) {
        
         if (pedido.getTipo_pedido() == 3  && (pedido.getBlocos() == null || pedido.getBlocos().size() !=3)) {
            throw new RuntimeException("Pedidos triplos exigem exatamente 3 blocos.");
        }
  
        pedido.getBlocos().forEach(bloco -> {
            if (bloco.getLaminas() != null && bloco.getLaminas().size() > 3) {
                throw new RuntimeException("Cada bloco pode ter no máximo 3 lâminas.");
            }
        });

        pedido.setStatus_pedido(1); // 1 - Pendente
        
        return pedidoRepository.save(pedido);
    }

    public void atualizarStatusParaConcluido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setStatus_pedido(3);
        
       
        pedidoRepository.save(pedido);
    }
} 