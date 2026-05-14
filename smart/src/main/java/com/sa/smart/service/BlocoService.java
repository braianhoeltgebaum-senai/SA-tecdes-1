package com.sa.smart.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.sa.smart.dto.BlocoDTO;
import com.sa.smart.model.Bloco;
import com.sa.smart.model.Estoque;
import com.sa.smart.model.Pedido;
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

    @Transactional
    public BlocoDTO criar(BlocoDTO dto) {

        Bloco bloco = new Bloco();

        if (dto.getCorBloco() == null || !CorBloco.isValid(dto.getCorBloco())) {

            throw new RuntimeException("Cor do bloco inválida. Use: 1-Preto, 2-Vermelho, 3-Azul");

        }

        if (dto.getPedidoOrdemProducao() != null) {

            Pedido pedido = pedidoRepository.findByOrdemProducao(dto.getPedidoOrdemProducao())
                    .orElseThrow(() -> new RuntimeException("Pedido de produção não encontrado: " + dto.getPedidoOrdemProducao()));
            bloco.setPedido(pedido);

        } else {

            throw new RuntimeException("Pedido não informado.");

        }

        if (dto.getEstoquePosicao() != null) {

            Estoque estoque = estoqueRepository.findByPosicao(dto.getEstoquePosicao())
                    .orElseThrow(() -> new RuntimeException("Posição de estoque não encontrada: " + dto.getEstoquePosicao()));

            if (estoque.getCor() == 0) {

                throw new RuntimeException("Posição de estoque vazia. Não é possível adicionar bloco.");

            }

            bloco.setEstoque(estoque);

        } else {

            throw new RuntimeException("Posição de estoque não informada.");

        }

        bloco.setCorBloco(dto.getCorBloco());
        bloco.setCriadoEm(LocalDateTime.now());

        Bloco salvo = blocoRepository.save(bloco);
        return BlocoDTO.fromEntity(salvo);

    }

    public List<BlocoDTO> listar() {

        List<Bloco> blocos = blocoRepository.findAll();
        List<BlocoDTO> listaDTO = new ArrayList<>();

        for (Bloco bloco : blocos) {

            listaDTO.add(BlocoDTO.fromEntity(bloco));

        }

        return listaDTO;

    }

    public BlocoDTO buscar(Long id) {

        Bloco bloco = blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));

        return BlocoDTO.fromEntity(bloco);

    }

    @Transactional
    public BlocoDTO put(Long id, BlocoDTO dto) {

        Bloco blocoExistente = blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));

        if (dto.getCorBloco() != null && !CorBloco.isValid(dto.getCorBloco())) {

            throw new RuntimeException("Cor do bloco inválida. Use: 1-Preto, 2-Vermelho, 3-Azul");

        }

        if (dto.getPedidoOrdemProducao() != null) {

            Pedido pedido = pedidoRepository.findByOrdemProducao(dto.getPedidoOrdemProducao())
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + dto.getPedidoOrdemProducao()));

            blocoExistente.setPedido(pedido);

        }

        if (dto.getEstoquePosicao() != null) {

            Estoque estoque = estoqueRepository.findByPosicao(dto.getEstoquePosicao())
                    .orElseThrow(() -> new RuntimeException("Posição de estoque não encontrada: " + dto.getEstoquePosicao()));

            blocoExistente.setEstoque(estoque);

        }

        if (dto.getCorBloco() != null) {

            blocoExistente.setCorBloco(dto.getCorBloco());

        }

        Bloco atualizado = blocoRepository.save(blocoExistente);
        return BlocoDTO.fromEntity(atualizado);

    }

    @Transactional
    public BlocoDTO patch(Long id, BlocoDTO dto) {

        Bloco blocoExistente = blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));

        if (dto.getCorBloco() != null && dto.getCorBloco() != 0 && !CorBloco.isValid(dto.getCorBloco())) {

            throw new RuntimeException("Cor do bloco inválida. Use: 1-Preto, 2-Vermelho, 3-Azul");

        }

        if (dto.getPedidoOrdemProducao() != null) {

            Pedido pedido = pedidoRepository.findByOrdemProducao(dto.getPedidoOrdemProducao())
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + dto.getPedidoOrdemProducao()));

            blocoExistente.setPedido(pedido);

        }

        if (dto.getEstoquePosicao() != null) {

            Estoque estoque = estoqueRepository.findByPosicao(dto.getEstoquePosicao())
                    .orElseThrow(() -> new RuntimeException("Posição de estoque não encontrada: " + dto.getEstoquePosicao()));

            blocoExistente.setEstoque(estoque);

        }

        if (dto.getCorBloco() != null && dto.getCorBloco() != 0) {

            blocoExistente.setCorBloco(dto.getCorBloco());

        }

        Bloco atualizado = blocoRepository.save(blocoExistente);
        return BlocoDTO.fromEntity(atualizado);

    }

    @Transactional
    public void deletar(Long id) {

        Bloco bloco = blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));

        blocoRepository.delete(bloco);

    }

    public List<BlocoDTO> listarBlocosDisponiveis() {

        List<Bloco> blocos = blocoRepository.findAll();
        List<BlocoDTO> listaDTO = new ArrayList<>();

        for (Bloco bloco : blocos) {

            if (bloco.getEstoque() != null && bloco.getEstoque().getCor() != 0) {

                listaDTO.add(BlocoDTO.fromEntity(bloco));

            }

        }

        return listaDTO;

    }

    @Transactional
    public void atualizarStatusConcluido(Long id) {

        Bloco bloco = blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));

        if (bloco.getPedido() != null) {

            Pedido pedido = bloco.getPedido();

            if (pedido.getTipoPedido() == 3) {

                long quantidadeBlocos = contarBlocosPorPedido(pedido);

                if (quantidadeBlocos != 3) {

                    throw new RuntimeException(
                            "Pedidos triplos exigem exatamente 3 blocos. Atualmente: " + quantidadeBlocos);

                }
            }

            pedido.setStatusPedido(3);
            pedido.setConcluidoEm(LocalDateTime.now());

        }

        blocoRepository.save(bloco);

    }

    public List<Bloco> listarTodos() {

        return blocoRepository.findAll();

    }

    public Bloco buscarPorId(Long id) {

        return blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));

    }

    @Transactional
    public Bloco salvarBloco(Bloco bloco) {

        if (bloco.getCorBloco() == null || !CorBloco.isValid(bloco.getCorBloco())) {

            throw new RuntimeException("Cor do bloco inválida. Use: 1-Preto, 2-Vermelho, 3-Azul");

        }

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

            if (!CorBloco.isValid(blocoAtualizado.getCorBloco())) {
                throw new RuntimeException("Cor do bloco inválida. Use: 1-Preto, 2-Vermelho, 3-Azul");
            }
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

    public void validarPedidoTriplo(Pedido pedido, List<Bloco> blocos) {

        if (pedido.getTipoPedido() == 3 && blocos.size() != 3) {

            throw new RuntimeException("Pedidos triplos exigem exatamente 3 blocos.");

        }
    }
}