package com.sa.smart;


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
import org.mockito.junit.jupiter.MockitoExtension;


import com.sa.smart.dto.ExpedicaoDTO;
import com.sa.smart.model.Expedicao;
import com.sa.smart.model.Pedido;
import com.sa.smart.repository.ExpedicaoRepository;
import com.sa.smart.repository.PedidoRepository;
import com.sa.smart.service.ExpedicaoService;


@ExtendWith(MockitoExtension.class)
class ExpedicaoServiceTest {


    @Mock
    private ExpedicaoRepository expedicaoRepository;


    @Mock
    private PedidoRepository pedidoRepository;


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
