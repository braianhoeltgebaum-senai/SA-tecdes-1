package com.sa.smart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sa.smart.dto.BlocoDTO;
import com.sa.smart.enums.EnumCorBloco;
import com.sa.smart.enums.EnumTipoPedido;
import com.sa.smart.model.Bloco;
import com.sa.smart.model.Estoque;
import com.sa.smart.model.Pedido;
import com.sa.smart.repository.BlocoRepository;
import com.sa.smart.repository.EstoqueRepository;
import com.sa.smart.repository.PedidoRepository;
import com.sa.smart.service.BlocoService;

@ExtendWith(MockitoExtension.class)
class BlocoServiceTest {

    @Mock
    private BlocoRepository blocoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private EstoqueRepository estoqueRepository;

    @InjectMocks
    private BlocoService blocoService;

    private Bloco bloco;
    private Pedido pedido;
    private Estoque estoque;
    private BlocoDTO dto;

    @BeforeEach
    void setup() {

        pedido = new Pedido();
        pedido.setIdPedido(1L);
        pedido.setTipoPedido(EnumTipoPedido.SIMPLES.getCodigo());

        estoque = new Estoque();
        estoque.setId(1L);
        estoque.setCor(1);

        bloco = new Bloco();
        bloco.setIdBloco(1L);
        bloco.setCorBloco(1);
        bloco.setPedido(pedido);
        bloco.setEstoque(estoque);
        bloco.setCriadoEm(LocalDateTime.now());

        dto = new BlocoDTO(
                null,
                EnumCorBloco.PRETO,
                null,
                1L,
                1L
        );
    }

    @Test
    void deveCriarBloco() {

        when(pedidoRepository.findById(1L))
                .thenReturn(Optional.of(pedido));

        when(estoqueRepository.findById(1L))
                .thenReturn(Optional.of(estoque));

        when(blocoRepository.save(any(Bloco.class)))
                .thenReturn(bloco);

        BlocoDTO resultado = blocoService.criar(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.corBloco())
                .isEqualTo(EnumCorBloco.PRETO);

        verify(blocoRepository, times(1))
                .save(any(Bloco.class));
    }

    @Test
    void deveListarBlocos() {

        when(blocoRepository.findAll())
                .thenReturn(List.of(bloco));

        List<BlocoDTO> lista = blocoService.listar();

        assertThat(lista).hasSize(1);
    }

    @Test
    void deveBuscarBlocoPorId() {

        when(blocoRepository.findById(1L))
                .thenReturn(Optional.of(bloco));

        BlocoDTO resultado = blocoService.buscar(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
    }

    @Test
    void deveAtualizarBlocoComPut() {

        BlocoDTO atualizado = new BlocoDTO(
                null,
                EnumCorBloco.VERMELHO,
                null,
                1L,
                1L
        );

        when(blocoRepository.findById(1L))
                .thenReturn(Optional.of(bloco));

        when(pedidoRepository.findById(1L))
                .thenReturn(Optional.of(pedido));

        when(estoqueRepository.findById(1L))
                .thenReturn(Optional.of(estoque));

        when(blocoRepository.save(any(Bloco.class)))
                .thenReturn(bloco);

        BlocoDTO resultado = blocoService.put(1L, atualizado);

        assertThat(resultado).isNotNull();
    }

    @Test
    void deveAtualizarBlocoComPatch() {

        BlocoDTO patch = new BlocoDTO(
                null,
                EnumCorBloco.VERMELHO,
                null,
                null,
                null
        );

        when(blocoRepository.findById(1L))
                .thenReturn(Optional.of(bloco));

        when(blocoRepository.save(any(Bloco.class)))
                .thenReturn(bloco);

        BlocoDTO resultado = blocoService.patch(1L, patch);

        assertThat(resultado).isNotNull();

        verify(blocoRepository)
                .save(any(Bloco.class));
    }

    @Test
    void deveLancarErroAoCriarBlocoSemPedido() {

        BlocoDTO dtoInvalido = new BlocoDTO(
                null,
                EnumCorBloco.PRETO,
                null,
                1L,
                null
        );

        assertThatThrownBy(() -> blocoService.criar(dtoInvalido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pedido não informado");
    }

    @Test
    void deveLancarErroQuandoEstoqueEstiverVazio() {

        estoque.setCor(0);

        when(pedidoRepository.findById(1L))
                .thenReturn(Optional.of(pedido));

        when(estoqueRepository.findById(1L))
                .thenReturn(Optional.of(estoque));

        assertThatThrownBy(() -> blocoService.criar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("estoque vazia");
    }

    @Test
    void deveDeletarBloco() {

        when(blocoRepository.findById(1L))
                .thenReturn(Optional.of(bloco));

        doNothing().when(blocoRepository)
                .delete(bloco);

        blocoService.deletar(1L);

        verify(blocoRepository)
                .delete(bloco);
    }

    @Test
    void deveLancarErroQuandoBlocoNaoExiste() {

        when(blocoRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> blocoService.buscar(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bloco não encontrado");
    }

    @Test
    void deveAtualizarStatusConcluido() {

        when(blocoRepository.findById(1L))
                .thenReturn(Optional.of(bloco));

        when(blocoRepository.save(any(Bloco.class)))
                .thenReturn(bloco);

        blocoService.atualizarStatusConcluido(1L);

        verify(pedidoRepository)
                .save(any(Pedido.class));
    }
}