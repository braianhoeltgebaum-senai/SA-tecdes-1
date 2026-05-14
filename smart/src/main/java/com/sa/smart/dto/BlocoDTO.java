package com.sa.smart.dto;

import java.time.LocalDateTime;

import com.sa.smart.model.Bloco;

public record BlocoDTO(

    Integer idBloco,
    Integer corBloco,
    LocalDateTime criadoEm,
    Integer estoquePosicao,
    String pedidoOrdemProducao

) {

    public static BlocoDTO fromEntity(Bloco bloco) {

        return new BlocoDTO(
                bloco.getIdBloco(),
                bloco.getCorBloco(),
                bloco.getCriadoEm(),
                bloco.getEstoque().getPosicao(),
                bloco.getPedido().getOrdemProducao()
        );

    }
}