package com.sa.smart.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.sa.smart.model.Bloco;
import com.sa.smart.model.Estoque;
import com.sa.smart.repository.BlocoRepository;
import com.sa.smart.repository.EstoqueRepository;
import com.sa.smart.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlocoService {

    private final BlocoRepository blocoRepository;
    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;

    public List<Bloco> listarTodos() {

        return blocoRepository.findAll();

    }

    public Bloco buscarPorId(Long id) {

        return blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));

    }

    @Transactional
    public Bloco salvarBloco(Bloco bloco) {

        if (bloco.getPedido() == null || bloco.getPedido().getOrdemProducao() == null) {

            throw new RuntimeException("Pedido não informado.");

        }

        Pedido pedido = pedidoRepository.findByOrdemProducao(bloco.getPedido().getOrdemProducao())
                .orElseThrow(() -> new RuntimeException(
                        "Pedido de produção não encontrado: " + bloco.getPedido().getOrdemProducao()));

        bloco.setPedido(pedido);

        if (bloco.getEstoque() == null || bloco.getEstoque().getPosicao() == null) {

            throw new RuntimeException("Posição de estoque não informada.");

        }

        Estoque estoque = estoqueRepository.findByPosicao(bloco.getEstoque().getPosicao())
                .orElseThrow(() -> new RuntimeException(
                        "Posição de estoque não encontrada: " + bloco.getEstoque().getPosicao()));

        if (estoque.getCor() == 0) {

            throw new RuntimeException("Posição de estoque vazia. Não é possível adicionar bloco.");

        }

        bloco.setEstoque(estoque);

        if (bloco.getCorBloco() == null || bloco.getCorBloco() == 0) {

            throw new RuntimeException("Cor do bloco inválida.");

        }

        if (bloco.getLaminas() != null && bloco.getLaminas().size() > 3) {

            throw new RuntimeException("Cada bloco pode ter no máximo 3 lâminas.");

        }

        bloco.setCriadoEm(LocalDateTime.now());

        return blocoRepository.save(bloco);

    }

    @Transactional
    public Bloco atualizarBloco(Long id, Bloco blocoAtualizado) {

        Bloco blocoExistente = buscarPorId(id);

        if (blocoAtualizado.getPedido() != null) {

            Pedido pedido = pedidoRepository.findByOrdemProducao(blocoAtualizado.getPedido().getOrdemProducao())
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

            blocoExistente.setPedido(pedido);

        }

        if (blocoAtualizado.getEstoque() != null) {

            Estoque estoque = estoqueRepository.findByPosicao(blocoAtualizado.getEstoque().getPosicao())
                    .orElseThrow(() -> new RuntimeException("Posição de estoque não encontrada"));

            blocoExistente.setEstoque(estoque);

        }

        if (blocoAtualizado.getCorBloco() != null) {

            blocoExistente.setCorBloco(blocoAtualizado.getCorBloco());

        }

        return blocoRepository.save(blocoExistente);

    }

    @Transactional
    public void deletarBloco(Long id) {

        Bloco bloco = buscarPorId(id);
        blocoRepository.delete(bloco);

    }

    public List<Bloco> buscarPorPedido(Pedido pedido) {

        return blocoRepository.findByPedido(pedido);

    }

    public long contarBlocosPorPedido(Pedido pedido) {

        return blocoRepository.countByPedido(pedido);

    }

    public List<Bloco> listarBlocosDisponiveis() {

        return blocoRepository.findAll().stream()
                .filter(bloco -> bloco.getEstoque() != null && bloco.getEstoque().getCor() != 0)
                .collect(Collectors.toList());

    }

    @Transactional
    public void atualizarStatusConcluido(Long id) {

        Bloco bloco = buscarPorId(id);

        if (bloco.getPedido() != null) {

            Pedido pedido = bloco.getPedido();

            if (pedido.getTipo() == 3) {

                long quantidadeBlocos = contarBlocosPorPedido(pedido);

                if (quantidadeBlocos != 3) {

                    throw new RuntimeException(
                            "Pedidos triplos exigem exatamente 3 blocos. Atualmente: " + quantidadeBlocos);

                }
            }

            pedido.setStatus(3);
            pedido.setConcluidoEm(LocalDateTime.now());

        }

        blocoRepository.save(bloco);

    }

    public void validarPedidoTriplo(Pedido pedido, List<Bloco> blocos) {

        if (pedido.getTipo() == 3 && blocos.size() != 3) {
            
            throw new RuntimeException("Pedidos triplos exigem exatamente 3 blocos.");

        }
    }
}