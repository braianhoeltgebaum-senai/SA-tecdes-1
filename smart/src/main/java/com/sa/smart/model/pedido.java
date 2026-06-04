package com.sa.smart.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private Long idPedido;

    @Column(nullable = false, unique = true)
    private String ordemProducao; 

    @Column(name = "status_pedido", nullable = false)
    private Integer statusPedido; 

    @Column(nullable = false)
    private Integer tipoPedido; 

    @Column(nullable = false)
    private Integer corTampa; 

    private Integer posicaoExpedicao; 
    private LocalDateTime timestamp; 

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bloco> blocos = new ArrayList<>();



    public void adicionarBloco(Bloco bloco) {
        this.blocos.add(bloco);
        bloco.setPedido(this);
    }
}