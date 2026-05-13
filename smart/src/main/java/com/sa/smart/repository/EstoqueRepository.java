package com.sa.smart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sa.smart.model.Estoque;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

}
