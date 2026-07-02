package com.sa.smart.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sa.smart.service.RastreamentoService;

@RestController
public class RastreamentoController {

    private final RastreamentoService rastreamentoService;

    public RastreamentoController(RastreamentoService rastreamentoService) {
        this.rastreamentoService = rastreamentoService;
    }

    @GetMapping("/rastreamento")
    public Map<String, Object> rastrear() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("bancadaAtual", RastreamentoService.bancadaAtual);
        resp.put("numeroOpAtual", RastreamentoService.numeroOpAtual);
        resp.put("estadoPedido", rastreamentoService.obterEstadoPedido());
        resp.put("statusEstoque", RastreamentoService.statusEstoqueRastreio);
        resp.put("statusProcesso", RastreamentoService.statusProcessoRastreio);
        resp.put("statusMontagem", RastreamentoService.statusMontagemRastreio);
        resp.put("statusExpedicao", RastreamentoService.statusExpedicaoRastreio);
        return resp;
    }
}