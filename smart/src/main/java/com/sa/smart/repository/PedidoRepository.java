package com.sa.smart.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sa.smart.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    long countByStatusPedido(Integer statusPedido);

    List<Pedido> findTop10ByOrderByTimestampDesc();

    List<Pedido> findByStatusPedido(Integer statusPedido);
}