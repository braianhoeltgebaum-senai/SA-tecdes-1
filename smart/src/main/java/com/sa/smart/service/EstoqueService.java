package com.sa.smart.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sa.smart.dto.EstoqueDTO;
import com.sa.smart.model.Estoque;
import com.sa.smart.repository.EstoqueRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class EstoqueService {

    private final EstoqueRepository repository;

    public EstoqueService(EstoqueRepository repository) {
        this.repository = repository;
    }

    public EstoqueDTO criar(EstoqueDTO dto) {

        Estoque e = new Estoque();

        e.setPosicao(dto.posicao());
        e.setCor(dto.cor());

        Estoque saved = repository.save(e);

        return new EstoqueDTO(
                saved.getId(),
                saved.getPosicao(),
                saved.getCor()
        );
    }


    public List<EstoqueDTO> listar() {

        return repository.findAll().stream()
                .map(e -> new EstoqueDTO(
                        e.getId(),
                        e.getPosicao(),
                        e.getCor()
                ))
                .toList();
    }


    public EstoqueDTO buscar(Long id) {

        Estoque e = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Estoque não encontrado"));

        return new EstoqueDTO(
                e.getId(),
                e.getPosicao(),
                e.getCor()
        );
    }


    public EstoqueDTO put(Long id, EstoqueDTO dto) {

        Estoque e = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Estoque não encontrado"));

        e.setPosicao(dto.posicao());
        e.setCor(dto.cor());

        Estoque updated = repository.save(e);

        return new EstoqueDTO(
                updated.getId(),
                updated.getPosicao(),
                updated.getCor()
        );
    }


    public EstoqueDTO patch(Long id, EstoqueDTO dto) {

        Estoque e = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Estoque não encontrado"));

        if (dto.posicao() != null) {
            e.setPosicao(dto.posicao());
        }

        if (dto.cor() != null) {
            e.setCor(dto.cor());
        }

        Estoque updated = repository.save(e);

        return new EstoqueDTO(
                updated.getId(),
                updated.getPosicao(),
                updated.getCor()
        );
    }

    public void deletar(Long id) {

        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Estoque não encontrado");
        }

        repository.deleteById(id);
    }
}