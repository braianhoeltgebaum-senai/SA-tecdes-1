package com.sa.smart.controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sa.smart.clpcomm.PlcConnectionService;
import com.sa.smart.clpcomm.PlcConnector;
import com.sa.smart.clpcomm.PlcReaderDB;
import com.sa.smart.clpcomm.PlcReaderMultDB;
import com.sa.smart.service.EstoqueService;
import com.sa.smart.service.ExpedicaoService;
import com.sa.smart.service.MontagemService;
import com.sa.smart.service.ProcessoService;
import com.sa.smart.service.SmartService;

@RestController
public class ClpController {

    private final Map<String, String> readingsCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService readingExecutor = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> readingFutures = new ConcurrentHashMap<>();

    private static byte[] dataClp1;
    private static byte[] dataClp2;
    private static byte[] dataClp3;
    private static byte[] dataClp4;

    @Autowired
    private PlcConnectionService plcConnectionService;

    @Autowired
    private SmartService smartService;

    @Autowired
    private EstoqueService estoqueService;

    @Autowired
    private ProcessoService processoService;

    @Autowired
    private MontagemService montagemService;

    @Autowired
    private ExpedicaoService expedicaoService;

    // =========================================================================
    // POST /start-readings
    // =========================================================================
    @PostMapping("/start-readings")
    public ResponseEntity<String> startReadings(@RequestBody Map<String, String> ips) {
        ips.forEach((nome, ip) -> {
            if (!readingFutures.containsKey(nome)) {
                PlcConnector plcConnector = plcConnectionService.getConnection(ip);
                if (plcConnector == null) {
                    System.err.println("Erro ao obter conexão com o CLP: " + ip);
                    return;
                }

                PlcReaderDB task = null;
                PlcReaderMultDB taskMult = null;
                long delayMs = 600;

                switch (nome.toLowerCase()) {
                    case "estoque" -> {
                        taskMult = new PlcReaderMultDB(
                                plcConnector,
                                nome,
                                new PlcReaderMultDB.PlcReadRequest(9, 0, 111),
                                new PlcReaderMultDB.PlcReadRequest(6, 0, 60),
                                new PlcReaderMultDB.PlcReadRequest(0, 0, 0),
                                new PlcReaderMultDB.PlcReadRequest(0, 0, 0),
                                new PlcReaderMultDB.PlcReadRequest(0, 0, 0),
                                dados -> {
                                    ClpController.dataClp1 = dados;
                                    estoqueService.processData(ip, dados);
                                    updateCache("estoque", dados);
                                });
                        delayMs = 600;
                    }
                    case "processo" -> {
                        task = new PlcReaderDB(plcConnector, nome, 2, 0, 9, dados -> {
                            ClpController.dataClp2 = dados;
                            processoService.processData(ip, dataClp2);
                            updateCache("processo", dados);
                        });
                        delayMs = 400;
                    }
                    case "montagem" -> {
                        taskMult = new PlcReaderMultDB(
                                plcConnector,
                                nome,
                                new PlcReaderMultDB.PlcReadRequest(57, 0, 9),
                                new PlcReaderMultDB.PlcReadRequest(30, 16, 16),
                                new PlcReaderMultDB.PlcReadRequest(600, 14, 16),
                                new PlcReaderMultDB.PlcReadRequest(92, 2, 16),
                                new PlcReaderMultDB.PlcReadRequest(60, 20, 16),
                                dados -> {
                                    ClpController.dataClp3 = dados;
                                    montagemService.processData(ip, dados);
                                    updateCache("montagem", dados);
                                });
                        delayMs = 400;
                    }
                    case "expedicao" -> {
                        task = new PlcReaderDB(plcConnector, nome, 9, 0, 48, dados -> {
                            ClpController.dataClp4 = dados;
                            expedicaoService.processData(ip, dados);
                            updateCache("expedicao", dados);
                        });
                        delayMs = 600;
                    }
                    default -> {
                        System.err.println("Nome de CLP inválido: " + nome);
                        return;
                    }
                }

                Runnable toSchedule = task != null ? task : taskMult;
                if (toSchedule != null) {
                    ScheduledFuture<?> future = readingExecutor.scheduleWithFixedDelay(
                            toSchedule, 0, delayMs, TimeUnit.MILLISECONDS);
                    readingFutures.put(nome, future);
                }
            }
        });

        return ResponseEntity.ok("Leituras com PlcReaderTask iniciadas.");
    }

    private void updateCache(String nome, byte[] dados) {
        StringBuilder sb = new StringBuilder();
        for (byte b : dados) {
            sb.append(String.format("%02X ", b));
        }
        readingsCache.put(nome, sb.toString().trim());
    }

    // =========================================================================
    // GET /data/{clp}
    // =========================================================================
    @GetMapping("/data/{clp}")
    public ResponseEntity<String> getData(@PathVariable String clp) {
        byte[] dados = switch (clp.toLowerCase()) {
            case "clp1" -> dataClp1;
            case "clp2" -> dataClp2;
            case "clp3" -> dataClp3;
            case "clp4" -> dataClp4;
            default     -> null;
        };

        if (dados == null) {
            return ResponseEntity.ok("Ainda não há dados para " + clp);
        }

        StringBuilder builder = new StringBuilder();
        for (byte b : dados) {
            builder.append(String.format("%02X ", b));
        }
        return ResponseEntity.ok(builder.toString().trim());
    }

    // =========================================================================
    // POST /stop-readings
    // =========================================================================
    @PostMapping("/stop-readings")
    public ResponseEntity<String> stopReadings() {
        readingFutures.forEach((nome, future) -> {
            future.cancel(true);
            System.out.println("Thread de leitura '" + nome + "' cancelada.");
        });
        readingFutures.clear();
        plcConnectionService.closeAll();
        return ResponseEntity.ok("Leituras interrompidas e eventos registrados.");
    }

    // =========================================================================
    // GET /status
    // JSON com o estado interpretado de cada estação para o frontend de
    // acompanhamento. Lê os campos estáticos dataClp1..4 já populados pelas
    // threads de leitura acima — sem abrir novas conexões ou duplicar lógica.
    //
    // Convenção de status:  0 = aguardando | 1 = em andamento | 2 = concluído
    //
    // Estrutura devolvida:
    // {
    //   "estoque":   { "statusEstoque": 0-2, "statusProcesso": 0-2,
    //                  "statusMontagem": 0-2, "statusExpedicao": 0-2,
    //                  "pedidoEmCurso": bool },
    //   "processo":  { "status": 0-2 },
    //   "montagem":  { "status": 0-2 },
    //   "expedicao": { "status": 0-2 }
    // }
    //
    // Os status são lidos diretamente de SmartService (variáveis estáticas
    // atualizadas pelos services de cada estação), que é a fonte de verdade
    // já existente no projeto — sem replicar parsing de bytes aqui.
    // =========================================================================
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> resp = new HashMap<>();

        // --- Estoque -----------------------------------------------------------
        // SmartService mantém os status globais de produção como campos estáticos,
        // atualizados pelos services (EstoqueService, ProcessoService, etc.)
        Map<String, Object> estoque = new HashMap<>();
        estoque.put("statusEstoque",   SmartService.statusEstoque   & 0xFF);
        estoque.put("statusProcesso",  SmartService.statusProcesso  & 0xFF);
        estoque.put("statusMontagem",  SmartService.statusMontagem  & 0xFF);
        estoque.put("statusExpedicao", SmartService.statusExpedicao & 0xFF);
        estoque.put("pedidoEmCurso",   SmartService.pedidoEmCurso);
        resp.put("estoque", estoque);

        // --- Processo ----------------------------------------------------------
        Map<String, Object> processo = new HashMap<>();
        processo.put("status", SmartService.statusProcesso & 0xFF);
        resp.put("processo", processo);

        // --- Montagem ----------------------------------------------------------
        Map<String, Object> montagem = new HashMap<>();
        montagem.put("status", SmartService.statusMontagem & 0xFF);
        resp.put("montagem", montagem);

        // --- Expedição ---------------------------------------------------------
        Map<String, Object> expedicao = new HashMap<>();
        expedicao.put("status", SmartService.statusExpedicao & 0xFF);
        resp.put("expedicao", expedicao);

        return ResponseEntity.ok(resp);
    }

    // =========================================================================
    // GET /smartstream/{bancada}  — SSE (sem alteração)
    // =========================================================================
    @GetMapping("/smartstream/{bancada}")
    public SseEmitter smartStream(@PathVariable String bancada) {
        SseEmitter emitter = new SseEmitter(0L);
        ExecutorService sseExecutor = Executors.newSingleThreadExecutor();

        sseExecutor.execute(() -> {
            try {
                while (true) {
                    byte[] dados = switch (bancada.toLowerCase()) {
                        case "estoque" -> {
                            byte[] extendidoEst = new byte[dataClp1.length + 6];
                            System.arraycopy(dataClp1, 0, extendidoEst, 0, dataClp1.length);
                            extendidoEst[extendidoEst.length - 6] = SmartService.statusEstoque;
                            extendidoEst[extendidoEst.length - 5] = SmartService.statusProcesso;
                            extendidoEst[extendidoEst.length - 4] = SmartService.statusMontagem;
                            extendidoEst[extendidoEst.length - 3] = SmartService.statusExpedicao;
                            extendidoEst[extendidoEst.length - 2] = SmartService.statusProducao;
                            extendidoEst[extendidoEst.length - 1] = (byte) (SmartService.pedidoEmCurso ? 1 : 0);
                            yield extendidoEst;
                        }
                        case "processo"  -> dataClp2;
                        case "montagem"  -> dataClp3;
                        case "expedicao" -> dataClp4;
                        default          -> null;
                    };

                    if (dados != null) {
                        StringBuilder hexBuilder = new StringBuilder();
                        for (byte b : dados) {
                            hexBuilder.append(String.format("%02X ", b));
                        }
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("leitura")
                                    .data(hexBuilder.toString().trim()));
                        } catch (IOException | IllegalStateException ex) {
                            emitter.complete();
                            break;
                        }
                    }

                    TimeUnit.MILLISECONDS.sleep(400);
                }
            } catch (InterruptedException e) {
                emitter.completeWithError(e);
                Thread.currentThread().interrupt();
            }
        });

        emitter.onCompletion(() -> System.out.println("SSE finalizado para " + bancada));
        emitter.onTimeout(emitter::complete);
        emitter.onError(emitter::completeWithError);

        return emitter;
    }

    // =========================================================================
    // Utilitários diversos (sem alteração)
    // =========================================================================
    @PostMapping("/smart/ping")
    public Map<String, Boolean> pingHosts(@RequestBody Map<String, String> ips) {
        Map<String, Boolean> resultados = new HashMap<>();
        ips.forEach((nome, ip) -> {
            boolean online = false;
            try (Socket socket = new Socket()) {
                SocketAddress address = new InetSocketAddress(ip, 102);
                socket.connect(address, 2000);
                online = true;
            } catch (IOException e) {
                online = false;
            }
            System.out.println(nome + ": " + online);
            resultados.put(nome, online);
        });
        return resultados;
    }

    @PostMapping("/smart/reset-status")
    public ResponseEntity<String> resetarStatus() {
        smartService.resetarStatus();
        return ResponseEntity.ok("Status zerados com sucesso.");
    }

    @PostMapping("/smart/readonly")
    public ResponseEntity<String> setReadOnly(@RequestParam boolean value) {
        smartService.setReadOnly(value);
        return ResponseEntity.ok("Modo readOnly: " + value);
    }

    @GetMapping("/smart/readonly")
    public boolean getReadOnly() {
        return smartService.isReadOnly();
    }
}