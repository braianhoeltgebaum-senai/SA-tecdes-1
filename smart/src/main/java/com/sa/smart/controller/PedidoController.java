package com.sa.smart.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.smart.dto.PedidoConfigDTO;
import com.sa.smart.dto.PedidoInfoDTO;
import com.sa.smart.model.Pedido;
import com.sa.smart.service.PedidoService;
import com.sa.smart.service.SmartService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final SmartService  smartService;

    // -------------------------------------------------------------------------
    // GET /pedidos — lista todos os pedidos
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<Pedido>> listar() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    // -------------------------------------------------------------------------
    // GET /pedidos/{id} — busca um pedido por ID
    // -------------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscar(@PathVariable Long id) {
        return pedidoService.listarTodos()
                .stream()
                .filter(p -> p.getIdPedido().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------------------------
    // POST /pedidos — cria um novo pedido
    // -------------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<Pedido> criar(@RequestBody Pedido pedido) {
        try {
            Pedido salvo = pedidoService.criarPedido(pedido);
            return ResponseEntity.ok(salvo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /pedidos/{id} — atualização parcial de campos do pedido
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}")
    public ResponseEntity<Pedido> atualizarParcial(
            @PathVariable Long id,
            @RequestBody Map<String, Object> campos) {
        try {
            Pedido atualizado = pedidoService.atualizarParcial(id, campos);
            return ResponseEntity.ok(atualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /pedidos/{id} — exclui pedido (somente se estiver Pendente)
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        try {
            pedidoService.excluir(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /pedidos/{id}/concluir — marca o pedido como Concluído (status 3)
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/concluir")
    public ResponseEntity<Void> concluir(@PathVariable Long id) {
        try {
            pedidoService.atualizarStatusParaConcluido(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // -------------------------------------------------------------------------
    // GET /pedidos/{id}/config — retorna o PedidoConfigDTO (dados de controle)
    // -------------------------------------------------------------------------
    @GetMapping("/{id}/config")
    public ResponseEntity<PedidoConfigDTO> config(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(pedidoService.gerarConfig(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // -------------------------------------------------------------------------
    // GET /pedidos/{id}/info — retorna o PedidoInfoDTO (dados de produção)
    // -------------------------------------------------------------------------
    @GetMapping("/{id}/info")
    public ResponseEntity<PedidoInfoDTO> info(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(pedidoService.gerarInfo(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // -------------------------------------------------------------------------
    // POST /pedidos/{id}/iniciar — envia o pedido ao CLP e inicia a produção
    //
    // Parâmetro opcional ?ip=192.168.x.x
    //   → se informado, sobrescreve o IP hardcoded no gerarConfig()
    //   → se omitido, usa o IP que estiver salvo no gerarConfig() (10.74.241.10)
    // -------------------------------------------------------------------------
    @PostMapping("/{id}/iniciar")
public ResponseEntity<String> iniciar(
        @PathVariable Long id,
        @RequestParam(required = false) String ip) {
    try {
        PedidoConfigDTO config = pedidoService.gerarConfig(id);
        PedidoInfoDTO   info   = pedidoService.gerarInfo(id);

        if (ip != null && !ip.isBlank()) {
            config.setIpClp(ip);
        }

        if (config.getIpClp() == null || config.getIpClp().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body("IP do CLP não informado.");
        }

        smartService.enviarParaProducao(config, info);

        // NOVO: persiste a transição de status Pendente → Em Produção
        pedidoService.atualizarStatusParaEmProducao(id);

        return ResponseEntity.ok(
            "Pedido #" + id + " enviado ao CLP " + config.getIpClp()
        );

    } catch (RuntimeException e) {
        return ResponseEntity
                .internalServerError()
                .body("Erro ao iniciar produção: " + e.getMessage());
    }
}
}