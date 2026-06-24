// package com.sa.smart.controller;

// import java.nio.ByteBuffer;
// import java.nio.ByteOrder;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.Executors;
// import java.util.concurrent.ScheduledExecutorService;
// import java.util.concurrent.ScheduledFuture;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpEntity;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.util.LinkedMultiValueMap;
// import org.springframework.util.MultiValueMap;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.client.RestTemplate;

// import com.sa.smart.config.ApiUrlConfig;
// import com.sa.smart.dto.BlocoDTO;
// import com.sa.smart.dto.LaminaDTO;
// import com.sa.smart.dto.PedidoConfigDTO;
// import com.sa.smart.model.Estoque;
// import com.sa.smart.model.Expedicao;
// import com.sa.smart.repository.EstoqueRepository;
// import com.sa.smart.repository.ExpedicaoRepository;
// import com.sa.smart.service.SmartService;

// @RestController
// public class SmartController {

//     private final Map<String, String> leiturasCache = new ConcurrentHashMap<>();
//     private final ScheduledExecutorService leituraExecutor = Executors.newScheduledThreadPool(4);
//     private final Map<String, ScheduledFuture<?>> leituraFutures = new ConcurrentHashMap<>();

//     @Autowired
//     private SmartService smartService;

//     @Autowired
//     private EstoqueRepository estoqueRepository;

//     @Autowired
//     private ExpedicaoRepository expedicaoRepository;

//     @Autowired
//     private ApiUrlConfig apiUrlConfig;

//     // ========================== ENDPOINTS ==========================

//     @PostMapping("/iniciar-pedido")
//     public ResponseEntity<String> startOrder(@RequestBody PedidoConfigDTO pedidoConfigDTO) {
//         Long idPedido = pedidoConfigDTO.getId();
//         int tipo = pedidoConfigDTO.getTipoPedido();
//         int tampa = pedidoConfigDTO.getCorTampa();
//         String ipClp = pedidoConfigDTO.getIpClp();
//         List<BlocoDTO> pedido = pedidoConfigDTO.getBlocos();

//         System.out.println("Iniciando pedido ID: " + idPedido);
//         System.out.println("IP do CLP: " + ipClp);
//         System.out.println("Tipo: " + tipo);
//         System.out.println("Cor da tampa: " + (tampa == 1 ? "Preto" : tampa == 2 ? "Vermelho" : "Azul"));

//         // Log dos blocos e lâminas (acesso direto por ser record)
//         for (BlocoDTO bloco : pedido) {
//             // bloco.andar() e bloco.laminas() agora existem
//             System.out.println("Andar: " + bloco.andar() + ", Cor do Bloco: " + bloco.corBloco().getCodigo());
//             int i = 1;
//             for (LaminaDTO lamina : bloco.laminas()) {
//                 System.out.println("  Lâmina-" + i + ": Cor=" + lamina.cor() + ", Padrão=" + lamina.padrao());
//                 i++;
//             }
//         }

//         try {
//             byte[] bytePedidoArray = assemblerOrderToClp(pedido, idPedido);

//             System.out.print("Bytes do pedido: ");
//             for (byte b : bytePedidoArray) System.out.printf("%02X ", b);
//             System.out.println();

//             // 1. Envia o bloco de bytes para o CLP
//             boolean envioClpOk = smartService.sendBlockBytesToClp(
//                     ipClp, 9, 2, bytePedidoArray, bytePedidoArray.length);

//             System.out.println("Seletor de Tampas Presente: " + apiUrlConfig.getSeletorTampasPresent());

//             if (!envioClpOk) {
//                 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                         .body("Erro: falha ao enviar bloco de bytes ao CLP.");
//             }

//             // 2. Se houver seletor de tampas, aciona a posição desejada
//             if (apiUrlConfig.getSeletorTampasPresent()) {
//                 try {
//                     RestTemplate apiSeletorTampa = new RestTemplate();
//                     String url = "http://10.74.241.245/api/move_pos";

//                     HttpHeaders headers = new HttpHeaders();
//                     headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

//                     MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
//                     map.add("pos", String.valueOf(tampa));
//                     map.add("offset", "0");

//                     HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

//                     ResponseEntity<String> rawResponse = apiSeletorTampa.postForEntity(url, request, String.class);
//                     System.out.println("Resposta Bruta do ESP32: " + rawResponse.getBody());

//                     ResponseEntity<Map> response = apiSeletorTampa.postForEntity(url, request, Map.class);
//                     Map<String, Object> body = response.getBody();

//                     if (body == null || body.get("status") == null) {
//                         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                                 .body("Erro: seletor de tampas retornou corpo vazio. Conteúdo: " + rawResponse.getBody());
//                     }

//                     String status = body.get("status").toString();
//                     if (!status.toLowerCase().contains("ok")) {
//                         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                                 .body("Erro: seletor de tampas não confirmou 'Ok'. Resposta: " + status);
//                     }

//                 } catch (Exception e) {
//                     e.printStackTrace();
//                     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                             .body("Erro ao comunicar com o seletor de tampas: " + e.getMessage());
//                 }
//             }

//             // 3. Inicia a execução do pedido no CLP
//             smartService.startExecuteOrder(ipClp);

//             return ResponseEntity.ok("Pedido enviado e iniciado no CLP com sucesso.");

//         } catch (Exception e) {
//             e.printStackTrace();
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                     .body("Erro ao processar pedido: " + e.getMessage());
//         }
//     }

//     // ... (os demais endpoints permanecem idênticos ao que você já tem) ...

//     // ========================== MÉTODO AUXILIAR ==========================

//     private byte[] assemblerOrderToClp(List<BlocoDTO> pedido, Long idPedido) {
//         int[] dados = new int[30];
//         Set<Integer> posicoesUsadas = new HashSet<>();
//         int andares = pedido.size();

//         for (BlocoDTO bloco : pedido) {
//             int indexBase = (bloco.andar() - 1) * 9;   // acesso direto

//             if (indexBase + 8 >= dados.length) {
//                 System.out.println("Ignorando andar fora do esperado: " + bloco.andar());
//                 continue;
//             }

//             // Obtém o código da cor do bloco (supondo que EnumCorBloco tenha getCodigo())
//             int corBloco = bloco.corBloco().getCodigo();
//             int posicaoEstoque = smartService.SearchFirstPositionByColor(corBloco, posicoesUsadas);
//             if (posicaoEstoque != -1) {
//                 posicoesUsadas.add(posicaoEstoque);
//             }

//             dados[indexBase] = corBloco;
//             dados[indexBase + 1] = posicaoEstoque;

//             List<LaminaDTO> laminas = bloco.laminas(); // acesso direto
//             for (int i = 0; i < Math.min(3, laminas.size()); i++) {
//                 dados[indexBase + 2 + i] = laminas.get(i).cor();    // acesso direto
//                 dados[indexBase + 5 + i] = laminas.get(i).padrao(); // acesso direto
//             }

//             dados[indexBase + 8] = 0;
//         }

//         dados[27] = idPedido != null ? idPedido.intValue() : 0;
//         dados[28] = andares;
//         dados[29] = smartService.searchFirstPositionFreeExp();

//         // Log para depuração
//         System.out.println("--- Dados montados para o CLP ---");
//         for (int andar = 1; andar <= 3; andar++) {
//             int base = (andar - 1) * 9;
//             System.out.printf("Andar %d: cor=%d, posEst=%d, lam1cor=%d, lam2cor=%d, lam3cor=%d, pad1=%d, pad2=%d, pad3=%d, proc=%d%n",
//                     andar,
//                     dados[base], dados[base+1], dados[base+2], dados[base+3], dados[base+4],
//                     dados[base+5], dados[base+6], dados[base+7], dados[base+8]);
//         }
//         System.out.printf("Nº Pedido: %d, Andares: %d, PosExpedição: %d%n",
//                 dados[27], dados[28], dados[29]);

//         ByteBuffer buffer = ByteBuffer.allocate(60).order(ByteOrder.BIG_ENDIAN);
//         for (int valor : dados) {
//             buffer.putShort((short) valor);
//         }
//         return buffer.array();
//     }
// }

package com.sa.smart.controller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sa.smart.clpcomm.PlcConnectionService;
import com.sa.smart.clpcomm.PlcConnector;
import com.sa.smart.clpcomm.PlcReaderDB;
import com.sa.smart.clpcomm.PlcReaderMultDB;
import com.sa.smart.clpcomm.PlcReaderMultDB.PlcReadRequest;

/**
 * Controlador principal para leitura dos CLPs da bancada Smart 4.0.
 *
 * Endpoints:
 *   POST /start-readings  → inicia o agendamento de leituras com os IPs enviados
 *   POST /stop-readings   → para o agendamento e fecha conexões
 *   GET  /data/clp1       → retorna os bytes lidos do CLP de Estoque (hex string)
 *   GET  /data/clp2       → retorna os bytes lidos do CLP de Processo (hex string)
 *   GET  /data/clp3       → retorna os bytes lidos do CLP de Montagem (hex string)
 *   GET  /data/clp4       → retorna os bytes lidos do CLP de Expedição (hex string)
 *   GET  /status          → retorna um JSON com o estado interpretado de cada estação
 */
@RestController
@CrossOrigin(origins = "*")
public class SmartController {

    // -------------------------------------------------------------------------
    // Dependências
    // -------------------------------------------------------------------------
    @Autowired
    private PlcConnectionService plcConnectionService;

    // -------------------------------------------------------------------------
    // Cache de bytes lidos (chave = nome do CLP: clp1..clp4)
    // -------------------------------------------------------------------------
    private final Map<String, byte[]> dadosClp = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Scheduler
    // -------------------------------------------------------------------------
    private ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> tarefas = new ConcurrentHashMap<>();

    // Período de leitura em milissegundos
    private static final long PERIODO_MS = 500;

    // =========================================================================
    // POST /start-readings
    // Corpo esperado (JSON): { "estoque": "10.74.241.10", "processo": "...", ... }
    // =========================================================================
    @PostMapping("/start-readings")
    public ResponseEntity<String> startReadings(@RequestBody Map<String, String> ips) {

        // Para qualquer leitura em andamento
        pararTudo();

        scheduler = Executors.newScheduledThreadPool(4);

        // --- CLP 1 – Estoque ---------------------------------------------------
        String ipEstoque = ips.getOrDefault("estoque", "10.74.241.10");
        agendarLeitura("clp1", ipEstoque, () -> {
            PlcConnector c = plcConnectionService.getConnection(ipEstoque);
            if (c == null) return;
            // DB principal do Estoque: DB1, offset 0, 120 bytes (ajuste conforme seu projeto)
            new PlcReaderDB(c, "Estoque", 1, 0, 120, bytes -> dadosClp.put("clp1", bytes)).run();
        });

        // --- CLP 2 – Processo --------------------------------------------------
        String ipProcesso = ips.getOrDefault("processo", "10.74.241.20");
        agendarLeitura("clp2", ipProcesso, () -> {
            PlcConnector c = plcConnectionService.getConnection(ipProcesso);
            if (c == null) return;
            // DB do Processo: DB2, offset 0, 20 bytes
            new PlcReaderDB(c, "Processo", 2, 0, 20, bytes -> dadosClp.put("clp2", bytes)).run();
        });

        // --- CLP 3 – Montagem --------------------------------------------------
        String ipMontagem = ips.getOrDefault("montagem", "10.74.241.30");
        agendarLeitura("clp3", ipMontagem, () -> {
            PlcConnector c = plcConnectionService.getConnection(ipMontagem);
            if (c == null) return;
            // DB da Montagem: DB3, offset 0, 20 bytes
            new PlcReaderDB(c, "Montagem", 3, 0, 20, bytes -> dadosClp.put("clp3", bytes)).run();
        });

        // --- CLP 4 – Expedição -------------------------------------------------
        String ipExpedicao = ips.getOrDefault("expedicao", "10.74.241.40");
        agendarLeitura("clp4", ipExpedicao, () -> {
            PlcConnector c = plcConnectionService.getConnection(ipExpedicao);
            if (c == null) return;
            // Expedição lê dois DBs e os concatena (MultDB)
            // DB4 (status/OP): offset 0, 40 bytes  |  DB5 (posição): offset 0, 10 bytes
            new PlcReaderMultDB(c, "Expedição",
                    new PlcReadRequest(4, 0, 40),
                    new PlcReadRequest(5, 0, 10),
                    null, null, null,
                    bytes -> dadosClp.put("clp4", bytes)).run();
        });

        System.out.println("[SmartController] Leituras iniciadas. IPs: " + ips);
        return ResponseEntity.ok("Leituras iniciadas");
    }

    // =========================================================================
    // POST /stop-readings
    // =========================================================================
    @PostMapping("/stop-readings")
    public ResponseEntity<String> stopReadings() {
        pararTudo();
        System.out.println("[SmartController] Leituras encerradas.");
        return ResponseEntity.ok("Leituras encerradas");
    }

    // =========================================================================
    // GET /data/clp{n}  →  hex string dos bytes lidos (ex: "00 1A FF 02 ...")
    // =========================================================================
    @GetMapping("/data/clp1")
    public ResponseEntity<String> getDadosClp1() {
        return responderHex("clp1");
    }

    @GetMapping("/data/clp2")
    public ResponseEntity<String> getDadosClp2() {
        return responderHex("clp2");
    }

    @GetMapping("/data/clp3")
    public ResponseEntity<String> getDadosClp3() {
        return responderHex("clp3");
    }

    @GetMapping("/data/clp4")
    public ResponseEntity<String> getDadosClp4() {
        return responderHex("clp4");
    }

    // =========================================================================
    // GET /status  →  JSON com o estado interpretado de cada estação
    // Útil para debug ou para um front mais simples sem parsing de bytes.
    // =========================================================================
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> resp = new HashMap<>();

        // Estoque
        byte[] estoque = dadosClp.get("clp1");
        if (estoque != null && estoque.length >= 100) {
            int len = estoque.length;
            Map<String, Object> e = new HashMap<>();
            e.put("statusEstoque",   estoque[len - 6] & 0xFF);
            e.put("statusProcesso",  estoque[len - 5] & 0xFF);
            e.put("statusMontagem",  estoque[len - 4] & 0xFF);
            e.put("statusExpedicao", estoque[len - 3] & 0xFF);
            e.put("pedidoEmCurso",   (estoque[len - 1] & 0xFF) == 1);
            e.put("op",   getInt16BE(estoque, 96));
            e.put("pos",  getInt16BE(estoque, 66));
            resp.put("estoque", e);
        }

        // Processo
        byte[] processo = dadosClp.get("clp2");
        if (processo != null && processo.length >= 6) {
            Map<String, Object> p = new HashMap<>();
            boolean start  = (processo[4] & 0x04) != 0;
            boolean finish = (processo[4] & 0x02) != 0;
            p.put("status", start ? 1 : finish ? 2 : 0);
            p.put("op", getInt16BE(processo, 2));
            resp.put("processo", p);
        }

        // Montagem
        byte[] montagem = dadosClp.get("clp3");
        if (montagem != null && montagem.length >= 6) {
            Map<String, Object> m = new HashMap<>();
            boolean start  = (montagem[4] & 0x04) != 0;
            boolean finish = (montagem[4] & 0x02) != 0;
            m.put("status", start ? 1 : finish ? 2 : 0);
            m.put("op", getInt16BE(montagem, 2));
            resp.put("montagem", m);
        }

        // Expedição
        byte[] expedicao = dadosClp.get("clp4");
        if (expedicao != null && expedicao.length >= 36) {
            Map<String, Object> x = new HashMap<>();
            boolean start  = (expedicao[32] & 0x04) != 0;
            boolean finish = (expedicao[32] & 0x02) != 0;
            x.put("status", start ? 1 : finish ? 2 : 0);
            x.put("op",  getInt16BE(expedicao, 30));
            x.put("pos", getInt16BE(expedicao, 4));
            resp.put("expedicao", x);
        }

        return ResponseEntity.ok(resp);
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /** Converte byte[] em hex string separada por espaços ("00 1A FF ..."). */
    private ResponseEntity<String> responderHex(String chave) {
        byte[] bytes = dadosClp.get(chave);
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.ok("Ainda não há dados");
        }
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return ResponseEntity.ok(sb.toString().trim());
    }

    /** Lê um int16 big-endian a partir de offset. */
    private int getInt16BE(byte[] bytes, int offset) {
        if (bytes == null || offset + 1 >= bytes.length) return 0;
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    /** Agenda uma Runnable periodicamente no scheduler. */
    private void agendarLeitura(String chave, String ip, Runnable tarefa) {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                tarefa, 0, PERIODO_MS, TimeUnit.MILLISECONDS);
        tarefas.put(chave, future);
    }

    /** Para o scheduler e fecha todas as conexões. */
    private synchronized void pararTudo() {
        tarefas.values().forEach(f -> f.cancel(true));
        tarefas.clear();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        plcConnectionService.closeAll();
        dadosClp.clear();
    }
}