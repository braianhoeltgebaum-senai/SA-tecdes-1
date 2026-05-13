package com.sa.smart.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record PedidoDTO(
    Long id,
    String ordemProducao,   // Necessário para identificar a OP [cite: 19]
    Integer tipoPedido,     // 1, 2 ou 3 [cite: 21]
    Integer statusPedido,   // 1, 2 ou 3 [cite: 20]
    Integer corTampa,       // 1, 2 ou 3 [cite: 22]
    List<BlocoDTO> blocos,  // Para validar se existem 3 blocos no tipo Triplo [cite: 59]
    Integer total
) {}