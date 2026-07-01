package com.sa.smart;

import com.sa.smart.model.Bloco;
import com.sa.smart.model.Estoque;
import com.sa.smart.model.Pedido;
import com.sa.smart.repository.EstoqueRepository;
import com.sa.smart.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PedidoIntegrationTest {
    // @Transactional foi removido propositalmente: a chamada via TestRestTemplate
    // sobe um servidor HTTP real e atende a requisição em outra thread/conexão.
    // Se a classe estivesse @Transactional, os dados inseridos no setUp() (dentro
    // da transação do teste, não commitados) ficariam invisíveis para o
    // controller/service que processa a requisição HTTP. Por isso a limpeza e o
    // setup do estoque abaixo dependem do deleteAll()/saveAll() do
    // PedidoRepository/EstoqueRepository, que já commitam por conta própria.

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @BeforeEach
    void setUp() {
        // Instancia manualmente para evitar problemas de bean
        restTemplate = new TestRestTemplate();

        // Limpa dados
        pedidoRepository.deleteAll();
        estoqueRepository.deleteAll();

        // Cria posições de estoque para teste
        Estoque e1 = new Estoque(null, 1, 1);
        Estoque e2 = new Estoque(null, 2, 2);
        Estoque e3 = new Estoque(null, 3, 3);
        estoqueRepository.saveAll(List.of(e1, e2, e3));
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void deveCriarPedidoDuploEAlterarEstoque() {
        Pedido pedido = new Pedido();
        pedido.setOrdemProducao("OP-INT-001");
        pedido.setTipoPedido(2);
        pedido.setCorTampa(1);
        pedido.setStatusPedido(1);

        Bloco b1 = new Bloco();
        b1.setCorBloco(1);
        Bloco b2 = new Bloco();
        b2.setCorBloco(2);
        pedido.setBlocos(List.of(b1, b2));

        ResponseEntity<Pedido> response = restTemplate.postForEntity(
                baseUrl() + "/pedidos", pedido, Pedido.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Pedido criado = response.getBody();
        assertThat(criado).isNotNull();
        assertThat(criado.getIdPedido()).isNotNull();

        // Verifica estoque
        List<Estoque> estoques = estoqueRepository.findAll();
        Estoque pos1 = estoques.stream().filter(e -> e.getPosicaoEstoque() == 1).findFirst().orElseThrow();
        Estoque pos2 = estoques.stream().filter(e -> e.getPosicaoEstoque() == 2).findFirst().orElseThrow();
        assertThat(pos1.getCor()).isEqualTo(0);
        assertThat(pos2.getCor()).isEqualTo(0);

        // Inicia produção
        ResponseEntity<String> iniciar = restTemplate.postForEntity(
                baseUrl() + "/pedidos/" + criado.getIdPedido() + "/iniciar?ip=10.74.241.10",
                null, String.class);
        assertThat(iniciar.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verifica status
        Pedido atualizado = restTemplate.getForObject(
                baseUrl() + "/pedidos/" + criado.getIdPedido(), Pedido.class);
        assertThat(atualizado).isNotNull();
        assertThat(atualizado.getStatusPedido()).isEqualTo(2);
    }

    @Test
    void deveAtualizarStatusParaEmProducao() {
        Pedido pedido = new Pedido();
        pedido.setOrdemProducao("OP-INT-002");
        pedido.setTipoPedido(1);
        pedido.setCorTampa(1);
        pedido.setStatusPedido(1);
        Bloco bloco = new Bloco();
        bloco.setCorBloco(1);
        pedido.setBlocos(List.of(bloco));

        ResponseEntity<Pedido> postResponse = restTemplate.postForEntity(
                baseUrl() + "/pedidos", pedido, Pedido.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long id = postResponse.getBody().getIdPedido();

        restTemplate.postForEntity(
                baseUrl() + "/pedidos/" + id + "/iniciar?ip=10.74.241.10",
                null, String.class);

        Pedido atualizado = restTemplate.getForObject(
                baseUrl() + "/pedidos/" + id, Pedido.class);
        assertThat(atualizado.getStatusPedido()).isEqualTo(2);
    }

    @Test
    void deveLancarErroAoCriarPedidoTriploComDoisBlocos() {
        Pedido pedido = new Pedido();
        pedido.setOrdemProducao("OP-INT-003");
        pedido.setTipoPedido(3);
        pedido.setCorTampa(1);
        pedido.setStatusPedido(1);
        pedido.setBlocos(List.of(new Bloco(), new Bloco()));

        ResponseEntity<Pedido> response = restTemplate.postForEntity(
                baseUrl() + "/pedidos", pedido, Pedido.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}