package com.sa.smart.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sa.smart.model.Pedido;
import com.sa.smart.service.PedidoService;


@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @GetMapping
    public List<Pedido> listar() {
        return pedidoService.listarTodos();
    }

    @PostMapping
    public ResponseEntity<Pedido> criar(@RequestBody Pedido pedido) {

        Pedido novoPedido = pedidoService.criarPedido(pedido);
        return ResponseEntity.ok(novoPedido);

    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> atualizarStatus(@PathVariable Long id) {

        pedidoService.atualizarStatusParaConcluido(id);
        return ResponseEntity.noContent().build();
        
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Pedido> atualizarParcial(@PathVariable Long id, @RequestBody Map<String, Object> campos) {

        Pedido pedidoAtualizado = pedidoService.atualizarParcial(id, campos);
        return ResponseEntity.ok(pedidoAtualizado);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {

        pedidoService.excluir(id);
        return ResponseEntity.noContent().build();
        
    }
}