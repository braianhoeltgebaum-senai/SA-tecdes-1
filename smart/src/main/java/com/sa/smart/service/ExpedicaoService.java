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
                .findByOrdemProducao(dto.ordem_producao())
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado"));

        Expedicao e = new Expedicao();

        e.setPosicao_expedicao(dto.posicao_expedicao());
        e.setEntrada_em(dto.entrada_em());
        e.setSaida_em(dto.saida_em());
        e.setPedido(pedido);

        Expedicao saved = repository.save(e);

        return new ExpedicaoDTO(
                saved.getId(),
                saved.getPosicao_expedicao(),
                saved.getEntrada_em(),
                saved.getSaida_em(),
                saved.getPedido().getOrdem_producao()
        );
    }

    // READ ALL
    public List<ExpedicaoDTO> listar() {

        return repository.findAll().stream()
                .map(e -> new ExpedicaoDTO(
                        e.getId(),
                        e.getPosicao_expedicao(),
                        e.getEntrada_em(),
                        e.getSaida_em(),
                        e.getPedido().getOrdem_producao()
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
                e.getPosicao_expedicao(),
                e.getEntrada_em(),
                e.getSaida_em(),
                e.getPedido().getOrdem_producao()
        );
    }

    // PUT
    public ExpedicaoDTO put(Long id, ExpedicaoDTO dto) {

        Expedicao e = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Expedição não encontrada"));

        Pedido pedido = pedidoRepository
                .findByOrdemProducao(dto.ordem_producao())
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado"));

        e.setPosicao_expedicao(dto.posicao_expedicao());
        e.setEntrada_em(dto.entrada_em());
        e.setSaida_em(dto.saida_em());
        e.setPedido(pedido);

        Expedicao updated = repository.save(e);

        return new ExpedicaoDTO(
                updated.getId(),
                updated.getPosicao_expedicao(),
                updated.getEntrada_em(),
                updated.getSaida_em(),
                updated.getPedido().getOrdem_producao()
        );
    }

    // PATCH
    public ExpedicaoDTO patch(Long id, ExpedicaoDTO dto) {

        Expedicao e = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Expedição não encontrada"));

        if (dto.posicao_expedicao() != null) {
            e.setPosicao_expedicao(dto.posicao_expedicao());
        }

        if (dto.entrada_em() != null) {
            e.setEntrada_em(dto.entrada_em());
        }

        if (dto.saida_em() != null) {
            e.setSaida_em(dto.saida_em());
        }

        if (dto.ordem_producao() != null) {

            Pedido pedido = pedidoRepository
                    .findByOrdemProducao(dto.ordem_producao())
                    .orElseThrow(() ->
                            new RuntimeException("Pedido não encontrado"));

            e.setPedido(pedido);
        }

        Expedicao updated = repository.save(e);

        return new ExpedicaoDTO(
                updated.getId(),
                updated.getPosicao_expedicao(),
                updated.getEntrada_em(),
                updated.getSaida_em(),
                updated.getPedido().getOrdem_producao()
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