package com.sa.smart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sa.smart.dto.PedidoConfigDTO;
import com.sa.smart.dto.PedidoInfoDTO;
import com.sa.smart.service.PedidoService;
import com.sa.smart.service.SmartService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/producao")
@RequiredArgsConstructor
public class SmartController {

    private final PedidoService pedidoService;
    private final SmartService smartService;

    @PostMapping("/{id}/gravar")
    public ResponseEntity<String> gravarPedido(
            @PathVariable Long id) {

        PedidoConfigDTO config = pedidoService.gerarConfig(id);
        PedidoInfoDTO info = pedidoService.gerarInfo(id);

        smartService.enviarParaProducao(config, info);

        return ResponseEntity.ok("Pedido enviado ao CLP");
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<String> iniciarPedido(
            @PathVariable Long id) {

        PedidoConfigDTO config = pedidoService.gerarConfig(id);

        smartService.iniciarExecucaoPedido(config.getIpClp());

        return ResponseEntity.ok("Produção iniciada.");
    }
}
