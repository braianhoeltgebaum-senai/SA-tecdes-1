package com.sa.smart.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PedidoConfigDTO {

    @JsonProperty("Id_Pedido")
    private Long id;
    @JsonProperty("Ordem_Pedido")
    private String ordemProducao;
    @JsonProperty("Tipo_Pedido")
    private int tipoPedido;
    @JsonProperty("Tampa_Pedido")
    private int corTampa;
    @JsonProperty("Status_Pedido")
    private int statusPedido;
    @JsonProperty("Blocos")
    private List<BlocoDTO> blocos;
    @JsonProperty("Ip_CLP")
    private String ipClp;
}
