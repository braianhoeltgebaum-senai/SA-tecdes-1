package com.sa.smart.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import com.sa.smart.model.Pedido;
import com.sa.smart.service.PedidoService;


@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // LISTAR TODOS
    @GetMapping("/listar")
    public List<Pedido> listar() {
        return pedidoService.listarTodos();
    }

    // CRIAR PEDIDO
    @PostMapping("/salvar")
    public ResponseEntity<Pedido> criar(@RequestBody Pedido pedido) {
        return ResponseEntity.ok(pedidoService.criarPedido(pedido));
    }

    // DASHBOARD (MES KPI)
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Long>> dashboard() {
        return ResponseEntity.ok(pedidoService.dashboard());
    }

    // INICIAR PRODUÇÃO (PENDENTE → PRODUÇÃO)
    @PutMapping("/{id}/producao")
    public ResponseEntity<Void> iniciarProducao(@PathVariable Long id) {
        pedidoService.iniciarProducao(id);
        return ResponseEntity.noContent().build();
    }

    // CONCLUIR PEDIDO (PRODUÇÃO → CONCLUÍDO)
    @PutMapping("/{id}/concluir")
    public ResponseEntity<Void> concluir(@PathVariable Long id) {
        pedidoService.atualizarStatusParaConcluido(id);
        return ResponseEntity.noContent().build();
    }

    // ATUALIZAÇÃO PARCIAL
    @PatchMapping("/{id}")
    public ResponseEntity<Pedido> atualizarParcial(
            @PathVariable Long id,
            @RequestBody Map<String, Object> campos) {

        return ResponseEntity.ok(
                pedidoService.atualizarParcial(id, campos)
            );
    }

    // DELETAR (somente pendentes)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pedidoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // TABELA PARA HTMX (DASHBOARD MES)
    @GetMapping("/tabela")
    public List<Pedido> tabela() {
        return pedidoService.ultimosPedidos();
    }

}