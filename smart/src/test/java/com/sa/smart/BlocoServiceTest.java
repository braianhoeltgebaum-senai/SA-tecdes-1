package com.sa.smart;

<<<<<<< Updated upstream
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
=======
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
>>>>>>> Stashed changes
import org.mockito.junit.jupiter.MockitoExtension;

import com.sa.smart.dto.BlocoDTO;
import com.sa.smart.enums.EnumCorBloco;
<<<<<<< Updated upstream
import com.sa.smart.enums.EnumTipoPedido;
=======
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
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
=======
    /**
     *
     */
    @InjectMocks
    private BlocoService blocoService;

    @Test
    void deveCriarBloco() {

        Pedido pedido = new Pedido();
        pedido.setIdPedido(1L);

        Estoque estoque = new Estoque();
        estoque.setId(1L);
        estoque.setCor(1);

        Bloco bloco = new Bloco();
        bloco.setIdBloco(1L);
        bloco.setPedido(pedido);
        bloco.setEstoque(estoque);
        bloco.setCorBloco(1);

        BlocoDTO dto = new BlocoDTO(
>>>>>>> Stashed changes
                null,
                EnumCorBloco.PRETO,
                null,
                1L,
                1L
        );
<<<<<<< Updated upstream
    }

    @Test
    void deveCriarBloco() {
=======
>>>>>>> Stashed changes

        when(pedidoRepository.findById(1L))
                .thenReturn(Optional.of(pedido));

        when(estoqueRepository.findById(1L))
                .thenReturn(Optional.of(estoque));

        when(blocoRepository.save(any(Bloco.class)))
                .thenReturn(bloco);

        BlocoDTO resultado = blocoService.criar(dto);

<<<<<<< Updated upstream
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
=======
        assertNotNull(resultado);
    }

    
    @Test
void deveLancarErroQuandoCorForNula() {

    BlocoDTO dto = new BlocoDTO(
            null,
            null,
            null,
            1L,
            1L
    );

    RuntimeException ex = assertThrows(
            RuntimeException.class,
            () -> blocoService.criar(dto)
    );

    assertEquals(
            "Cor do bloco inválida. Use: 1-Preto, 2-Vermelho, 3-Azul",
            ex.getMessage()
    );
}

@Test
void deveLancarErroQuandoPedidoNaoExistir() {

    when(pedidoRepository.findById(1L))
            .thenReturn(Optional.empty());

    BlocoDTO dto = new BlocoDTO(
            null,
            EnumCorBloco.PRETO,
            null,
            1L,
            1L
    );

    RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> blocoService.criar(dto)
    );

    assertTrue(exception.getMessage()
            .contains("Pedido não encontrado"));
}

@Test
void deveBuscarBlocoPorId() {

    Pedido pedido = new Pedido();
    pedido.setIdPedido(1L);

    Estoque estoque = new Estoque();
    estoque.setId(1L);

    Bloco bloco = new Bloco();
    bloco.setIdBloco(1L);
    bloco.setPedido(pedido);
    bloco.setEstoque(estoque);
    bloco.setCorBloco(1);

    when(blocoRepository.findById(1L))
            .thenReturn(Optional.of(bloco));

    BlocoDTO resultado = blocoService.buscar(1L);

    assertNotNull(resultado);
    assertEquals(1L, resultado.id());
}
@Test
void deveDeletarBloco() {

    Bloco bloco = new Bloco();
    bloco.setIdBloco(1L);

    when(blocoRepository.findById(1L))
            .thenReturn(Optional.of(bloco));

    blocoService.deletar(1L);

    verify(blocoRepository).delete(bloco);
}

@Test
void deveLancarErroQuandoEstoqueNaoExistir() {

    Pedido pedido = new Pedido();
    pedido.setIdPedido(1L);

    when(pedidoRepository.findById(1L))
            .thenReturn(Optional.of(pedido));

    when(estoqueRepository.findById(1L))
            .thenReturn(Optional.empty());

    BlocoDTO dto = new BlocoDTO(
            null,
            EnumCorBloco.PRETO,
            null,
            1L,
            1L
    );

    assertThrows(
            RuntimeException.class,
            () -> blocoService.criar(dto)
    );
}
@Test

void deveLancarErroQuandoEstoqueEstiverVazio() {

    Pedido pedido = new Pedido();
    pedido.setIdPedido(1L);

    Estoque estoque = new Estoque();
    estoque.setId(1L);
    estoque.setCor(0);

    when(pedidoRepository.findById(1L))
            .thenReturn(Optional.of(pedido));

    when(estoqueRepository.findById(1L))
            .thenReturn(Optional.of(estoque));

    BlocoDTO dto = new BlocoDTO(
            null,
            EnumCorBloco.PRETO,
            null,
            1L,
            1L
    );

    assertThrows(
            RuntimeException.class,
            () -> blocoService.criar(dto)
    );
}

@Test
void deveLancarErroAoBuscarBlocoInexistente() {

    when(blocoRepository.findById(1L))
            .thenReturn(Optional.empty());

    assertThrows(
            RuntimeException.class,
            () -> blocoService.buscar(1L)
    );
}

@Test
void deveListarBlocos() {

    Bloco bloco = new Bloco();
    bloco.setIdBloco(1L);
    bloco.setCorBloco(1);

    when(blocoRepository.findAll())
            .thenReturn(List.of(bloco));

    var resultado = blocoService.listar();

    assertEquals(1, resultado.size());
}
>>>>>>> Stashed changes
}