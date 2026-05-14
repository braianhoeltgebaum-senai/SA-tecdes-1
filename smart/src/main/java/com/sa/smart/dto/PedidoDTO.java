package com.sa.smart.dto;

import java.util.List;

import lombok.Builder;

@Builder


public record PedidoDTO(
    Long id,
    String ordemProducao,   
    Integer tipoPedido,     
    Integer statusPedido,   
    Integer corTampa,       
    List<BlocoDTO> blocos,  
    Integer total
) {}