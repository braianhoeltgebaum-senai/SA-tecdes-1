package com.sa.smart.service;

import java.net.http.HttpHeaders;
import java.nio.ByteBuffer;
import java.text.DateFormat.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
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

    // Variáveis globais do programa
    public static boolean readOnly = false;
    public static boolean aux_expedicao = false; // Aux Expedição

    public static boolean pedidoEmCurso = false;
    public static byte statusProducao = 0;

    public static byte statusEstoque = 0;
    public static byte statusProcesso = 0;
    public static byte statusMontagem = 0;
    public static byte statusExpedicao = 0;

    public static int posicaoEstoqueSolicitada = 0;
    public static int posicaoExpedicaoSolicitada = 0;

    public static boolean blockFinished = false;

    @Autowired
    private PlcConnectionService plcConnectionService;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private ExpedicaoRepository expedicaoRepository;

    @Autowired
    private ApiUrlConfig apiUrlConfig;

    /*---- RealidadeAumentada */
    boolean xEmergenciaAtivadaExp = false;
    boolean xComutadorAutomaticoExp = false;
    boolean xNecessitaHomeEixoVerticalExp = false;
    boolean xNecessitaHomeEixoGiroExp = false;
    boolean xNecessitaHomeEixoHorizontalExp = false;
    boolean xServoDesligadoEixoHorizontalExp = false;
    boolean xServoDesligadoEixoGiroExp = false;
    boolean xServoDesligadoEixoVerticalExp = false;
    boolean xCondicaoIniciarExp = false;

    private Map<String, List<String>> eventosCLP = new ConcurrentHashMap<>();

    public void chamarApis() {
        String estoqueUrl = apiUrlConfig.getEstoqueApiUrl();
        String expedicaoUrl = apiUrlConfig.getExpedicaoApiUrl();

        System.out.println("Chamando estoque em: " + estoqueUrl);
        System.out.println("Chamando expedição em: " + expedicaoUrl);
    }

    public boolean sendBlockBytesToClp(String ipClp, int db, int offset, byte[] dados, int size) {
        PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);
        if (plcConnector == null) {
            System.out.println("Sem conexão com CLP " + ipClp);
            return false;
        }
        if (!readOnly) {
            try {
                plcConnector.writeBlock(db, offset, size, dados); // escreve no bloco de dados
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        // se for readOnly ou nada a fazer, considere sucesso
        return true;
    }

    //*************************************************************
    // Função para iniciar a Execução do pedido
    //*************************************************************
    public void startExecuteOrder(String ipClp) {

        // Etapas a desenvolver:
        // 1 - ATUALIZAR O PRÓXIMO NÚMERO DE PEDIDO
        // MainFrame.posExpedArray[12] = MainFrame.posExpedArray[12] + 1;
        // int orderProduction = obterProximoPedido();
        //PlcConnector plcConnector = new PlcConnector(ipClp, 102); // ajuste o IP se necessário
        if (!readOnly) {

            PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);
            if (plcConnector == null) {
                return;
            }

            posicaoExpedicaoSolicitada = searchFirstPositionFreeExp();

            try {

                // Inicializa as flags da estação ESTOQUE
                //plcConnector.connect();
                plcConnector.writeBit(9, 0, 0, Boolean.parseBoolean("FALSE"));
                plcConnector.writeBit(9, 64, 0, Boolean.parseBoolean("FALSE"));
                plcConnector.writeBit(9, 64, 1, Boolean.parseBoolean("FALSE"));
                plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE"));

                // plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE"));
                // Iniciar pedido
                System.out.println("INICIAR PEDIDO 2");
                plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("TRUE"));

            } catch (Exception ex) {

            }
        }
    }

    //***************************************************************
    // Funções para gerenciamento de posições no Estoque e Expedição
    //***************************************************************
    //********************************************************************************************************************************************** */
    public int SearchFirstPositionByColor(int cor, Set<Integer> posicoesUsadas) {
        List<Estoque> estoque = estoqueRepository.findByCorOrderByPosicaoEstoqueAsc(cor);

        for (Estoque e : estoque) {
            if (!posicoesUsadas.contains(e.getPosicaoEstoque())) {
                return e.getPosicaoEstoque();
            }
        }

        return -1; // Nenhuma posição disponível
    }

    public int searchFirstPositionFreeExp() {
        List<Integer> ocupadas = expedicaoRepository.findAllPosicoesOcupadas();

        for (int i = 1; i <= 12; i++) {
            if (!ocupadas.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    //*************************************************************
    // Função para reiniciar status de operação das estações
    //*************************************************************
    public void resetarStatus() {
        statusEstoque = 0;
        statusProcesso = 0;
        statusMontagem = 0;
        statusExpedicao = 0;
    }

    //*************************************************************
    // Funções para gerenciamento do modo Leitura
    //*************************************************************
    public boolean isReadOnly() {
        return readOnly;
    }

    public void enviarParaProducao(PedidoConfigDTO config, PedidoInfoDTO detalhes) {

        byte[] buffer = converterParaBytes(detalhes);
        printHex(buffer);

        PlcConnector connector = plcConnectionService.getConnection(config.getIpClp());

        if (connector != null) {
            try {
                connector.writeBlock(9, 2, 60, buffer);
                System.out.println("Dados enviados para o CLP: " + config.getIpClp());

                // FIX 3: passa corTampa do config para o método que precisava de 'tampa'
                iniciarExecucaoPedido(config.getIpClp(), config.getCorTampa());

            } catch (Exception ex) {
                System.err.println("Erro ao enviar dados para o CLP: " + ex.getMessage());
            }
        }
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        System.out.println("readOnly: " + readOnly);
    }

    public void printHex(byte[] bytes) {

        StringBuilder sb = new StringBuilder();
        System.out.println("--- BLOCO DE BYTES (HEXADECIMAL) ---");

        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
            if ((i + 1) % 10 == 0) {
                sb.append("\n");
            }
        }

        System.out.println(sb.toString());
        System.out.println("------------------------------------");
    }

    private byte[] converterParaBytes(PedidoInfoDTO dto) {

        Field[] campos = dto.getClass().getDeclaredFields();
        ByteBuffer buffer = ByteBuffer.allocate(campos.length * 2);

        try {
            for (Field campo : campos) {
                campo.setAccessible(true);
                int valor = campo.getInt(dto);
                buffer.putShort((short) valor);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return buffer.array();
    }

    public void startExecuteOrder(String ipClp, int corTampa) {

        PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);
        if (plcConnector == null) {
            System.err.println("Não foi possível obter conexão com o CLP: " + ipClp);
            return;
        }

        // --- Passo 1: escreve flags no CLP ---
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
            // FIX 4: era "return ResponseEntity.status(...)" — inválido em void
            System.err.println("Erro ao escrever bits no CLP: " + ex.getMessage());
        }

        // --- Passo 2: seleciona a tampa no ESP32 via HTTP ---
        try {
            RestTemplate apiSeletorTampa = new RestTemplate();
            String url = "http://10.74.241.245/api/move_pos";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("pos",    String.valueOf(corTampa)); // FIX 3: era 'tampa' (variável inexistente)
            map.add("offset", "0");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            // Lê a resposta bruta primeiro para facilitar debug
            var rawResponse = apiSeletorTampa.postForEntity(url, request, String.class);
            System.out.println("Resposta bruta do ESP32: " + rawResponse.getBody());

            var response = apiSeletorTampa.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || body.get("status") == null) {
                // FIX 4: era "return ResponseEntity.status(...)" — inválido em void
                System.err.println("Erro: seletor de tampas enviou corpo vazio ou sem 'status'. "
                        + "Conteúdo: " + rawResponse.getBody());
                return;
            }

            String status = body.get("status").toString();

            if (!status.toLowerCase().contains("ok")) {
                // FIX 4: era "return ResponseEntity.status(...)" — inválido em void
                System.err.println("Erro: seletor de tampas não confirmou 'Ok'. Resposta: " + status);
            }

        } catch (Exception e) {
            // FIX 4: era "return ResponseEntity.status(...)" — inválido em void
            System.err.println("Erro ao comunicar com o seletor de tampas: " + e.getMessage());
            e.printStackTrace();
        }
    }

}

