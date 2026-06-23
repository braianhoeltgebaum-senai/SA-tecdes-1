package com.sa.smart.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sa.smart.model.Estoque;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findFirstByCorOrderByPosicaoAsc(Integer cor);

    Optional<Estoque> findByPosicaoEstoque(Integer posicaoEstoque);

    List<Estoque> findByCorOrderByPosicaoEstoqueAsc(Integer cor);
}
