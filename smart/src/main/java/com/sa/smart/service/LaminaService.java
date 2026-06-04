package com.sa.smart.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.smart.dto.LaminaDTO;
import com.sa.smart.model.Bloco;
import com.sa.smart.model.Lamina;
import com.sa.smart.repository.BlocoRepository;
import com.sa.smart.repository.LaminaRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class LaminaService {

    private final LaminaRepository laminaRepository;
    private final BlocoRepository blocoRepository;

 
    public LaminaService(
            LaminaRepository laminaRepository,
            BlocoRepository blocoRepository) {

        this.laminaRepository = laminaRepository;
        this.blocoRepository = blocoRepository;
    }

    @Transactional
    public LaminaDTO criar(LaminaDTO dto) {

        Bloco bloco = blocoRepository.findById(dto.blocoIdBloco())
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado"));

        if (dto.posicaoNoBloco() < 1 || dto.posicaoNoBloco() > 3) {
            throw new RuntimeException("A posição no bloco deve ser entre 1 e 3");
        }

        long quantidade = laminaRepository.countByBlocoIdBloco(bloco.getIdBloco());

        if (quantidade >= 3) {
            throw new RuntimeException("O bloco já possui 3 lâminas");
        }

        Lamina l = new Lamina();
        l.setCor(dto.cor());
        l.setPadrao(dto.padrao());
        l.setPosicaoNoBloco(dto.posicaoNoBloco());
        l.setBloco(bloco);

        Lamina saved = laminaRepository.save(l);

        return new LaminaDTO(
                saved.getId(),
                saved.getCor(),
                saved.getPadrao(),
                saved.getPosicaoNoBloco(),
                saved.getBloco().getIdBloco()
        );
    }

    @Transactional(readOnly = true)
    public List<LaminaDTO> listar() {
        return laminaRepository.findAll().stream()
                .map(l -> new LaminaDTO(
                        l.getId(),
                        l.getCor(),
                        l.getPadrao(),
                        l.getPosicaoNoBloco(),
                        l.getBloco().getIdBloco()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public LaminaDTO buscar(Long id) {
        Lamina l = laminaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lâmina não encontrada"));

        return new LaminaDTO(
                l.getId(),
                l.getCor(),
                l.getPadrao(),
                l.getPosicaoNoBloco(),
                l.getBloco().getIdBloco()
        );
    }

    @Transactional
    public LaminaDTO put(Long id, LaminaDTO dto) {
        Lamina l = laminaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lâmina não encontrada"));

        Bloco novoBloco = blocoRepository.findById(dto.blocoIdBloco())
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado"));

        if (dto.posicaoNoBloco() < 1 || dto.posicaoNoBloco() > 3) {
            throw new RuntimeException("A posição no bloco deve ser entre 1 e 3");
        }


        if (!l.getBloco().getIdBloco().equals(novoBloco.getIdBloco())) {

            long quantidade = laminaRepository.countByBlocoIdBloco(novoBloco.getIdBloco());

            if (quantidade >= 3) {
                throw new RuntimeException("O bloco já possui 3 lâminas");
            }
        }

        l.setCor(dto.cor());
        l.setPadrao(dto.padrao());
        l.setPosicaoNoBloco(dto.posicaoNoBloco());
        l.setBloco(novoBloco);

        Lamina updated = laminaRepository.save(l);

        return new LaminaDTO(
                updated.getId(),
                updated.getCor(),
                updated.getPadrao(),
                updated.getPosicaoNoBloco(),
                updated.getBloco().getIdBloco()
        );
    }

    @Transactional
    public LaminaDTO patch(Long id, LaminaDTO dto) {
        Lamina l = laminaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lâmina não encontrada"));

        if (dto.cor() != null) {
            l.setCor(dto.cor());
        }

        if (dto.padrao() != null) {
            l.setPadrao(dto.padrao());
        }

        if (dto.posicaoNoBloco() != null) {
            if (dto.posicaoNoBloco() < 1 || dto.posicaoNoBloco() > 3) {
                throw new RuntimeException("A posição no bloco deve ser entre 1 e 3");
            }
            l.setPosicaoNoBloco(dto.posicaoNoBloco());
        }

        Lamina updated = laminaRepository.save(l);

        return new LaminaDTO(
                updated.getId(),
                updated.getCor(),
                updated.getPadrao(),
                updated.getPosicaoNoBloco(),
                updated.getBloco().getIdBloco()
        );
    }

    @Transactional
    public void deletar(Long id) {
        if (!laminaRepository.existsById(id)) {
            throw new EntityNotFoundException("Lâmina não encontrada");
        }
        laminaRepository.deleteById(id);
    }
}