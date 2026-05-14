package com.sa.smart.dto;

import java.time.LocalDateTime;

public record ExpedicaoDTO(Long id, Integer posicaoExpedicao, LocalDateTime entradaEm, LocalDateTime saidaEm, String ordemProducao) {

}
