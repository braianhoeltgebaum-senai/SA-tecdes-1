package com.sa.smart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Lê o bloco "api.*" do application.properties / application.yml.
 *
 * Exemplo de configuração:
 *   api.estoque-api-url=http://host-estoque/api
 *   api.expedicao-api-url=http://host-expedicao/api
 *   api.seletor-tampas-present=true
 */
@Component
@ConfigurationProperties(prefix = "api")
public class ApiUrlConfig {

    private String  estoqueApiUrl;
    private String  expedicaoApiUrl;
    // CORRIGIDO: boolean primitivo com getter no padrão "is" para @ConfigurationProperties
    private boolean seletorTampasPresent;

    public String getEstoqueApiUrl() {
        return estoqueApiUrl;
    }

    public void setEstoqueApiUrl(String estoqueApiUrl) {
        this.estoqueApiUrl = estoqueApiUrl;
    }

    public String getExpedicaoApiUrl() {
        return expedicaoApiUrl;
    }

    public void setExpedicaoApiUrl(String expedicaoApiUrl) {
        this.expedicaoApiUrl = expedicaoApiUrl;
    }

    /** Retorna true se o seletor de tampas está presente na bancada. */
    public boolean isSeletorTampasPresent() {
        return seletorTampasPresent;
    }

    /**
     * Alias para compatibilidade com SmartController que chama getSeletorTampasPresent().
     * Ambos retornam o mesmo valor.
     */
    public boolean getSeletorTampasPresent() {
        return seletorTampasPresent;
    }

    public void setSeletorTampasPresent(boolean seletorTampasPresent) {
        this.seletorTampasPresent = seletorTampasPresent;
    }
}