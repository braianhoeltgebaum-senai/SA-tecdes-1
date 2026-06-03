package com.sa.smart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sa.smart.model.Lamina;

@Repository
public interface LaminaRepository extends JpaRepository<Lamina, Long> {
    // Adicione esta linha para corrigir o erro "method undefined"
    long countByBlocoIdBloco(Long idBloco);
}