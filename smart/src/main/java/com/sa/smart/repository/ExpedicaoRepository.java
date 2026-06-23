package com.sa.smart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sa.smart.model.Expedicao;

public interface ExpedicaoRepository extends JpaRepository<Expedicao, Long> {

    Optional<Expedicao> findByPosicaoExpedicao(Integer posicaoExpedicao);

    @Query("SELECT e.posicaoExpedicao FROM Expedicao e WHERE e.orderNumber IS NOT NULL AND e.orderNumber > 0")
    List<Integer> findAllPosicoesOcupadas();
}
