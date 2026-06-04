package com.sa.smart.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.smart.dto.ExpedicaoDTO;
import com.sa.smart.model.Expedicao;
import com.sa.smart.model.Pedido;
import com.sa.smart.repository.ExpedicaoRepository;
import com.sa.smart.repository.PedidoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

@Service
public class ExpedicaoService {

    private final ExpedicaoRepository expedicaoRepository;
    private final PedidoRepository pedidoRepository;
    private final EntityManager entityManager;

    public ExpedicaoService(
            ExpedicaoRepository expedicaoRepository,
            PedidoRepository pedidoRepository,
            EntityManager entityManager) {

        this.expedicaoRepository = expedicaoRepository;
        this.pedidoRepository = pedidoRepository;
        this.entityManager = entityManager;
    }



    @Transactional
    public ExpedicaoDTO criar(ExpedicaoDTO dto) {
     
        Pedido pedido = buscarPedidoPorOrdemProducao(dto.ordemProducao());

        Expedicao e = new Expedicao();
        e.setPosicaoExpedicao(dto.posicaoExpedicao());
        e.setEntradaEm(dto.entradaEm());
        e.setSaidaEm(dto.saidaEm());
        e.setPedido(pedido);

        Expedicao saved = expedicaoRepository.save(e);

        return new ExpedicaoDTO(
                saved.getId(),
                saved.getPosicaoExpedicao(),
                saved.getEntradaEm(),
                saved.getSaidaEm(),
                saved.getPedido().getOrdemProducao()
        );
    }

    @Transactional(readOnly = true)
    public List<ExpedicaoDTO> listar() {
        return expedicaoRepository.findAll().stream()
                .map(e -> new ExpedicaoDTO(
                        e.getId(),
                        e.getPosicaoExpedicao(),
                        e.getEntradaEm(),
                        e.getSaidaEm(),
                        e.getPedido().getOrdemProducao()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpedicaoDTO buscar(Long id) {
        Expedicao e = expedicaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expedição não encontrada"));
        return new ExpedicaoDTO(
                e.getId(),
                e.getPosicaoExpedicao(),
                e.getEntradaEm(),
                e.getSaidaEm(),
                e.getPedido().getOrdemProducao()
        );
    }

    @Transactional
    public ExpedicaoDTO put(Long id, ExpedicaoDTO dto) {
        Expedicao e = expedicaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expedição não encontrada"));

        Pedido pedido = buscarPedidoPorOrdemProducao(dto.ordemProducao());

        e.setPosicaoExpedicao(dto.posicaoExpedicao());
        e.setEntradaEm(dto.entradaEm());
        e.setSaidaEm(dto.saidaEm());
        e.setPedido(pedido);

        Expedicao updated = expedicaoRepository.save(e);

        return new ExpedicaoDTO(
                updated.getId(),
                updated.getPosicaoExpedicao(),
                updated.getEntradaEm(),
                updated.getSaidaEm(),
                updated.getPedido().getOrdemProducao()
        );
    }

    @Transactional
    public ExpedicaoDTO patch(Long id, ExpedicaoDTO dto) {
        Expedicao e = expedicaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expedição não encontrada"));

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
            Pedido pedido = buscarPedidoPorOrdemProducao(dto.ordemProducao());
            e.setPedido(pedido);
        }

        Expedicao updated = expedicaoRepository.save(e);

        return new ExpedicaoDTO(
                updated.getId(),
                updated.getPosicaoExpedicao(),
                updated.getEntradaEm(),
                updated.getSaidaEm(),
                updated.getPedido().getOrdemProducao()
        );
    }

    @Transactional
    public void deletar(Long id) {
        if (!expedicaoRepository.existsById(id)) {
            throw new EntityNotFoundException("Expedição não encontrada");
        }
        expedicaoRepository.deleteById(id);
    }

    private Pedido buscarPedidoPorOrdemProducao(String ordemProducao) {
        String jpql = "SELECT p FROM Pedido p WHERE p.ordemProducao = :ordemProducao";
        return entityManager.createQuery(jpql, Pedido.class)
                .setParameter("ordemProducao", ordemProducao)
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com ordemProducao: " + ordemProducao));
    }
}
