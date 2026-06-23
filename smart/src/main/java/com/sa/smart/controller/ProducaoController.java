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
public class ProducaoController {

    private final PedidoService pedidoService;
    private final SmartService  smartService;

    /** Serializa o pedido em bytes e escreve no bloco de dados do CLP. */
    @PostMapping("/{id}/gravar")
    public ResponseEntity<String> gravarPedido(@PathVariable Long id) {
        PedidoConfigDTO config = pedidoService.gerarConfig(id);
        PedidoInfoDTO   info   = pedidoService.gerarInfo(id);
        smartService.enviarParaProducao(config, info);
        return ResponseEntity.ok("Pedido enviado ao CLP");
    }

    /**
     * Inicia a execução do pedido: seta/reseta flags no CLP e,
     * se configurado, aciona o seletor de tampas via ESP32.
     *
     * CORRIGIDO: era smartService.iniciarExecucaoPedido(ip, tampa) — método
     * que não existia. Agora existe em SmartService com essa assinatura exata.
     */
    @PostMapping("/{id}/iniciar")
    public ResponseEntity<String> iniciarPedido(@PathVariable Long id) {
        PedidoConfigDTO config = pedidoService.gerarConfig(id);
        smartService.iniciarExecucaoPedido(config.getIpClp(), config.getCorTampa());
        return ResponseEntity.ok("Produção iniciada.");
    }
}