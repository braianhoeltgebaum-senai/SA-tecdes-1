package com.sa.smart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "api")
public class ApiUrlConfig {
    private String estoqueApiUrl;
    private String expedicaoApiUrl;
    private boolean seletorTampasPresent;
    // getters e setters
}