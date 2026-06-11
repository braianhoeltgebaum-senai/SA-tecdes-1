// package com.sa.smart.model;

// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;

// import com.fasterxml.jackson.annotation.JsonBackReference;
// import com.fasterxml.jackson.annotation.JsonManagedReference;

// import jakarta.persistence.CascadeType;
// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.JoinColumn;
// import jakarta.persistence.ManyToOne;
// import jakarta.persistence.OneToMany;
// import jakarta.persistence.PrePersist;
// import lombok.AllArgsConstructor;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// @Entity
// @Data
// @NoArgsConstructor
// @AllArgsConstructor
// public class Bloco {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long idBloco;

//     @JsonBackReference
//     @ManyToOne
//     @JoinColumn(name = "pedido_id", nullable = false)
//     private Pedido pedido;

//     @ManyToOne
//     @JoinColumn(name = "estoque_id", nullable = false)
//     private Estoque estoque;

//     @Column(name = "cor_bloco", nullable = false)
//     private Integer corBloco;

//     @Column(name = "criado_em", nullable = false)
//     private LocalDateTime criadoEm;

//     @JsonManagedReference
//     @OneToMany(mappedBy = "bloco",
//             cascade = CascadeType.ALL,
//             orphanRemoval = true)
//     private List<Lamina> laminas = new ArrayList<>();

//     @PrePersist
//     public void prePersist() {
//         this.criadoEm = LocalDateTime.now();
//     }

//     public void adicionarLamina(Lamina lamina) {
//         laminas.add(lamina);
//         lamina.setBloco(this);
//     }
// }

package com.sa.smart.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
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

    @Transient
    private Long estoqueId;

    @ManyToOne
    @JoinColumn(name = "pedido_ordem_producao", nullable = false)
    @JsonIgnore
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "estoque_posicao", nullable = false)
    private Estoque estoque;

    @Column(name = "cor_bloco", nullable = false)
    private Integer corBloco;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @JsonManagedReference
    @OneToMany(mappedBy = "bloco", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lamina> laminas = new ArrayList<>();

    public void adicionarLamina(Lamina lamina) {

        laminas.add(lamina);
        lamina.setBloco(this);

    }

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}