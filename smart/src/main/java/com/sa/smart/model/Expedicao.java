package com.sa.smart.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expedicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Expedicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer posicao_expedicao;
    private LocalDateTime entrada_em;
    private LocalDateTime saida_em;

    @ManyToOne
    @JoinColumn(name = "pedido_ordem_producao", nullable = false)
    private Pedido pedido;

}
