package com.sa.smart.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor


public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_pedido;

    @Column(nullable = false)
    private String ordem_producao; 
    @Column(nullable = false)
    private Integer status_pedido; 

    @Column(nullable = false)
    private Integer tipo_pedido; 

    @Column(nullable = false)
    private Integer cor_tampa; 

    private Integer posicao_expedicao; 

    private LocalDateTime timestamp; 


    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    private List<Bloco> blocos;
}
    
    
