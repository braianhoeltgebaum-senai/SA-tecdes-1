package com.sa.smart.dto;

public record DashboardDTO(
    long pendentes,
    long emProducao,
    long concluidos
) {}