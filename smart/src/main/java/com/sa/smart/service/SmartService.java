package com.sa.smart.service;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.sa.smart.clpcomm.PlcConnectionService;
import com.sa.smart.clpcomm.PlcConnector;
import com.sa.smart.dto.PedidoConfigDTO;
import com.sa.smart.dto.PedidoInfoDTO;

import lombok.RequiredArgsConstructor;

@Service                    // FIX 1: anotação estava faltando — Spring não registrava o bean
@RequiredArgsConstructor
public class SmartService {

    private final PlcConnectionService plcConnectionService;

    // -------------------------------------------------------------------------
    // Envia os dados do pedido ao CLP e dispara a produção
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Converte o PedidoInfoDTO para bloco de bytes (cada campo int → Short de 2 bytes)
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Printa o bloco de bytes em hexadecimal no console (debug)
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Seta as flags no CLP para iniciar o pedido e seleciona a tampa no ESP32
    //
    // FIX 3: parâmetro 'corTampa' adicionado — era referenciado como 'tampa'
    //         sem estar declarado em lugar nenhum (erro de compilação)
    // FIX 4: 'return ResponseEntity...' removido — método é void, não pode retornar
    //         ResponseEntity. Erros agora são logados via System.err
    // -------------------------------------------------------------------------
    public void iniciarExecucaoPedido(String ipClp, int corTampa) {

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