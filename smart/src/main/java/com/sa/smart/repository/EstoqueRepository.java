package com.sa.smart.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sa.smart.model.Estoque;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    // Busca por posição exata
    Optional<Estoque> findByPosicaoEstoque(int posicao);

    // Busca a primeira posição disponível com a cor ordenada pelo campo
    // posicaoEstoque
    Optional<Estoque> findFirstByCorOrderByPosicaoEstoqueAsc(Integer cor);

    // Lista todas as posições com determinada cor ordenadas por posicaoEstoque
    List<Estoque> findByCorOrderByPosicaoEstoqueAsc(Integer cor);
}
