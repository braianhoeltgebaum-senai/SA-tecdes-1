package com.sa.smart.dto;

import java.time.LocalDateTime;

public record ExpedicaoDTO(Long id, Integer posicao_expedicao, LocalDateTime entrada_em, LocalDateTime saida_em, String ordem_producao) {

}
