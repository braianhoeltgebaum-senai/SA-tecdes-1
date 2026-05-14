package com.sa.smart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sa.smart.model.Pedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

   
    Optional<Pedido> findByOrdemProducao(String ordemProducao);


    boolean existsByOrdemProducao(String ordemProducao);

}