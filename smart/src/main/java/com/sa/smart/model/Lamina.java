package com.sa.smart.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "lamina")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lamina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer cor;
    private Integer padrao;
    private Integer posicaoNoBloco;

    @ManyToOne
    @JoinColumn(name = "bloco_id_bloco", nullable = false)
    @JsonIgnore  
    private Bloco bloco;
}