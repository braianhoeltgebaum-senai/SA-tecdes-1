package com.sa.smart.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.sa.smart.enums.EnumCorTampa;
import com.sa.smart.enums.EnumStatusPedido;
import com.sa.smart.enums.EnumTipoPedido;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pedidos") // Nome do banco: dbSmart40 
@Data
@NoArgsConstructor
@AllArgsConstructor


public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_pedido;

    @Column(nullable = false, unique = true)
    private String ordem_producao; 

    @Column(nullable = false)
    private EnumStatusPedido status_pedido; 

    @Column(nullable = false)
    private EnumTipoPedido tipo_pedido; 

    @Column(nullable = false)
    private EnumCorTampa cor_tampa; 

    private Integer posicao_expedicao; 
    private LocalDateTime timestamp; 

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bloco> blocos = new ArrayList<>();


    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
        if (this.status_pedido == null) {
            this.status_pedido = EnumStatusPedido.PENDENTE;}
    }


    public void adicionarBloco(Bloco bloco) {
        this.blocos.add(bloco);
        bloco.setPedido(this);
    }
}