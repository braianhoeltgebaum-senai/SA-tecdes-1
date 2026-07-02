package com.sa.smart.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sa.smart.clpcomm.PlcConnectionService;
import com.sa.smart.clpcomm.PlcConnector;

@Service
public class MontagemService {

    //********************** Monatgem **************************
    //----------------------- NodeToPlc ------------------------
    /*---- StatusOP -------*/
    boolean recebidoOpMon = false;
    //----------------------- PlcToNode ------------------------
    /*---- StatusOP -------*/
    int numeroOPMon = 0;
    boolean cancelOPMon = false;
    boolean finishOPMon = false;
    boolean startOPMon = false;

    /*---- StatusEstacao -------*/
    boolean ocupadoMon = false;
    boolean aguardandoMon = false;
    boolean manualMon = false;
    boolean emergenciaMon = false;

    /*---- RealidadeAumentada --------*/
    boolean xEmergenciaAtivadaMon = false;
    boolean xComutadorAutomaticoMon = false;
    boolean xCondicaoIniciarMon = false;

    @Autowired
    private PlcConnectionService plcConnectionService;

    @Autowired
    private RastreamentoService rastreamentoService; // NOVO

    public void processData(String ip, byte[] dadosClp3) {
        // lógica que hoje está no método clpMontagem(...)

        //-------------- Apresentação no console -----------------
        StringBuilder leituraClp3 = new StringBuilder();
        for (byte b : dadosClp3) {
            leituraClp3.append(String.format("%02X ", b));
        }
        String clp3 = leituraClp3.toString().trim();
        //System.out.println("[CLP3] (" + dadosClp3.length + " bytes): " + clp3);

        PlcConnector plcConnectorMon = plcConnectionService.getConnection(ip);
        if (plcConnectorMon == null) {
            return;
        }
        //-------------- Leitura das variáveis -------------------
        recebidoOpMon = (dadosClp3[0] & 0x01) != 0;

        numeroOPMon = ((dadosClp3[2] & 0xFF) << 8) | (dadosClp3[3] & 0xFF);
        cancelOPMon = (dadosClp3[4] & 0x01) != 0;
        finishOPMon = (dadosClp3[4] & 0x02) != 0;
        startOPMon = (dadosClp3[4] & 0x04) != 0;

        ocupadoMon = (dadosClp3[6] & 0x01) != 0;
        aguardandoMon = (dadosClp3[6] & 0x02) != 0;
        manualMon = (dadosClp3[6] & 0x04) != 0;
        emergenciaMon = (dadosClp3[6] & 0x08) != 0;

        // NOVO: repassa para o rastreamento, sem alterar a lógica original abaixo
        rastreamentoService.processarMontagem(numeroOPMon, startOPMon, finishOPMon);

        // System.out.println("StatusEstoque: " + statusEstoque + "\n"
        //         + "StatusProcesso: " + statusProcesso + "\n"
        //         + "StatusMontagem: " + statusMontagem + "\n"
        //         + "StatusExpedicao: " + statusExpedicao + "\n");
        // Se as três flags (StartOPMon, FinishOPMon e CancelOPMon) estão em FALSE, então a flag
        // RecebidoOPMon fica em FALSE
        if (startOPMon == false && finishOPMon == false && cancelOPMon == false) {
            if (SmartService.readOnly == false) {

                try {

                    plcConnectorMon.writeBit(57, 0, 0, Boolean.parseBoolean("FALSE")); // coloca RecebidoOPMon em FALSE
                } catch (Exception ex) {
                }
            }
        }
        // Se a estação MONTAGEM sinalizou o inicio da operação e recebidoOpMon está em FALSE, então a
        // flag RecebidoOPMon fica em TRUE
        if (startOPMon == true && recebidoOpMon == false) {
            if (SmartService.statusProducao == 0 & SmartService.pedidoEmCurso == true) {
                SmartService.statusMontagem = 1;
            } else {
                //statusMontagem = 0;
            }

            // updateDisplayStation();
            if (SmartService.readOnly == false) {

                try {

                    plcConnectorMon.writeBit(57, 0, 0, Boolean.parseBoolean("TRUE")); // coloca RecebidoOPMon em TRUE
                } catch (Exception ex) {
                }

            }

        }

        // Se a estação MONTAGEM sinalizou o témino da operação e ficou OCUPADO, então a
        // flag RecebidoOP fica em TRUE
        if (finishOPMon == true && recebidoOpMon == false) {
            if (SmartService.readOnly == false) {

                try {
                    plcConnectorMon.writeBit(57, 0, 0, Boolean.parseBoolean("TRUE"));  // coloca RecebidoOPMon em TRUE
                } catch (Exception e) {
                    e.printStackTrace();
                } // RecebidoOPMon
                if (SmartService.statusProducao == 0 & SmartService.pedidoEmCurso == true) {
                    SmartService.statusMontagem = 2;
                } else {
                    //statusMontagem = 0;
                }

            }
        }
    }


}