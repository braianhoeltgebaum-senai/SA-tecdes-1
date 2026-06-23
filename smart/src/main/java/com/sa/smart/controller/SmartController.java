package com.sa.smart.controller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.sa.smart.config.ApiUrlConfig;
import com.sa.smart.dto.BlocoDTO;
import com.sa.smart.dto.LaminaDTO;
import com.sa.smart.dto.PedidoConfigDTO;
import com.sa.smart.model.Estoque;
import com.sa.smart.model.Expedicao;
import com.sa.smart.repository.EstoqueRepository;
import com.sa.smart.repository.ExpedicaoRepository;
import com.sa.smart.service.SmartService;

@RestController
public class SmartController {

    private final Map<String, String> leiturasCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService leituraExecutor = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> leituraFutures = new ConcurrentHashMap<>();

    @Autowired
    private SmartService smartService;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private ExpedicaoRepository expedicaoRepository;

    @Autowired
    private ApiUrlConfig apiUrlConfig;

    // ========================== ENDPOINTS ==========================

    @PostMapping("/iniciar-pedido")
    public ResponseEntity<String> startOrder(@RequestBody PedidoConfigDTO pedidoConfigDTO) {
        Long idPedido = pedidoConfigDTO.getId();
        int tipo = pedidoConfigDTO.getTipoPedido();
        int tampa = pedidoConfigDTO.getCorTampa();
        String ipClp = pedidoConfigDTO.getIpClp();
        List<BlocoDTO> pedido = pedidoConfigDTO.getBlocos();

        System.out.println("Iniciando pedido ID: " + idPedido);
        System.out.println("IP do CLP: " + ipClp);
        System.out.println("Tipo: " + tipo);
        System.out.println("Cor da tampa: " + (tampa == 1 ? "Preto" : tampa == 2 ? "Vermelho" : "Azul"));

        // Log dos blocos e lâminas (acesso direto por ser record)
        for (BlocoDTO bloco : pedido) {
            // bloco.andar() e bloco.laminas() agora existem
            System.out.println("Andar: " + bloco.andar() + ", Cor do Bloco: " + bloco.corBloco().getCodigo());
            int i = 1;
            for (LaminaDTO lamina : bloco.laminas()) {
                System.out.println("  Lâmina-" + i + ": Cor=" + lamina.cor() + ", Padrão=" + lamina.padrao());
                i++;
            }
        }

        try {
            byte[] bytePedidoArray = assemblerOrderToClp(pedido, idPedido);

            System.out.print("Bytes do pedido: ");
            for (byte b : bytePedidoArray) System.out.printf("%02X ", b);
            System.out.println();

            // 1. Envia o bloco de bytes para o CLP
            boolean envioClpOk = smartService.sendBlockBytesToClp(
                    ipClp, 9, 2, bytePedidoArray, bytePedidoArray.length);

            System.out.println("Seletor de Tampas Presente: " + apiUrlConfig.getSeletorTampasPresent());

            if (!envioClpOk) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Erro: falha ao enviar bloco de bytes ao CLP.");
            }

            // 2. Se houver seletor de tampas, aciona a posição desejada
            if (apiUrlConfig.getSeletorTampasPresent()) {
                try {
                    RestTemplate apiSeletorTampa = new RestTemplate();
                    String url = "http://10.74.241.245/api/move_pos";

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
                    map.add("pos", String.valueOf(tampa));
                    map.add("offset", "0");

                    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

                    ResponseEntity<String> rawResponse = apiSeletorTampa.postForEntity(url, request, String.class);
                    System.out.println("Resposta Bruta do ESP32: " + rawResponse.getBody());

                    ResponseEntity<Map> response = apiSeletorTampa.postForEntity(url, request, Map.class);
                    Map<String, Object> body = response.getBody();

                    if (body == null || body.get("status") == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Erro: seletor de tampas retornou corpo vazio. Conteúdo: " + rawResponse.getBody());
                    }

                    String status = body.get("status").toString();
                    if (!status.toLowerCase().contains("ok")) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Erro: seletor de tampas não confirmou 'Ok'. Resposta: " + status);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Erro ao comunicar com o seletor de tampas: " + e.getMessage());
                }
            }

            // 3. Inicia a execução do pedido no CLP
            smartService.startExecuteOrder(ipClp);

            return ResponseEntity.ok("Pedido enviado e iniciado no CLP com sucesso.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar pedido: " + e.getMessage());
        }
    }

    // ... (os demais endpoints permanecem idênticos ao que você já tem) ...

    // ========================== MÉTODO AUXILIAR ==========================

    private byte[] assemblerOrderToClp(List<BlocoDTO> pedido, Long idPedido) {
        int[] dados = new int[30];
        Set<Integer> posicoesUsadas = new HashSet<>();
        int andares = pedido.size();

        for (BlocoDTO bloco : pedido) {
            int indexBase = (bloco.andar() - 1) * 9;   // acesso direto

            if (indexBase + 8 >= dados.length) {
                System.out.println("Ignorando andar fora do esperado: " + bloco.andar());
                continue;
            }

            // Obtém o código da cor do bloco (supondo que EnumCorBloco tenha getCodigo())
            int corBloco = bloco.corBloco().getCodigo();
            int posicaoEstoque = smartService.SearchFirstPositionByColor(corBloco, posicoesUsadas);
            if (posicaoEstoque != -1) {
                posicoesUsadas.add(posicaoEstoque);
            }

            dados[indexBase] = corBloco;
            dados[indexBase + 1] = posicaoEstoque;

            List<LaminaDTO> laminas = bloco.laminas(); // acesso direto
            for (int i = 0; i < Math.min(3, laminas.size()); i++) {
                dados[indexBase + 2 + i] = laminas.get(i).cor();    // acesso direto
                dados[indexBase + 5 + i] = laminas.get(i).padrao(); // acesso direto
            }

            dados[indexBase + 8] = 0;
        }

        dados[27] = idPedido != null ? idPedido.intValue() : 0;
        dados[28] = andares;
        dados[29] = smartService.searchFirstPositionFreeExp();

        // Log para depuração
        System.out.println("--- Dados montados para o CLP ---");
        for (int andar = 1; andar <= 3; andar++) {
            int base = (andar - 1) * 9;
            System.out.printf("Andar %d: cor=%d, posEst=%d, lam1cor=%d, lam2cor=%d, lam3cor=%d, pad1=%d, pad2=%d, pad3=%d, proc=%d%n",
                    andar,
                    dados[base], dados[base+1], dados[base+2], dados[base+3], dados[base+4],
                    dados[base+5], dados[base+6], dados[base+7], dados[base+8]);
        }
        System.out.printf("Nº Pedido: %d, Andares: %d, PosExpedição: %d%n",
                dados[27], dados[28], dados[29]);

        ByteBuffer buffer = ByteBuffer.allocate(60).order(ByteOrder.BIG_ENDIAN);
        for (int valor : dados) {
            buffer.putShort((short) valor);
        }
        return buffer.array();
    }
}