package com.sa.smart.controller;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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

@CrossOrigin("*")
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<Pedido> listar() {
        return pedidoService.listarTodos();
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Pedido pedido) {
        try {
            Pedido novoPedido = pedidoService.criarPedido(pedido);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoPedido);
        } catch (DataIntegrityViolationException e) {
            // Retorna o HTTP 409 (Conflito) junto com um objeto de erro amigável em formato JSON
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("{\"erro\": \"A Ordem de Produção '" + pedido.getOrdemProducao() + "' já está cadastrada.\"}");
        }
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

