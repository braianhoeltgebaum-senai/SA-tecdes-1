package com.sa.smart.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sa.smart.clpcomm.PlcConnectionService;
import com.sa.smart.clpcomm.PlcConnector;
import com.sa.smart.dto.EstoqueDTO;
import com.sa.smart.model.Estoque;
import com.sa.smart.repository.EstoqueRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class EstoqueService {

    private final EstoqueRepository repository;
    private final PlcConnectionService plcConnectionService;
    private final ApiIntegrationService apiIntegrationService;

    public EstoqueService(
            EstoqueRepository repository,
            PlcConnectionService plcConnectionService,
            ApiIntegrationService apiIntegrationService) {

        this.repository = repository;
        this.plcConnectionService = plcConnectionService;
        this.apiIntegrationService = apiIntegrationService;
    }

    // ─── Campos de estado lidos do CLP ────────────────────────────────────────
    //  NodeToPlc
    boolean recebidoOpEst       = false;
    //  PlcToNode — OP
    int     numeroOPEst         = 0;
    boolean cancelOPEst         = false;
    boolean finishOPEst         = false;
    boolean startOPEst          = false;
    //  PlcToNode — estação
    boolean ocupadoEst          = false;
    boolean aguardandoEst       = false;
    boolean manualEst           = false;
    boolean emergenciaEst       = false;
    //  PlcToNode — controle do magazine
    boolean iniciarPedido       = false;
    boolean recebidoEstoque     = false;
    boolean iniciarGuardarEst   = false;
    int     posicaoGuardarEst   = 0;
    byte[]  posicoesOcupadas    = new byte[28];
    boolean pedirPosicaoEst     = false;
    int     posicaoEstoque      = 0;
    boolean adicionarEstoque    = false;
    boolean removerEstoque      = false;
    boolean retornoEstoqueCheio = false;
    int     corGuardarEstoque   = 0;

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    public EstoqueDTO criar(EstoqueDTO dto) {
        validarCor(dto.cor());

        Estoque e = new Estoque();
        e.setPosicaoEstoque(dto.posicaoEstoque());
        e.setCor(dto.cor());

        Estoque saved = repository.save(e);
        return toDTO(saved);
    }

    public List<EstoqueDTO> listar() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public EstoqueDTO buscar(Long id) {
        return toDTO(repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estoque não encontrado")));
    }

    public EstoqueDTO put(Long id, EstoqueDTO dto) {
        validarCor(dto.cor());

        Estoque e = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estoque não encontrado"));

        e.setPosicaoEstoque(dto.posicaoEstoque());
        e.setCor(dto.cor());

        return toDTO(repository.save(e));
    }

    public EstoqueDTO patch(Long id, EstoqueDTO dto) {
        Estoque e = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estoque não encontrado"));

        if (dto.posicaoEstoque() != null) {
            e.setPosicaoEstoque(dto.posicaoEstoque());
        }
        if (dto.cor() != null) {
            validarCor(dto.cor());
            e.setCor(dto.cor());
        }

        return toDTO(repository.save(e));
    }

    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Estoque não encontrado");
        }
        repository.deleteById(id);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private EstoqueDTO toDTO(Estoque e) {
        return new EstoqueDTO(e.getId(), e.getPosicaoEstoque(), e.getCor());
    }

    private void validarCor(Integer cor) {
        if (cor == null || cor < 0 || cor > 3) {
            throw new IllegalArgumentException("Cor inválida. Use: 0=vazio, 1=preto, 2=vermelho, 3=azul");
        }
    }

    /** Busca a primeira posição de estoque com a cor informada que ainda não foi usada. */
    public int buscarPrimeiraPosicaoPorCor(int cor, Set<Integer> usadas) {
        return repository.findByCorOrderByPosicaoEstoqueAsc(cor)
                .stream()
                .filter(e -> !usadas.contains(e.getPosicaoEstoque()))
                .map(Estoque::getPosicaoEstoque)
                .findFirst()
                .orElse(-1);
    }

    // ─── Processamento de dados do CLP ────────────────────────────────────────

    public void processData(String ip, byte[] dadosClp1) {

        PlcConnector plcConnectorEst = plcConnectionService.getConnection(ip);
        if (plcConnectorEst == null) {
            return;
        }

        // Leitura das variáveis
        recebidoOpEst       = (dadosClp1[0]   & 0x01) != 0;
        iniciarPedido       = (dadosClp1[62]  & 0x01) != 0;
        recebidoEstoque     = (dadosClp1[64]  & 0x01) != 0;
        iniciarGuardarEst   = (dadosClp1[64]  & 0x02) != 0;
        posicaoGuardarEst   = ((dadosClp1[66] & 0xFF) << 8) | (dadosClp1[67] & 0xFF);

        for (int c = 0; c < 28; c++) {
            posicoesOcupadas[c] = dadosClp1[68 + c];
        }

        numeroOPEst   = ((dadosClp1[96]  & 0xFF) << 8) | (dadosClp1[97]  & 0xFF);
        cancelOPEst   =  (dadosClp1[98]  & 0x01) != 0;
        finishOPEst   =  (dadosClp1[98]  & 0x02) != 0;
        startOPEst    =  (dadosClp1[98]  & 0x04) != 0;
        ocupadoEst    =  (dadosClp1[100] & 0x01) != 0;
        aguardandoEst =  (dadosClp1[100] & 0x02) != 0;
        manualEst     =  (dadosClp1[100] & 0x04) != 0;
        emergenciaEst =  (dadosClp1[100] & 0x08) != 0;

        pedirPosicaoEst     =  (dadosClp1[102] & 0x01) != 0;
        posicaoEstoque      = ((dadosClp1[104] & 0xFF) << 8) | (dadosClp1[105] & 0xFF);
        adicionarEstoque    =  (dadosClp1[106] & 0x01) != 0;
        // CORRIGIDO: cast (boolean) removido — era inválido em Java
        removerEstoque      =  (dadosClp1[106] & 0x02) != 0;
        retornoEstoqueCheio =  (dadosClp1[106] & 0x04) != 0;
        corGuardarEstoque   = ((dadosClp1[108] & 0xFF) << 8) | (dadosClp1[109] & 0xFF);

        // Se iniciarPedido == true e a estação ficou OCUPADA, reseta a flag
        if (iniciarPedido && ocupadoEst) {
            SmartService.pedidoEmCurso = true;
            SmartService.statusEstoque = 0;
            SmartService.statusProducao = 0;
            if (!SmartService.readOnly) {
                try {
                    plcConnectorEst.writeBit(9, 62, 0, false);
                } catch (Exception e) {
                    System.out.println("ERRO [iniciarPedido]: Flag IniciarPedido [DB9:62.0] para FALSE");
                }
            }
        }

        // StartOP/FinishOP/CancelOP todos FALSE → RecebidoOPEst = FALSE
        if (!startOPEst && !finishOPEst && !cancelOPEst) {
            if (!SmartService.readOnly) {
                try {
                    plcConnectorEst.writeBit(9, 0, 0, false);
                } catch (Exception e) {
                    System.out.println("ERRO: Flag RecebidoOPEstoque [DB9:0.0] para FALSE");
                }
            }
        }

        // StartOP TRUE e RecebidoOP FALSE → RecebidoOPEst = TRUE
        if (startOPEst && !recebidoOpEst) {
            if (SmartService.statusProducao == 0 && SmartService.pedidoEmCurso) {
                SmartService.statusEstoque = 1;
            }
            if (!SmartService.readOnly) {
                try {
                    plcConnectorEst.writeBit(9, 0, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO [startOp]: Flag RecebidoOPEstoque [DB9:0.0] para TRUE");
                }
            }
        }

        // FinishOP TRUE e RecebidoOP FALSE → RecebidoOPEst = TRUE
        if (finishOPEst && !recebidoOpEst) {
            if (!SmartService.readOnly) {
                try {
                    plcConnectorEst.writeBit(9, 0, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO [finishOp]: Flag RecebidoOPEstoque [DB9:0.0] para TRUE");
                }
                if (SmartService.statusProducao == 0 && SmartService.pedidoEmCurso) {
                    SmartService.statusEstoque = 2;
                }
            }
        }

        // removerEstoque == false e adicionarEstoque == false → RecebidoEstoque = FALSE
        if (!removerEstoque && !adicionarEstoque) {
            if (!SmartService.readOnly) {
                try {
                    plcConnectorEst.writeBit(9, 64, 0, false);
                } catch (Exception e) {
                    System.out.println("ERRO: Flag RecebidoEstoque [DB9:64.0] para FALSE");
                }
            }
        }

        // Remover bloco do estoque
        if (posicaoEstoque > 0 && removerEstoque) {
            if (!SmartService.readOnly) {
                try {
                    plcConnectorEst.writeBit(9, 64, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO: Flag RecebidoEstoque [DB9:64.0] para TRUE");
                }
                byte offset = (byte) (68 + (posicaoEstoque - 1));
                try {
                    plcConnectorEst.writeByte(9, offset, (byte) 0);

                    Map<String, Integer> dadosMap = new HashMap<>();
                    dadosMap.put("posicao:" + posicaoEstoque, 0);

                    boolean sucesso = apiIntegrationService.salvarEstoque(dadosMap);
                    System.out.println(sucesso
                            ? "Estoque removido com sucesso na API."
                            : "Falha ao remover estoque na API.");
                } catch (Exception e) {
                    System.out.println("ERRO: Na tentativa de remover do Estoque");
                    e.printStackTrace();
                }
            }
        }

        // Adicionar bloco ao estoque
        if (posicaoEstoque > 0 && adicionarEstoque) {
            if (!SmartService.readOnly) {
                try {
                    plcConnectorEst.writeBit(9, 64, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO: Flag RecebidoEstoque [DB9:64.0] para TRUE");
                }
                byte offset = (byte) (68 + (posicaoEstoque - 1));
                try {
                    plcConnectorEst.writeByte(9, offset, (byte) corGuardarEstoque);

                    Map<String, Integer> dadosMap = new HashMap<>();
                    dadosMap.put("posicao:" + posicaoEstoque, corGuardarEstoque);

                    boolean sucesso = apiIntegrationService.salvarEstoque(dadosMap);
                    System.out.println(sucesso
                            ? "Estoque adicionado com sucesso na API."
                            : "Falha ao adicionar estoque na API.");
                } catch (Exception e) {
                    System.out.println("ERRO: Na tentativa de adicionar no Estoque");
                    e.printStackTrace();
                }
            }
        }

        // ocupadoEst ou retornoEstoqueCheio TRUE e iniciarGuardarEst TRUE → IniciarGuardar = FALSE
        if ((ocupadoEst || retornoEstoqueCheio) && iniciarGuardarEst) {
            if (!SmartService.readOnly) {
                try {
                    plcConnectorEst.writeBit(9, 64, 1, false);
                } catch (Exception e) {
                    System.out.println("ERRO: Flag IniciarGuardarEstoque [DB9:64.1] para FALSE");
                }
            }
        }

        // pedirPosicaoEst == true e estação livre → fornece posição para guardar
        if (pedirPosicaoEst && !ocupadoEst) {
            if (!SmartService.readOnly) {
                Set<Integer> posicoesUsadasSet = new HashSet<>();
                int posEstoqueLivre = buscarPrimeiraPosicaoPorCor(0, posicoesUsadasSet);
                if (posEstoqueLivre > 0) {
                    try {
                        plcConnectorEst.writeInt(9, 66, posEstoqueLivre);
                    } catch (Exception e) {
                        System.out.println("ERRO: Atualização da PosicaoGuardarEstoque [DB9:66]");
                    }
                    try {
                        plcConnectorEst.writeBit(9, 64, 1, true);
                    } catch (Exception e) {
                        System.out.println("ERRO: Flag IniciarGuardarEstoque [DB9:64.1] para TRUE");
                    }
                } else {
                    System.out.println("ERRO: Não existe posição livre no estoque.");
                }
            }
        }
    }
}