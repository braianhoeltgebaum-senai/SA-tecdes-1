package com.sa.smart.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sa.smart.dto.ExpedicaoDTO;
import com.sa.smart.model.Expedicao;
import com.sa.smart.model.Pedido;
import com.sa.smart.repository.ExpedicaoRepository;
import com.sa.smart.repository.PedidoRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ExpedicaoService {

    private final ExpedicaoRepository repository;
    private final PedidoRepository pedidoRepository;

    public ExpedicaoService(ExpedicaoRepository repository,
                            PedidoRepository pedidoRepository) {
        this.repository = repository;
        this.pedidoRepository = pedidoRepository;
    }

    // CREATE
    public ExpedicaoDTO criar(ExpedicaoDTO dto) {

        Pedido pedido = pedidoRepository
                .findByOrdemProducao(dto.ordemProducao())
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado"));

        Expedicao e = new Expedicao();

        e.setPosicaoExpedicao(dto.posicaoExpedicao());
        e.setEntradaEm(dto.entradaEm());
        e.setSaidaEm(dto.saidaEm());
        e.setPedido(pedido);

        Expedicao saved = repository.save(e);

        return new ExpedicaoDTO(
                saved.getId(),
                saved.getPosicaoExpedicao(),
                saved.getEntradaEm(),
                saved.getSaidaEm(),
                saved.getPedido().getOrdemProducao()
        );
    }

    // READ ALL
    public List<ExpedicaoDTO> listar() {

        return repository.findAll().stream()
                .map(e -> new ExpedicaoDTO(
                        e.getId(),
                        e.getPosicaoExpedicao(),
                        e.getEntradaEm(),
                        e.getSaidaEm(),
                        e.getPedido().getOrdemProducao()
                ))
                .toList();
    }

    // READ BY ID
    public ExpedicaoDTO buscar(Long id) {

        Expedicao e = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Expedição não encontrada"));

        return new ExpedicaoDTO(
                e.getId(),
                e.getPosicaoExpedicao(),
                e.getEntradaEm(),
                e.getSaidaEm(),
                e.getPedido().getOrdemProducao()
        );
    }

    // PUT
    public ExpedicaoDTO put(Long id, ExpedicaoDTO dto) {

        Expedicao e = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Expedição não encontrada"));

        Pedido pedido = pedidoRepository
                .findByOrdemProducao(dto.ordemProducao())
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado"));

        e.setPosicaoExpedicao(dto.posicaoExpedicao());
        e.setEntradaEm(dto.entradaEm());
        e.setSaidaEm(dto.saidaEm());
        e.setPedido(pedido);

        Expedicao updated = repository.save(e);

        return new ExpedicaoDTO(
                updated.getId(),
                updated.getPosicaoExpedicao(),
                updated.getEntradaEm(),
                updated.getSaidaEm(),
                updated.getPedido().getOrdemProducao()
        );
    }

    // PATCH
    public ExpedicaoDTO patch(Long id, ExpedicaoDTO dto) {

        Expedicao e = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Expedição não encontrada"));

        if (dto.posicaoExpedicao() != null) {
            e.setPosicaoExpedicao(dto.posicaoExpedicao());
        }

        if (dto.entradaEm() != null) {
            e.setEntradaEm(dto.entradaEm());
        }

        if (dto.saidaEm() != null) {
            e.setSaidaEm(dto.saidaEm());
        }

        if (dto.ordemProducao() != null) {

            Pedido pedido = pedidoRepository
                    .findByOrdemProducao(dto.ordemProducao())
                    .orElseThrow(() ->
                            new RuntimeException("Pedido não encontrado"));

            e.setPedido(pedido);
        }

        Expedicao updated = repository.save(e);

        return new ExpedicaoDTO(
                updated.getId(),
                updated.getPosicaoExpedicao(),
                updated.getEntradaEm(),
                updated.getSaidaEm(),
                updated.getPedido().getOrdemProducao()
        );
    }

    // DELETE
    public void deletar(Long id) {

        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Expedição não encontrada");
        }

        repository.deleteById(id);
    }
}