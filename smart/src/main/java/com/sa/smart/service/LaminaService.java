package com.sa.smart.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sa.smart.dto.LaminaDTO;
import com.sa.smart.model.Bloco;
import com.sa.smart.model.Lamina;
import com.sa.smart.repository.BlocoRepository;
import com.sa.smart.repository.LaminaRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LaminaService {

    private final LaminaRepository repository;
    private final BlocoRepository blocoRepository;

    public LaminaDTO criar(LaminaDTO dto) {

        Bloco bloco = buscarBloco(dto.blocoId());

        validarQuantidadeLaminas(bloco);

        Lamina lamina = new Lamina();

        lamina.setCor(dto.cor());
        lamina.setPadrao(dto.padrao());
        lamina.setPosicaoNoBloco(dto.posicaoNoBloco());
        lamina.setBloco(bloco);

        Lamina salva = repository.save(lamina);

        return converterParaDTO(salva);
    }

    public List<LaminaDTO> listar() {

        return repository.findAll()
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    public LaminaDTO buscar(Long id) {

        Lamina lamina = buscarLamina(id);

        return converterParaDTO(lamina);
    }

    public LaminaDTO put(Long id, LaminaDTO dto) {

        Lamina lamina = buscarLamina(id);

        Bloco bloco = buscarBloco(dto.blocoId());

        if (!lamina.getBloco().getIdBloco().equals(bloco.getIdBloco())) {

            validarQuantidadeLaminas(bloco);
        }

        lamina.setCor(dto.cor());
        lamina.setPadrao(dto.padrao());
        lamina.setPosicaoNoBloco(dto.posicaoNoBloco());
        lamina.setBloco(bloco);

        Lamina atualizada = repository.save(lamina);

        return converterParaDTO(atualizada);
    }

    public LaminaDTO patch(Long id, LaminaDTO dto) {

        Lamina lamina = buscarLamina(id);

        if (dto.cor() != null) {
            lamina.setCor(dto.cor());
        }

        if (dto.padrao() != null) {
            lamina.setPadrao(dto.padrao());
        }

        if (dto.posicaoNoBloco() != null) {
            lamina.setPosicaoNoBloco(dto.posicaoNoBloco());
        }

        if (dto.blocoId() != null) {

            Bloco bloco = buscarBloco(dto.blocoId());

            if (!lamina.getBloco().getIdBloco().equals(bloco.getIdBloco())) {

                validarQuantidadeLaminas(bloco);
            }

            lamina.setBloco(bloco);
        }

        Lamina atualizada = repository.save(lamina);

        return converterParaDTO(atualizada);
    }

    public void deletar(Long id) {

        if (!repository.existsById(id)) {

            throw new EntityNotFoundException(
                    "Lâmina não encontrada");
        }

        repository.deleteById(id);
    }

    private Lamina buscarLamina(Long id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Lâmina não encontrada"));
    }

    private Bloco buscarBloco(Long id) {

        return blocoRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Bloco não encontrado"));
    }

    private void validarQuantidadeLaminas(Bloco bloco) {

        long quantidade =
                repository.countByBlocoId(bloco.getIdBloco());

        if (quantidade >= 3) {

            throw new IllegalArgumentException(
                    "O bloco já possui 3 lâminas");
        }
    }

    private LaminaDTO converterParaDTO(Lamina lamina) {

        return new LaminaDTO(
                lamina.getId(),
                lamina.getCor(),
                lamina.getPadrao(),
                lamina.getPosicaoNoBloco(),
                lamina.getBloco().getIdBloco()
        );
    }
}