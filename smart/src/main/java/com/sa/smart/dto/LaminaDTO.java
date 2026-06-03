package com.sa.smart.dto;

public record LaminaDTO(
    Long id,
    Integer cor,
    Integer padrao, // <-- Garanta que está como Integer aqui!
    Integer posicaoNoBloco,
    Long blocoIdBloco
) {}