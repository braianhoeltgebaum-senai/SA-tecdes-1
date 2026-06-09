package com.sa.smart.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.sa.smart.enums.EnumCorBloco;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
    private Long idBloco;

    @ManyToOne
    @JoinColumn(name = "pedido_ordem_producao", nullable = false)
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "estoque_posicao", nullable = false)
    private Estoque estoque;

    @Column(name = "cor_bloco", nullable = false)
    private Integer corBloco; // sem @Enumerated

    /*
     * @Enumerated(EnumType.STRING)
     * 
     * @Column(name = "cor_bloco", nullable = false)
     * private Integer corBloco;
     */

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "bloco", cascade = CascadeType.ALL)
    private List<Lamina> laminas = new ArrayList<>();

}