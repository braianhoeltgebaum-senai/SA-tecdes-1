package com.sa.smart.service;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import org.springframework.stereotype.Service;
import com.sa.smart.clpcomm.PlcConnectionService;
import com.sa.smart.clpcomm.PlcConnector;
import com.sa.smart.dto.PedidoConfigDTO;
import com.sa.smart.dto.PedidoInfoDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmartService {

    private final PlcConnectionService plcConnectionService;
    
    public void enviarParaProducao(PedidoConfigDTO config, PedidoInfoDTO detalhes) {
        // 1. Converter o DTO para um bloco de bytes (byte[])
        byte[] buffer = converterParaBytes(detalhes);

        printHex(buffer);
        // 2. Obter a conexão única via seu Service
        PlcConnector connector = plcConnectionService.getConnection(config.getIpClp());

        if (connector != null) {
            try {
                // 3. Escrever bloco de bytes no CLP (ex: a partir da DB19, offset 2)
                connector.writeBlock(9, 2, 60, buffer);
                System.out.println("Dados enviados para o CLP: " + config.getIpClp());

                iniciarExecucaoPedido(config.getIpClp());

            } catch (Exception ex) {
                System.err.println("Erro ao enviar dados para o CLP: " + ex.getMessage());
            }
        }
    }

    // Converter PedidoInfoDTO para bloco de bytes a ser enviado para o Clp_Estoque
    private byte[] converterParaBytes(PedidoInfoDTO dto) {
        // Pega todos os campos da classe
        Field[] campos = dto.getClass().getDeclaredFields();

        // Cada int será um Short (2 bytes) no CLP. 
        // Tamanho = número de campos * 2
        ByteBuffer buffer = ByteBuffer.allocate(campos.length * 2);

        try {
            for (Field campo : campos) {
                campo.setAccessible(true); // Permite ler o campo private

                // Pega o valor int do campo no objeto dto
                int valor = campo.getInt(dto);

                // Coloca no buffer como Short (2 bytes)
                buffer.putShort((short) valor);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return buffer.array();
    }

    // Printar bloco de bytes do Pedido no console
    public void printHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        System.out.println("--- BLOCO DE BYTES (HEXADECIMAL) ---");

        for (int i = 0; i < bytes.length; i++) {
            // Converte o byte para Hex e garante que tenha 2 dígitos (ex: 0A em vez de A)
            sb.append(String.format("%02X ", bytes[i]));

            // Opcional: Quebra de linha a cada 10 bytes para facilitar a leitura
            if ((i + 1) % 10 == 0) {
                sb.append("\n");
            }
        }

        System.out.println(sb.toString());
        System.out.println("------------------------------------");
    }

    // Envia comando para a Planta Smart iniciar a produção do Pedido
    public void iniciarExecucaoPedido(String ipClp) {
        PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);
        if (plcConnector == null) {
            return;
        }

        try {

            // Inicializa as flags da estação ESTOQUE
            //plcConnector.connect();
            plcConnector.writeBit(9, 0, 0, Boolean.parseBoolean("FALSE"));
            plcConnector.writeBit(9, 64, 0, Boolean.parseBoolean("FALSE"));
            plcConnector.writeBit(9, 64, 1, Boolean.parseBoolean("FALSE"));
            plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE"));

            // plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE"));
            // Iniciar pedido
            System.out.println("SETAR FLAG INICIAR PEDIDO");
            plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("TRUE"));

            Thread.sleep(800);

            System.out.println("RESETAR FLAG INICIAR PEDIDO");
            plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE"));

        } catch (Exception ex) {

        }
    }
}