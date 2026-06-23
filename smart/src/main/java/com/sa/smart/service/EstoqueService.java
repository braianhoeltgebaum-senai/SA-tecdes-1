package com.sa.smart.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sa.smart.clpcomm.PlcConnector;
import com.sa.smart.dto.EstoqueDTO;
import com.sa.smart.model.Estoque;
import com.sa.smart.repository.EstoqueRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class EstoqueService {

    private final EstoqueRepository repository;

    public EstoqueService(EstoqueRepository repository) {
        this.repository = repository;
    }

    // CREATE
    public EstoqueDTO criar(EstoqueDTO dto) {
        validarCor(dto.cor());

        Estoque e = new Estoque();
        e.setPosicao(dto.posicao());
        e.setCor(dto.cor());

        Estoque saved = repository.save(e);

        return new EstoqueDTO(
                saved.getId(),
                saved.getPosicao(),
                saved.getCor()
        );
    }

    // READ ALL
    public List<EstoqueDTO> listar() {
        return repository.findAll().stream()
                .map(e -> new EstoqueDTO(
                        e.getId(),
                        e.getPosicao(),
                        e.getCor()
                ))
                .toList();
    }

    // READ BY ID
    public EstoqueDTO buscar(Long id) {
        Estoque e = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estoque não encontrado"));

        return new EstoqueDTO(
                e.getId(),
                e.getPosicao(),
                e.getCor()
        );
    }

    // PUT (substituição total)
    public EstoqueDTO put(Long id, EstoqueDTO dto) {
        validarCor(dto.cor());

        Estoque e = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estoque não encontrado"));

        e.setPosicao(dto.posicao());
        e.setCor(dto.cor());

        Estoque updated = repository.save(e);

        return new EstoqueDTO(
                updated.getId(),
                updated.getPosicao(),
                updated.getCor()
        );
    }

    // PATCH (atualização parcial)
    public EstoqueDTO patch(Long id, EstoqueDTO dto) {
        Estoque e = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estoque não encontrado"));

        if (dto.posicao() != null) {    
            e.setPosicao(dto.posicao());
        }
        if (dto.cor() != null) {
            validarCor(dto.cor());
            e.setCor(dto.cor());
        }

        Estoque updated = repository.save(e);

        return new EstoqueDTO(
                updated.getId(),
                updated.getPosicao(),
                updated.getCor()
        );
    }

    // DELETE
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Estoque não encontrado");
        }
        repository.deleteById(id);
    }

    // Validação da cor (0=vazio, 1=preto, 2=vermelho, 3=azul)
    private void validarCor(Integer cor) {
        if (cor == null || cor < 0 || cor > 3) {
            throw new IllegalArgumentException("Cor inválida. Use: 0=vazio, 1=preto, 2=vermelho, 3=azul");
        }
    }

    public int buscarPrimeiraPosicaoPorCor(int cor, Set<Integer> usadas) {
        return estoqueRepository.findByCorOrderByPosicaoEstoqueAsc(cor)
                .stream()
                .filter(e -> !usadas.contains(e.getPosicaoEstoque()))
                .map(Estoque::getPosicaoEstoque)
                .findFirst()
                .orElse(-1);
    }

    public void processData(String ip, byte[] dadosClp1) {
        // lógica que hoje está no método clpEstoque(...)
        //-------------- Apresentação no console -----------------
        StringBuilder leituraClp1 = new StringBuilder();
        for (byte b : dadosClp1) {
            leituraClp1.append(String.format("%02X ", b));
        }
        //String clp1 = leituraClp1.toString().trim();

        //System.out.println("[CLP ESTOQUE] " + clp1);
        PlcConnector plcConnectorEst = plcConnectionService.getConnection(ip);
        if (plcConnectorEst == null) {
            return;
        }
        //-------------- Leitura das variáveis -------------------
        recebidoOpEst = (dadosClp1[0] & 0x01) != 0;

        iniciarPedido = (dadosClp1[62] & (byte) 0x01) != 0;
        recebidoEstoque = (dadosClp1[64] & 0x01) != 0;
        iniciarGuardarEst = (dadosClp1[64] & 0x02) != 0;

        posicaoGuardarEst = ((dadosClp1[66] & 0xFF) << 8) | (dadosClp1[67] & 0xFF);
        
        for (int c = 0; c < 28; c++) {
            posicoesOcupadas[c] = dadosClp1[68 + c];
        }
        
        numeroOPEst = ((dadosClp1[96] & 0xFF) << 8) | (dadosClp1[97] & 0xFF);
        cancelOPEst = (dadosClp1[98] & 0x01) != 0;
        finishOPEst = (dadosClp1[98] & 0x02) != 0;
        startOPEst = (dadosClp1[98] & 0x04) != 0;

        ocupadoEst = (dadosClp1[100] & 0x01) != 0;
        aguardandoEst = (dadosClp1[100] & 0x02) != 0;
        manualEst = (dadosClp1[100] & 0x04) != 0;
        emergenciaEst = (dadosClp1[100] & 0x08) != 0;

        pedirPosicaoEst = (dadosClp1[102] & 0x01) != 0;
        posicaoEstoque = ((dadosClp1[104] & 0xFF) << 8) | (dadosClp1[105] & 0xFF);
        adicionarEstoque = (dadosClp1[106] & 0x01) != 0;

        removerEstoque = (boolean) ((dadosClp1[106] & 0x02) != 0);

        retornoEstoqueCheio = (dadosClp1[106] & 0x04) != 0;
        corGuardarEstoque = ((dadosClp1[108] & 0xFF) << 8) | (dadosClp1[109] & 0xFF);

        removerEstoque = (dadosClp1[106] & 0x02) != 0;

        // System.out.println("StatusEstoque: " + statusEstoque + "\n"
        //         + "StatusProcesso: " + statusProcesso + "\n"
        //         + "StatusMontagem: " + statusMontagem + "\n"
        //         + "StatusExpedicao: " + statusExpedicao + "\n");
        // //--------------------------------------------------------
        // Se o pedido foi iniciado e a estação ESTOQUE informou que iniciou a operação
        // ficando no estado OCUPADO
        // então a flag iniciarPedido fica em FALSE
        if (iniciarPedido == true && ocupadoEst == true) {
            SmartService.pedidoEmCurso = true;
            SmartService.statusEstoque = 0;
            SmartService.statusProducao = 0;
            //updateDisplayStation();
            //eventos.add("Seq " + seq++ + ": iniciarPedido == true & ocupadoEst == true");
            if (!SmartService.readOnly) {
                //System.out.println("Flag: IniciarPedido: Verificando se a estação ESTOQUE iniciou o pedido...");

                // System.out.println("Flag: IniciarPedido: ESTOQUE iniciou o pedido...");
                try {
                    // System.out.println("Flag IniciarPedido: " + plcConnector.readBit(9, 62, 0));
                    //System.out.println("colocando IniciarPedido em FALSE");
                    //eventos.add("Seq " + seq++ + ": coloca IniciarPedido em FALSE");
                    plcConnectorEst.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE")); // coloca IniciarPedido em FALSE

                    // System.out.println("Flag IniciarPedido: " + plcConnector.readBit(9, 62, 0));
                    // System.out.println("Flag ocupadoEst: " + plcConnector.readBit(9, 100, 0));
                } catch (Exception e) {
                    System.out.println(
                            "ERRO [iniciarPedido == true & ocupadoEst == true]: Atualização da Flag IniciarPedido [DB9:62.0] para FALSE");
                }
            }

        }

        // Se as três flags (StartOP, FinishOP e CancelOP) estão em FALSE, então a flag
        // RecebidoOP fica em FALSE
        if (startOPEst == false & finishOPEst == false & cancelOPEst == false) {
            //eventos.add("Seq " + seq++ + ": startOPEst == false & finishOPEst == false & cancelOPEst == false");
            if (SmartService.readOnly == false) {

                try {
                    //System.out.println("Seq " + seq++ + ": colocando RecebidoOPEst em FALSE");
                    //eventos.add("Seq " + seq++ + ": coloca RecebidoOPEst em FALSE");
                    plcConnectorEst.writeBit(9, 0, 0, Boolean.parseBoolean("FALSE")); // coloca RecebidoOPEst em FALSE

                } catch (Exception e) {
                    System.out.println("ERRO: Atualização da Flag RecebidoOPEstoque [DB9:0.0] para FALSE");
                }
            }
        }

        // Se a estação ESTOQUE sinalizou o início da operação e ficou OCUPADO, então a
        // flag RecebidoOP fica em TRUE
        if (startOPEst == true & recebidoOpEst == false) {
            if (SmartService.statusProducao == 0 & SmartService.pedidoEmCurso == true) {
                SmartService.statusEstoque = 1;
            } else {
                //statusEstoque = 0;
            }
            // updateDisplayStation();
            //eventos.add("Seq " + seq++ + ": startOPEst == true & recebidoOpEst == false");
            if (SmartService.readOnly == false) {
                //System.out.println("Flag: RecebidoOPEstoque_TRUE");
                try {
                    //System.out.println("StartOP: colocando RecebidoOPEstoque em TRUE");
                    //eventos.add("Seq " + seq++ + ": coloca RecebidoOPEst em TRUE");
                    plcConnectorEst.writeBit(9, 0, 0, Boolean.parseBoolean("TRUE")); // coloca RecebidoOPEst em TRUE

                } catch (Exception e) {
                    System.out.println(
                            "ERRO [startOp]: Atualização da Flag RecebidoOPEstoque [DB9:0.0] para TRUE");
                }
            }
        }

        // Se a estação ESTOQUE sinalizou o témino da operação e ficou OCUPADO, então a
        // flag RecebidoOP fica em TRUE
        if (finishOPEst == true & recebidoOpEst == false) {
            //eventos.add("Seq " + seq++ + ": finishOPEst == true & recebidoOpEst == false");
            if (SmartService.readOnly == false) {
                //System.out.println("Flag: RecebidoOPEstoque_TRUE");
                try {
                    //System.out.println("FinishOP: colocando RecebidoOPEstoque em TRUE");
                    //eventos.add("Seq " + seq++ + ": coloca RecebidoOPEst em TRUE");
                    plcConnectorEst.writeBit(9, 0, 0, Boolean.parseBoolean("TRUE")); // coloca RecebidoOPEst em TRUE

                } catch (Exception e) {
                    System.out.println(
                            "ERRO [finishOp]: Atualização da Flag RecebidoOPEstoque [DB9:0.0] para TRUE");
                }
                if (SmartService.statusProducao == 0 & SmartService.pedidoEmCurso == true) {
                    SmartService.statusEstoque = 2;
                } else {
                    //statusEstoque = 0;
                }
            }
        }

        // Se as flags de remover ou adicionar no estoque estão em FALSE então a flag RecebidoEstoque fica em FALSE
        if (removerEstoque == false & adicionarEstoque == false) {
            //eventos.add("Seq " + seq++ + ": removerEstoque == false & adicionarEstoque == false");
            if (SmartService.readOnly == false) {

                //System.out.println("colocando RecebidoEstoque em FALSEe");
                try {
                    //eventos.add("Seq " + seq++ + ": coloca RecebidoEstoquet em FALSE");
                    plcConnectorEst.writeBit(9, 64, 0, Boolean.parseBoolean("FALSE")); // coloca RecebidoEstoque em FALSE

                } catch (Exception e) {
                    System.out.println("ERRO: Atualização da Flag RecebidoEstoque [DB9:64.0] para FALSE");
                }
            }
        }

        // Atualiza a posição removida na tabela Estoque e na memória do clp Estoque
        if ((posicaoEstoque > 0) && removerEstoque == true) {
            if (!SmartService.readOnly) {
                try {
                    plcConnectorEst.writeBit(9, 64, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO: Atualização da Flag RecebidoEstoque [DB9:64.0] para TRUE");
                }

                byte offset = (byte) (68 + (posicaoEstoque - 1));

                try {
                    // Atualiza cor no CLP
                    System.out.println("\n\nREMOVENDO ESTOQUE NA POSIÇÃO: " + offset + " COR: " + corGuardarEstoque + "\n\n");
                    plcConnectorEst.writeByte(9, offset, (byte) 0);

                    // Cria mapa de dados com apenas uma posição
                    Map<String, Integer> dadosMap = new HashMap<>();
                    dadosMap.put("posicao:" + posicaoEstoque, 0);

                    // === Chama serviço de integração ===
                    boolean sucesso = apiIntegrationService.salvarEstoque(dadosMap);

                    if (sucesso) {
                        System.out.println("Estoque salvo com sucesso na API.");
                    } else {
                        System.out.println("Falha ao salvar estoque na API.");
                        // aqui você poderia lançar uma exceção ou marcar para tentar novamente
                    }

                } catch (Exception e) {
                    System.out.println("ERRO: Na tentativa de remover do Estoque");
                    e.printStackTrace();
                }
            }
        }

        // Atualiza na posição a cor do bloco adicionado na tabela Estoque e na memória do clp Estoque
        if ((posicaoEstoque > 0) && adicionarEstoque == true) {
            //eventos.add("Seq " + seq++ + ": (posicaoEstoque > 0) & adicionarEstoque == true");
            if (SmartService.readOnly == false) {

                //System.out.println("Flag: RecebidoEstoque_TRUE - ADICIONAR ESTOQUE");
                // Coloca a flag RecebidoEstoque em TRUE
                try {
                    //eventos.add("Seq " + seq++ + ": coloca RecebidoEstoque em TRUE");
                    plcConnectorEst.writeBit(9, 64, 0, Boolean.parseBoolean("TRUE")); // coloca RecebidoEstoque em TRUE
                } catch (Exception e) {
                    System.out.println("ERRO: Atualização da Flag RecebidoEstoque [DB9:64.0] para TRUE");
                }

                //eventos.add("Seq " + seq++ + ": Adicionando bloco na posição: " + posicaoEstoque);
                byte offset = (byte) (68 + (posicaoEstoque - 1));

                try {
                    // Atualiza cor no CLP
                    plcConnectorEst.writeByte(9, offset, (byte) corGuardarEstoque);

                    // Cria mapa de dados com apenas uma posição
                    Map<String, Integer> dadosMap = new HashMap<>();
                    dadosMap.put("posicao:" + posicaoEstoque, corGuardarEstoque);

                    // === Chama serviço de integração ===
                    boolean sucesso = apiIntegrationService.salvarEstoque(dadosMap);

                    if (sucesso) {
                        System.out.println("Estoque salvo com sucesso na API.");
                    } else {
                        System.out.println("Falha ao salvar estoque na API.");
                        // aqui você poderia lançar uma exceção ou marcar para tentar novamente
                    }

                } catch (Exception e) {
                    System.out.println("ERRO: Na tentativa de adicionar no Estoque");
                    e.printStackTrace();
                }
            }
        }

        //--------------------------------------------------------------------------------------------------------------------------------------
        // Se as flags ocupadoEst ou retornoEstoqueCheio estão em TRUE E a flag iniciarGuardarEst foi ativada então a flag iniciarGuardarEst fica em FALSE
        if ((ocupadoEst == true | retornoEstoqueCheio == true) & iniciarGuardarEst == true) {
            //eventos.add("Seq " + seq++ + ": (ocupadoEst == true | retornoEstoqueCheio == true) & iniciarGuardarEst == true");
            if (SmartService.readOnly == false) {
                //System.out.println("Flag: IniciarGuardar_FALSE");

                // Coloca a flag IniciarGuardar em FALSE
                try {
                    //eventos.add("Seq " + seq++ + ": Coloca iniciarGuardarEst em FALSE");
                    plcConnectorEst.writeBit(9, 64, 1, Boolean.parseBoolean("FALSE")); // Coloca iniciarGuardarEst em FALSE

                } catch (Exception e) {
                    System.out.println(
                            "ERRO: Atualização da Flag IniciarGuardarEstoque [DB9:64.1] para FALSE");
                }
            }

        }

        // Verificar se a estação Estoque está livre E pede posição para guardar
        // Aqui deve ser implementada/chamada a função que verifica qual a situação de ocupação de cada
        // posição do Magazine de Estoque
        if (pedirPosicaoEst == true & ocupadoEst == false) {
            //System.out.println("ESTOU AQUI- (pedirPosicaoEst == true) & ocupadoEst == false");
            //eventos.add("Seq " + seq++ + ": pedirPosicaoEst == true & ocupadoEst == false");
            // Rotina para verificar qual posição está disponível para guardar
            if (!SmartService.readOnly) {
                // Solicita posição disponível para guardar (0-LIVRE 1-PRETO 2-VERMELHO 3-AZUL)
                // Certifique-se de que posEstoqueLivre é seguro para acesso
                Set<Integer> posicoesUsadas = new HashSet<>(); // Para evitar duplicidade

                int posEstoqueLivre = buscarPrimeiraPosicaoPorCor(0, posicoesUsadas) /*getPositionEstoque(0)*/;
                // System.out.println("Posição disponível no Magazine Estoque: " + posEstoqueRequest);
                if (posEstoqueLivre > 0) {

                    try {
                        // Atualiza a variável PosicaoGuardar no CLP ESTOQUE
                        //eventos.add("Seq " + seq++ + ": Atualiza a variável PosicaoGuardar no CLP ESTOQUE");
                        plcConnectorEst.writeInt(9, 66, posEstoqueLivre);
                    } catch (Exception e) {
                        System.out.println("ERRO: Atualização da PosicaoGuardarEstoque [DB9:66]");
                    }

                    try {
                        // Coloca a flag IniciarGuardar em TRUE
                        //eventos.add("Seq " + seq++ + ": Coloca a flag IniciarGuardar em TRUE");
                        plcConnectorEst.writeBit(9, 64, 1, Boolean.parseBoolean("TRUE"));  // coloca IniciarGuardar em TRUE
                    } catch (Exception e) {
                        System.out.println("ERRO: Atualização da Flag IniciarGuardarEstoque [DB9:64.1]");
                    }
                } else {
                    System.out.println("ERRO: Nao existe posição livre.");
                }
            }
        }

    }
}