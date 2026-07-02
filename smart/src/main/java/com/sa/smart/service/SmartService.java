package com.sa.smart.service;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.sa.smart.clpcomm.PlcConnectionService;
import com.sa.smart.clpcomm.PlcConnector;
import com.sa.smart.config.ApiUrlConfig;
import com.sa.smart.dto.PedidoConfigDTO;
import com.sa.smart.dto.PedidoInfoDTO;
import com.sa.smart.model.Estoque;
import com.sa.smart.repository.EstoqueRepository;
import com.sa.smart.repository.ExpedicaoRepository;

@Service
public class SmartService {

    // ─── Variáveis globais de estado da produção ──────────────────────────────
    public static boolean readOnly              = false;
    public static boolean aux_expedicao         = false;

    // pedidoEmCurso é a flag que "abre" o ciclo de rastreamento automático.
    // Enquanto ela for false, RastreamentoService NUNCA vai gravar mudanças
    // de status no banco (Em Produção / Concluído) — é o guard usado por
    // todas as bancadas (Estoque, Processo, Montagem, Expedição).
    public static boolean pedidoEmCurso         = false;

    public static byte    statusProducao        = 0;
    public static byte    statusEstoque         = 0;
    public static byte    statusProcesso        = 0;
    public static byte    statusMontagem        = 0;
    public static byte    statusExpedicao       = 0;
    public static int     posicaoEstoqueSolicitada  = 0;
    public static int     posicaoExpedicaoSolicitada = 0;
    public static boolean blockFinished         = false;

    @Autowired private PlcConnectionService plcConnectionService;
    @Autowired private EstoqueRepository    estoqueRepository;
    @Autowired private ExpedicaoRepository  expedicaoRepository;
    @Autowired private ApiUrlConfig         apiUrlConfig;

    private final Map<String, List<String>> eventosCLP = new ConcurrentHashMap<>();

    // ─── Envio de bloco de bytes ao CLP ──────────────────────────────────────

    public boolean sendBlockBytesToClp(String ipClp, int db, int offset, byte[] dados, int size) {
        PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);
        if (plcConnector == null) {
            System.out.println("Sem conexão com CLP " + ipClp);
            return false;
        }
        if (!readOnly) {
            try {
                plcConnector.writeBlock(db, offset, size, dados);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    // ─── Envio de pedido ao CLP e início da execução ─────────────────────────

    public void enviarParaProducao(PedidoConfigDTO config, PedidoInfoDTO detalhes) {
        byte[] buffer = converterParaBytes(detalhes);
        printHex(buffer);

        PlcConnector connector = plcConnectionService.getConnection(config.getIpClp());
        if (connector != null) {
            try {
                connector.writeBlock(9, 2, 60, buffer);
                System.out.println("Dados enviados para o CLP: " + config.getIpClp());

                // NOVO: abre o ciclo de rastreamento automático deste pedido.
                // Sem isso, RastreamentoService nunca considera nenhuma bancada
                // (incluindo a Expedição) como parte de um pedido "em curso",
                // e por isso nunca grava Em Produção/Concluído no banco.
                // Também garante que os status de cada bancada comecem
                // zerados, para não herdar valores de um ciclo anterior.
                statusEstoque   = 0;
                statusProcesso  = 0;
                statusMontagem  = 0;
                statusExpedicao = 0;
                statusProducao  = 0;
                pedidoEmCurso   = true;

                iniciarExecucaoPedido(config.getIpClp(), config.getCorTampa());
            } catch (Exception ex) {
                System.err.println("Erro ao enviar dados para o CLP: " + ex.getMessage());
            }
        }
    }

    /**
     * Inicia a execução do pedido no CLP e, se configurado, aciona o seletor de tampas.
     * Usado por ProducaoController e SmartController.
     */
    public void iniciarExecucaoPedido(String ipClp, int corTampa) {
        PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);
        if (plcConnector == null) {
            System.err.println("Não foi possível obter conexão com o CLP: " + ipClp);
            return;
        }

        // Passo 1: escreve flags no CLP
        try {
            plcConnector.writeBit(9, 0,  0, false);
            plcConnector.writeBit(9, 64, 0, false);
            plcConnector.writeBit(9, 64, 1, false);
            plcConnector.writeBit(9, 62, 0, false);

            System.out.println("SETAR FLAG INICIAR PEDIDO");
            plcConnector.writeBit(9, 62, 0, true);

            Thread.sleep(800);

            System.out.println("RESETAR FLAG INICIAR PEDIDO");
            plcConnector.writeBit(9, 62, 0, false);

        } catch (Exception ex) {
            System.err.println("Erro ao escrever bits no CLP: " + ex.getMessage());
        }

        // Passo 2: aciona seletor de tampas via ESP32 (somente se configurado)
        if (apiUrlConfig.isSeletorTampasPresent()) {
            try {
                RestTemplate apiSeletorTampa = new RestTemplate();
                String url = "http://10.74.241.245/api/move_pos";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
                map.add("pos",    String.valueOf(corTampa));
                map.add("offset", "0");

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

                var rawResponse = apiSeletorTampa.postForEntity(url, request, String.class);
                System.out.println("Resposta bruta do ESP32: " + rawResponse.getBody());

                @SuppressWarnings("unchecked")
                var response = apiSeletorTampa.postForEntity(url, request, Map.class);
                Map<String, Object> body = response.getBody();

                if (body == null || body.get("status") == null) {
                    System.err.println("Erro: seletor de tampas enviou corpo vazio ou sem 'status'. "
                            + "Conteúdo: " + rawResponse.getBody());
                    return;
                }

                String status = body.get("status").toString();
                if (!status.toLowerCase().contains("ok")) {
                    System.err.println("Erro: seletor de tampas não confirmou 'Ok'. Resposta: " + status);
                }

            } catch (Exception e) {
                System.err.println("Erro ao comunicar com o seletor de tampas: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Sobrecarga sem corTampa — usada pelo SmartController (fluxo legado).
     * Apenas seta e reseta a flag de iniciar pedido no CLP, sem acionar o seletor de tampas.
     */
    public void startExecuteOrder(String ipClp) {
        if (!readOnly) {
            PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);
            if (plcConnector == null) return;

            posicaoExpedicaoSolicitada = searchFirstPositionFreeExp();

            try {
                plcConnector.writeBit(9, 0,  0, false);
                plcConnector.writeBit(9, 64, 0, false);
                plcConnector.writeBit(9, 64, 1, false);
                plcConnector.writeBit(9, 62, 0, false);

                System.out.println("INICIAR PEDIDO 2");
                plcConnector.writeBit(9, 62, 0, true);
            } catch (Exception ex) {
                System.err.println("Erro ao iniciar pedido no CLP: " + ex.getMessage());
            }
        }
    }

    // ─── Gerenciamento de posições ────────────────────────────────────────────

    public int SearchFirstPositionByColor(int cor, Set<Integer> posicoesUsadas) {
        List<Estoque> estoque = estoqueRepository.findByCorOrderByPosicaoEstoqueAsc(cor);
        for (Estoque e : estoque) {
            if (!posicoesUsadas.contains(e.getPosicaoEstoque())) {
                return e.getPosicaoEstoque();
            }
        }
        return -1;
    }

    public int searchFirstPositionFreeExp() {
        List<Integer> ocupadas = expedicaoRepository.findAllPosicoesOcupadas();
        for (int i = 1; i <= 12; i++) {
            if (!ocupadas.contains(i)) return i;
        }
        return -1;
    }

    // ─── Utilitários ─────────────────────────────────────────────────────────

    public void resetarStatus() {
        statusEstoque  = 0;
        statusProcesso = 0;
        statusMontagem = 0;
        statusExpedicao = 0;
    }

    public boolean isReadOnly() { return readOnly; }

    public void setReadOnly(boolean value) {
        readOnly = value;
        System.out.println("readOnly: " + value);
    }

    public void chamarApis() {
        System.out.println("Chamando estoque em: "  + apiUrlConfig.getEstoqueApiUrl());
        System.out.println("Chamando expedição em: " + apiUrlConfig.getExpedicaoApiUrl());
    }

    public void printHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        System.out.println("--- BLOCO DE BYTES (HEXADECIMAL) ---");
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
            if ((i + 1) % 10 == 0) sb.append("\n");
        }
        System.out.println(sb);
        System.out.println("------------------------------------");
    }

    // CORRIGIDO: import correto (java.lang.reflect.Field) e conversão via getInt segura
    private byte[] converterParaBytes(PedidoInfoDTO dto) {
        Field[] campos = dto.getClass().getDeclaredFields();
        ByteBuffer buffer = ByteBuffer.allocate(campos.length * 2);
        for (Field campo : campos) {
            campo.setAccessible(true);
            try {
                int valor = campo.getInt(dto);
                buffer.putShort((short) valor);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                // Campo não é int (ex.: anotações Lombok) — ignora
                buffer.putShort((short) 0);
            }
        }
        return buffer.array();
    }
}