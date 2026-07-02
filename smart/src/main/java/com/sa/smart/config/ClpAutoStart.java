package com.sa.smart.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sa.smart.controller.ClpController;

/**
 * Antes, o polling dos CLPs (POST /start-readings) só era disparado pelo
 * JavaScript da página "Acompanhamento" (visualizacaoBancada.html) ao ser
 * carregada no navegador. Isso significava que, se ninguém tivesse essa
 * aba aberta — ou sempre que o backend reiniciasse — nenhuma leitura
 * acontecia, e o rastreamento automático de status (RastreamentoService)
 * nunca tinha dados para processar.
 *
 * Este componente chama o mesmo endpoint automaticamente assim que a
 * aplicação Spring termina de subir, garantindo que as leituras dos CLPs
 * rodem de forma contínua e independente de qualquer página estar aberta.
 *
 * Os IPs podem ser sobrescritos via application.properties:
 *   smart.clp.estoque-ip=10.74.241.10
 *   smart.clp.processo-ip=10.74.241.20
 *   smart.clp.montagem-ip=10.74.241.30
 *   smart.clp.expedicao-ip=10.74.241.40
 */
@Component
public class ClpAutoStart {

    private final ClpController clpController;

    @Value("${smart.clp.estoque-ip:10.74.241.10}")
    private String ipEstoque;

    @Value("${smart.clp.processo-ip:10.74.241.20}")
    private String ipProcesso;

    @Value("${smart.clp.montagem-ip:10.74.241.30}")
    private String ipMontagem;

    @Value("${smart.clp.expedicao-ip:10.74.241.40}")
    private String ipExpedicao;

    public ClpAutoStart(ClpController clpController) {
        this.clpController = clpController;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciarLeiturasAutomaticamente() {
        Map<String, String> ips = new LinkedHashMap<>();
        ips.put("estoque", ipEstoque);
        ips.put("processo", ipProcesso);
        ips.put("montagem", ipMontagem);
        ips.put("expedicao", ipExpedicao);

        System.out.println("[ClpAutoStart] Iniciando leituras automáticas dos CLPs: " + ips);
        clpController.startReadings(ips);
    }
}