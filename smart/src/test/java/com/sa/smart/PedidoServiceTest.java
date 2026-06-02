package com.sa.smart;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import com.sa.smart.model.Bloco;
import com.sa.smart.model.Pedido;
import com.sa.smart.repository.PedidoRepository;
import com.sa.smart.service.PedidoService;


@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {


    @Mock
    private PedidoRepository pedidoRepository;


    @InjectMocks
    private PedidoService pedidoService;


    private Pedido pedido;


    @BeforeEach
    void setup() {


        pedido = new Pedido();


        pedido.setIdPedido(1L);
        pedido.setOrdemProducao("OP001");
        pedido.setTipoPedido(1);
        pedido.setStatusPedido(1);
    }


    @Test
    void deveCriarPedido() {


        when(pedidoRepository.save(any(Pedido.class)))
                .thenReturn(pedido);


        Pedido resultado =
                pedidoService.criarPedido(pedido);


        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatusPedido())
                .isEqualTo(1);


        verify(pedidoRepository)
                .save(any(Pedido.class));
    }


    @Test
    void deveCriarPedidoTriploComTresBlocos() {


        Bloco b1 = new Bloco();
        Bloco b2 = new Bloco();
        Bloco b3 = new Bloco();


        pedido.setTipoPedido(3);
        pedido.setBlocos(List.of(b1, b2, b3));


        when(pedidoRepository.save(any(Pedido.class)))
                .thenReturn(pedido);


        Pedido resultado =
                pedidoService.criarPedido(pedido);


        assertThat(resultado).isNotNull();


        verify(pedidoRepository)
                .save(any(Pedido.class));
    }


    @Test
    void deveLancarErroQuandoPedidoTriploNaoTemTresBlocos() {


        Bloco b1 = new Bloco();


        pedido.setTipoPedido(3);
        pedido.setBlocos(List.of(b1));


        assertThatThrownBy(() ->
                pedidoService.criarPedido(pedido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(
                        "Pedidos triplos exigem exatamente 3 blocos");
    }


    @Test
    void deveListarPedidos() {


        when(pedidoRepository.findAll())
                .thenReturn(List.of(pedido));


        List<Pedido> lista =
                pedidoService.listarTodos();


        assertThat(lista).hasSize(1);
    }


    @Test
    void deveAtualizarStatusParaConcluido() {


        when(pedidoRepository.findById(1L))
                .thenReturn(java.util.Optional.of(pedido));


        when(pedidoRepository.save(any(Pedido.class)))
                .thenReturn(pedido);


        pedidoService.atualizarStatusParaConcluido(1L);


        assertThat(pedido.getStatusPedido())
                .isEqualTo(3);


        verify(pedidoRepository)
                .save(any(Pedido.class));
    }


    @Test
    void deveAtualizarParcialmentePedido() {


        when(pedidoRepository.findById(1L))
                .thenReturn(java.util.Optional.of(pedido));


        when(pedidoRepository.save(any(Pedido.class)))
                .thenReturn(pedido);


        Map<String, Object> campos = Map.of(
                "ordemProducao", "OP999"
        );


        Pedido resultado =
                pedidoService.atualizarParcial(1L, campos);


        assertThat(resultado.getOrdemProducao())
                .isEqualTo("OP999");


        verify(pedidoRepository)
                .save(any(Pedido.class));
    }


    @Test
    void deveExcluirPedidoPendente() {


        pedido.setStatusPedido(1);


        when(pedidoRepository.findById(1L))
                .thenReturn(java.util.Optional.of(pedido));


        doNothing().when(pedidoRepository)
                .delete(any(Pedido.class));


        pedidoService.excluir(1L);


        verify(pedidoRepository)
                .delete(any(Pedido.class));
    }


    @Test
    void deveLancarErroAoExcluirPedidoNaoPendente() {


        pedido.setStatusPedido(2);


        when(pedidoRepository.findById(1L))
                .thenReturn(java.util.Optional.of(pedido));


        assertThatThrownBy(() ->
                pedidoService.excluir(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(
                        "Apenas pedidos Pendentes podem ser excluídos");
    }


    @Test
    void deveLancarErroQuandoPedidoNaoExisteAoAtualizarStatus() {


        when(pedidoRepository.findById(1L))
                .thenReturn(java.util.Optional.empty());


        assertThatThrownBy(() ->
                pedidoService.atualizarStatusParaConcluido(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pedido não encontrado");
    }


    @Test
    void deveLancarErroQuandoPedidoNaoExisteAoExcluir() {


        when(pedidoRepository.findById(1L))
                .thenReturn(java.util.Optional.empty());


        assertThatThrownBy(() ->
                pedidoService.excluir(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pedido não encontrado");
    }
}
