package com.sa.smart.dto;

import java.time.LocalDateTime;
import com.sa.smart.enums.EnumCorBloco;

public record BlocoDTO(Long id, EnumCorBloco corBloco, LocalDateTime criadoEm, Long estoqueId, Long pedidoId) {

    public Object getId() {

        return id;

    }

}
