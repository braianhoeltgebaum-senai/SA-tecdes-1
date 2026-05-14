package com.sa.smart.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.sa.smart.dto.BlocoDTO;
import com.sa.smart.enums.EnumCorBloco;
import com.sa.smart.enums.EnumStatusPedido;
import com.sa.smart.enums.EnumTipoPedido;
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
        // Validação: dto.corBloco() é EnumCorBloco, então pegamos o código
        if (dto.corBloco() == null || !EnumCorBloco.isValid(dto.corBloco().getCodigo())) {
            throw new RuntimeException("Cor do bloco inválida. Use: 1-Preto, 2-Vermelho, 3-Azul");
        }

        if (dto.pedidoId() == null) {
            throw new RuntimeException("Pedido não informado.");
        }
        Pedido pedido = pedidoRepository.findById(dto.pedidoId())
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com ID: " + dto.pedidoId()));

        if (dto.estoqueId() == null) {
            throw new RuntimeException("Posição de estoque não informada.");
        }
        Estoque estoque = estoqueRepository.findById(dto.estoqueId())
                .orElseThrow(() -> new RuntimeException("Posição de estoque não encontrada com ID: " + dto.estoqueId()));

        if (estoque.getCor() == 0) {
            throw new RuntimeException("Posição de estoque vazia. Não é possível adicionar bloco.");
        }

        Bloco bloco = new Bloco();
        bloco.setPedido(pedido);
        bloco.setEstoque(estoque);
        bloco.setCorBloco(dto.corBloco().getCodigo()); // salva o código (Integer)
        bloco.setCriadoEm(LocalDateTime.now());

        Bloco salvo = blocoRepository.save(bloco);
        return new BlocoDTO(
            salvo.getIdBloco(),
            EnumCorBloco.fromCodigo(salvo.getCorBloco()), // converte Integer -> Enum
            salvo.getCriadoEm(),
            salvo.getEstoque() != null ? salvo.getEstoque().getId() : null,
            salvo.getPedido() != null ? salvo.getPedido().getId_pedido() : null
        );
    }

    public List<BlocoDTO> listar() {
        List<Bloco> blocos = blocoRepository.findAll();
        List<BlocoDTO> lista = new ArrayList<>();
        for (Bloco b : blocos) {
            lista.add(new BlocoDTO(
                b.getIdBloco(),
                EnumCorBloco.fromCodigo(b.getCorBloco()),
                b.getCriadoEm(),
                b.getEstoque() != null ? b.getEstoque().getId() : null,
                b.getPedido() != null ? b.getPedido().getId_pedido() : null
            ));
        }
        return lista;
    }

    public BlocoDTO buscar(Long id) {
        Bloco b = blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));
        return new BlocoDTO(
            b.getIdBloco(),
            EnumCorBloco.fromCodigo(b.getCorBloco()),
            b.getCriadoEm(),
            b.getEstoque() != null ? b.getEstoque().getId() : null,
            b.getPedido() != null ? b.getPedido().getId_pedido() : null
        );
    }

    @Transactional
    public BlocoDTO put(Long id, BlocoDTO dto) {
        Bloco existente = blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));

        if (dto.corBloco() != null) {
            if (!EnumCorBloco.isValid(dto.corBloco().getCodigo())) {
                throw new RuntimeException("Cor do bloco inválida. Use: 1-Preto, 2-Vermelho, 3-Azul");
            }
            existente.setCorBloco(dto.corBloco().getCodigo());
        }
        if (dto.pedidoId() != null) {
            Pedido p = pedidoRepository.findById(dto.pedidoId())
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + dto.pedidoId()));
            existente.setPedido(p);
        }
        if (dto.estoqueId() != null) {
            Estoque e = estoqueRepository.findById(dto.estoqueId())
                    .orElseThrow(() -> new RuntimeException("Estoque não encontrado: " + dto.estoqueId()));
            existente.setEstoque(e);
        }

        Bloco atualizado = blocoRepository.save(existente);
        return new BlocoDTO(
            atualizado.getIdBloco(),
            EnumCorBloco.fromCodigo(atualizado.getCorBloco()),
            atualizado.getCriadoEm(),
            atualizado.getEstoque() != null ? atualizado.getEstoque().getId() : null,
            atualizado.getPedido() != null ? atualizado.getPedido().getId_pedido() : null
        );
    }

    @Transactional
    public BlocoDTO patch(Long id, BlocoDTO dto) {
        Bloco existente = blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));

        if (dto.corBloco() != null && dto.corBloco().getCodigo() != 0) {
            if (!EnumCorBloco.isValid(dto.corBloco().getCodigo())) {
                throw new RuntimeException("Cor do bloco inválida. Use: 1-Preto, 2-Vermelho, 3-Azul");
            }
            existente.setCorBloco(dto.corBloco().getCodigo());
        }
        if (dto.pedidoId() != null) {
            Pedido p = pedidoRepository.findById(dto.pedidoId())
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + dto.pedidoId()));
            existente.setPedido(p);
        }
        if (dto.estoqueId() != null) {
            Estoque e = estoqueRepository.findById(dto.estoqueId())
                    .orElseThrow(() -> new RuntimeException("Estoque não encontrado: " + dto.estoqueId()));
            existente.setEstoque(e);
        }

        Bloco atualizado = blocoRepository.save(existente);
        return new BlocoDTO(
            atualizado.getIdBloco(),
            EnumCorBloco.fromCodigo(atualizado.getCorBloco()),
            atualizado.getCriadoEm(),
            atualizado.getEstoque() != null ? atualizado.getEstoque().getId() : null,
            atualizado.getPedido() != null ? atualizado.getPedido().getId_pedido() : null
        );
    }

    @Transactional
    public void deletar(Long id) {
        Bloco b = blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));
        blocoRepository.delete(b);
    }

    public List<BlocoDTO> listarBlocosDisponiveis() {
        List<Bloco> blocos = blocoRepository.findAll();
        List<BlocoDTO> lista = new ArrayList<>();
        for (Bloco b : blocos) {
            if (b.getEstoque() != null && b.getEstoque().getCor() != 0) {
                lista.add(new BlocoDTO(
                    b.getIdBloco(),
                    EnumCorBloco.fromCodigo(b.getCorBloco()),
                    b.getCriadoEm(),
                    b.getEstoque() != null ? b.getEstoque().getId() : null,
                    b.getPedido() != null ? b.getPedido().getId_pedido() : null
                ));
            }
        }
        return lista;
    }

    @Transactional
    public void atualizarStatusConcluido(Long id) {
        Bloco b = blocoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado com ID: " + id));
        if (b.getPedido() != null) {
            Pedido p = b.getPedido();
            if (p.getTipoPedido() != null && p.getTipoPedido().getCodigo() == EnumTipoPedido.TRIPLO.getCodigo()) {
                long qtd = blocoRepository.findAll().stream()
                        .filter(bl -> bl.getPedido() != null && bl.getPedido().getId_pedido().equals(p.getId_pedido()))
                        .count();
                if (qtd != 3) {
                    throw new RuntimeException("Pedidos triplos exigem 3 blocos. Atualmente: " + qtd);
                }
            }
            p.setStatusPedido(EnumStatusPedido.CONCLUIDO);
            pedidoRepository.save(p);
        }
        blocoRepository.save(b);
    }
}