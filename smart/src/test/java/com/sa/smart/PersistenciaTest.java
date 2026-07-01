package com.sa.smart;

import com.sa.smart.model.Pedido;
import com.sa.smart.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PersistenciaTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Test
    void dadosDevemPersistirAposReinicio() {
        // Cria e salva um pedido
        Pedido p = new Pedido();
        p.setOrdemProducao("OP-PERSIST");
        p.setStatusPedido(1);
        p.setTipoPedido(1);
        p.setCorTampa(1);
        pedidoRepository.save(p);

        List<Pedido> todos = pedidoRepository.findAll();
        assertThat(todos).hasSize(1);
        assertThat(todos.get(0).getOrdemProducao()).isEqualTo("OP-PERSIST");

    }

}