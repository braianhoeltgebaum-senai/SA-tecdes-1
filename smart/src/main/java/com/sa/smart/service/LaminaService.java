package com.sa.smart.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sa.smart.dto.LaminaDTO;
import com.sa.smart.model.Bloco;
import com.sa.smart.model.Lamina;
import com.sa.smart.repository.BlocoRepository;
import com.sa.smart.repository.LaminaRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class LaminaService {

    private final LaminaRepository repository;
    private final BlocoRepository blocoRepository;

    public LaminaService(LaminaRepository repository,
                         BlocoRepository blocoRepository) {
        this.repository = repository;
        this.blocoRepository = blocoRepository;
    }

    public LaminaDTO criar(LaminaDTO dto) {

        Bloco bloco = blocoRepository.findById(dto.blocoId())
        .orElseThrow(() ->
                new RuntimeException("Bloco não encontrado"));

        long quantidade = repository.countByBlocoId(bloco.getId());

        if (quantidade >= 3) {
            throw new RuntimeException(
                    "O bloco já possui 3 lâminas");
        }

        Lamina l = new Lamina();

        l.setCor(dto.cor());
        l.setPadrao(dto.padrao());
        l.setPosicaoNoBloco(dto.posicaoNoBloco());
        l.setBloco(bloco);

        Lamina saved = repository.save(l);

        return new LaminaDTO(
                saved.getId(),
                saved.getCor(),
                saved.getPadrao(),
                saved.getPosicaoNoBloco(),
                saved.getBloco().getId()
        );
    }
    
    public List<LaminaDTO> listar() {

        return repository.findAll().stream()
                .map(l -> new LaminaDTO(
                        l.getId(),
                        l.getCor(),
                        l.getPadrao(),
                        l.getPosicaoNoBloco(),
                        l.getBloco().getId()
                ))
                .toList();
    }

    public LaminaDTO buscar(Long id) {

        Lamina l = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Lâmina não encontrada"));

        return new LaminaDTO(
                l.getId(),
                l.getCor(),
                l.getPadrao(),
                l.getPosicaoNoBloco(),
                l.getBloco().getId()
        );
    }

    public LaminaDTO put(Long id, LaminaDTO dto) {

        Lamina l = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Lâmina não encontrada"));

        Bloco bloco = blocoRepository.findById(dto.blocoId())
                .orElseThrow(() ->
                        new RuntimeException("Bloco não encontrado"));

        // valida limite de 3 ao trocar de bloco
        if (!l.getBloco().getId().equals(bloco.getId())) {

            long quantidade = repository.countByBlocoId(bloco.getId());

            if (quantidade >= 3) {
                throw new RuntimeException(
                        "O bloco já possui 3 lâminas");
            }
        }

        l.setCor(dto.cor());
        l.setPadrao(dto.padrao());
        l.setPosicaoNoBloco(dto.posicaoNoBloco());
        l.setBloco(bloco);

        Lamina updated = repository.save(l);

        return new LaminaDTO(
                updated.getId(),
                updated.getCor(),
                updated.getPadrao(),
                updated.getPosicaoNoBloco(),
                updated.getBloco().getId()
        );
    }

    public LaminaDTO patch(Long id, LaminaDTO dto) {

        Lamina l = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Lâmina não encontrada"));

        if (dto.cor() != null) {
            l.setCor(dto.cor());
        }

        if (dto.padrao() != null) {
            l.setPadrao(dto.padrao());
        }

        if (dto.posicaoNoBloco() != null) {
            l.setPosicaoNoBloco(dto.posicaoNoBloco());
        }

        Lamina updated = repository.save(l);

        return new LaminaDTO(
                updated.getId(),
                updated.getCor(),
                updated.getPadrao(),
                updated.getPosicaoNoBloco(),
                updated.getBloco().getId()
        );
    }

    public void deletar(Long id) {

        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Lâmina não encontrada");
        }

        repository.deleteById(id);
    }
}