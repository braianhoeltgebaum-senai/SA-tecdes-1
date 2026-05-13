package com.sa.smart.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bloco")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bloco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idBloco;
    
    @ManyToOne
    @JoinColumn(name = "pedido_ordem_producao", nullable = false)
    private Pedido pedido;
    
    @ManyToOne
    @JoinColumn(name = "estoque_posicao", nullable = false)
    private Estoque estoque;
    
    @Column(name = "cor_bloco", nullable = false)
    private Integer corBloco;
    
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
    
}