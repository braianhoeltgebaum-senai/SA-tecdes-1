package com.sa.smart;

<<<<<<< Updated upstream
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
>>>>>>> Stashed changes
import org.mockito.junit.jupiter.MockitoExtension;

import com.sa.smart.dto.ExpedicaoDTO;
import com.sa.smart.model.Expedicao;
import com.sa.smart.model.Pedido;
import com.sa.smart.repository.ExpedicaoRepository;
import com.sa.smart.repository.PedidoRepository;
import com.sa.smart.service.ExpedicaoService;

<<<<<<< Updated upstream
=======
import jakarta.persistence.EntityNotFoundException;

>>>>>>> Stashed changes
@ExtendWith(MockitoExtension.class)
class ExpedicaoServiceTest {

    @Mock
    private ExpedicaoRepository expedicaoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

<<<<<<< Updated upstream
    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Pedido> typedQuery;

    @InjectMocks
    private ExpedicaoService expedicaoService;

    private Expedicao expedicao;
    private Pedido pedido;
    private ExpedicaoDTO dto;

    @BeforeEach
    void setup() {

        pedido = new Pedido();
        pedido.setIdPedido(1L);
        pedido.setOrdemProducao("OP001");

        expedicao = new Expedicao();
        expedicao.setId(1L);
        expedicao.setPosicaoExpedicao(1);
        expedicao.setEntradaEm(LocalDateTime.now());
        expedicao.setSaidaEm(null);
        expedicao.setPedido(pedido);

        dto = new ExpedicaoDTO(
                null,
                1,
                LocalDateTime.now(),
                null,
                "OP001"
        );
    }

    private void mockBuscarPedido() {

        when(entityManager.createQuery(anyString(), eq(Pedido.class)))
                .thenReturn(typedQuery);

        when(typedQuery.setParameter(eq("ordemProducao"), any()))
                .thenReturn(typedQuery);

        when(typedQuery.getResultList())
                .thenReturn(List.of(pedido));
    }

    @Test
    void deveCriarExpedicao() {

        mockBuscarPedido();

        when(expedicaoRepository.save(any(Expedicao.class)))
                .thenReturn(expedicao);

        ExpedicaoDTO resultado = expedicaoService.criar(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.ordemProducao())
                .isEqualTo("OP001");

        verify(expedicaoRepository)
                .save(any(Expedicao.class));
    }

    @Test
    void deveListarExpedicoes() {

        when(expedicaoRepository.findAll())
                .thenReturn(List.of(expedicao));

        List<ExpedicaoDTO> lista = expedicaoService.listar();

        assertThat(lista).hasSize(1);
    }

    @Test
    void deveBuscarExpedicaoPorId() {

        when(expedicaoRepository.findById(1L))
                .thenReturn(Optional.of(expedicao));

        ExpedicaoDTO resultado = expedicaoService.buscar(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
    }

    @Test
    void deveAtualizarExpedicaoComPut() {

        mockBuscarPedido();

        ExpedicaoDTO atualizado = new ExpedicaoDTO(
                null,
                5,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "OP001"
        );

        when(expedicaoRepository.findById(1L))
                .thenReturn(Optional.of(expedicao));

        when(expedicaoRepository.save(any(Expedicao.class)))
                .thenReturn(expedicao);

        ExpedicaoDTO resultado =
                expedicaoService.put(1L, atualizado);

        assertThat(resultado).isNotNull();

        verify(expedicaoRepository)
                .save(any(Expedicao.class));
    }

    @Test
    void deveAtualizarExpedicaoComPatch() {

        ExpedicaoDTO patch = new ExpedicaoDTO(
                null,
                null,
                null,
                LocalDateTime.now(),
                null
        );

        when(expedicaoRepository.findById(1L))
                .thenReturn(Optional.of(expedicao));

        when(expedicaoRepository.save(any(Expedicao.class)))
                .thenReturn(expedicao);

        ExpedicaoDTO resultado =
                expedicaoService.patch(1L, patch);

        assertThat(resultado).isNotNull();

        verify(expedicaoRepository)
                .save(any(Expedicao.class));
    }

    @Test
    void deveDeletarExpedicao() {

        when(expedicaoRepository.existsById(1L))
                .thenReturn(true);

        doNothing().when(expedicaoRepository)
                .deleteById(1L);

        expedicaoService.deletar(1L);

        verify(expedicaoRepository)
                .deleteById(1L);
    }

    @Test
    void deveLancarErroQuandoExpedicaoNaoExisteAoBuscar() {

        when(expedicaoRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                expedicaoService.buscar(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Expedição não encontrada");
    }

    @Test
    void deveLancarErroQuandoExpedicaoNaoExisteAoDeletar() {

        when(expedicaoRepository.existsById(1L))
                .thenReturn(false);

        assertThatThrownBy(() ->
                expedicaoService.deletar(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Expedição não encontrada");
    }

    @Test
    void deveLancarErroQuandoPedidoNaoExiste() {

        when(entityManager.createQuery(anyString(), eq(Pedido.class)))
                .thenReturn(typedQuery);

        when(typedQuery.setParameter(eq("ordemProducao"), any()))
                .thenReturn(typedQuery);

        when(typedQuery.getResultList())
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                expedicaoService.criar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pedido não encontrado");
    }
}
=======
    @InjectMocks
    private ExpedicaoService expedicaoService;

    @Test
void deveListarExpedicoes() {

    Pedido pedido = new Pedido();
    pedido.setOrdemProducao("OP001");

    Expedicao expedicao = new Expedicao();
    expedicao.setId(1L);
    expedicao.setPedido(pedido);

    when(expedicaoRepository.findAll())
            .thenReturn(List.of(expedicao));

    List<ExpedicaoDTO> resultado =
            expedicaoService.listar();

    assertEquals(1, resultado.size());
}

@Test
void deveBuscarExpedicaoPorId() {

    Pedido pedido = new Pedido();
    pedido.setOrdemProducao("OP001");

    Expedicao expedicao = new Expedicao();
    expedicao.setId(1L);
    expedicao.setPedido(pedido);

    when(expedicaoRepository.findById(1L))
            .thenReturn(Optional.of(expedicao));

    ExpedicaoDTO resultado =
            expedicaoService.buscar(1L);

    assertNotNull(resultado);
    assertEquals(1L, resultado.id());
}

@Test
void deveLancarErroAoBuscarExpedicaoInexistente() {

    when(expedicaoRepository.findById(1L))
            .thenReturn(Optional.empty());

    assertThrows(
            RuntimeException.class,
            () -> expedicaoService.buscar(1L)
    );
}

@Test
void deveDeletarExpedicao() {

    when(expedicaoRepository.existsById(1L))
            .thenReturn(true);

    expedicaoService.deletar(1L);

    verify(expedicaoRepository)
            .deleteById(1L);
}

@Test
void deveLancarErroAoDeletarExpedicaoInexistente() {

    when(expedicaoRepository.existsById(1L))
            .thenReturn(false);

    assertThrows(
            EntityNotFoundException.class,
            () -> expedicaoService.deletar(1L)
    );
}
}
>>>>>>> Stashed changes
