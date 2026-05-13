package com.sa.smart.dto;

import java.time.LocalDateTime;

public record BlocoDTO(Long id, int corBloco, LocalDateTime criadoEm, Long estoqueId, Long pedidoId) {

    public Object getId() {

        return id;

    }

}
