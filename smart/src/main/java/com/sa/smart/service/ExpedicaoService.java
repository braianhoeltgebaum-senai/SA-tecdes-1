package com.sa.smart.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.smart.clpcomm.PlcConnectionService;
import com.sa.smart.clpcomm.PlcConnector;
import com.sa.smart.dto.ExpedicaoDTO;
import com.sa.smart.model.Expedicao;
import com.sa.smart.model.Pedido;
import com.sa.smart.repository.ExpedicaoRepository;
import com.sa.smart.repository.PedidoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

@Service
public class ExpedicaoService {

    //********************** Expedição *************************
    //----------------------- NodeToPlc ------------------------
    boolean recebidoOpExp            = false;
    boolean recebidoExpedicao        = false;
    boolean iniciarGuardarExp        = false;
    int     posicaoGuardarExp        = 0;
    int[]   orderExpedicao           = new int[12];

    //----------------------- PlcToNode ------------------------
    int     numeroOPExp              = 0;
    boolean cancelOPExp              = false;
    boolean finishOPExp              = false;
    boolean startOPExp               = false;

    boolean ocupadoExp               = false;
    boolean aguardandoExp            = false;
    boolean manualExp                = false;
    boolean emergenciaExp            = false;

    boolean pedirPosicaoExp          = false;
    int     posicaoGuardadoExpedicao = 0;
    int     posicaoRemovidoExpedicao = 0;
    boolean adicionarExpedicao       = false;
    boolean removerExpedicao         = false;
    int     opGuardadoExpedicao      = 0;

    // ─── Dependências ─────────────────────────────────────────────────────────
    private final ExpedicaoRepository  expedicaoRepository;
    private final PedidoRepository     pedidoRepository;
    private final EntityManager        entityManager;
    private final PlcConnectionService plcConnectionService;
    private final ApiIntegrationService apiIntegrationService;
    private final RastreamentoService  rastreamentoService;

    public ExpedicaoService(
            ExpedicaoRepository  expedicaoRepository,
            PedidoRepository     pedidoRepository,
            EntityManager        entityManager,
            PlcConnectionService plcConnectionService,
            ApiIntegrationService apiIntegrationService,
            RastreamentoService  rastreamentoService) {

        this.expedicaoRepository  = expedicaoRepository;
        this.pedidoRepository     = pedidoRepository;
        this.entityManager        = entityManager;
        this.plcConnectionService = plcConnectionService;
        this.apiIntegrationService = apiIntegrationService;
        this.rastreamentoService  = rastreamentoService;
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public ExpedicaoDTO criar(ExpedicaoDTO dto) {
        Pedido pedido = buscarPedidoPorOrdemProducao(dto.ordemProducao());

        Expedicao e = new Expedicao();
        e.setPosicaoExpedicao(dto.posicaoExpedicao());
        e.setEntradaEm(dto.entradaEm());
        e.setSaidaEm(dto.saidaEm());
        e.setPedido(pedido);

        Expedicao saved = expedicaoRepository.save(e);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpedicaoDTO> listar() {
        return expedicaoRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpedicaoDTO buscar(Long id) {
        return toDTO(expedicaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expedição não encontrada")));
    }

    @Transactional
    public ExpedicaoDTO put(Long id, ExpedicaoDTO dto) {
        Expedicao e = expedicaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expedição não encontrada"));

        e.setPosicaoExpedicao(dto.posicaoExpedicao());
        e.setEntradaEm(dto.entradaEm());
        e.setSaidaEm(dto.saidaEm());
        e.setPedido(buscarPedidoPorOrdemProducao(dto.ordemProducao()));

        return toDTO(expedicaoRepository.save(e));
    }

    @Transactional
    public ExpedicaoDTO patch(Long id, ExpedicaoDTO dto) {
        Expedicao e = expedicaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expedição não encontrada"));

        if (dto.posicaoExpedicao() != null) e.setPosicaoExpedicao(dto.posicaoExpedicao());
        if (dto.entradaEm()        != null) e.setEntradaEm(dto.entradaEm());
        if (dto.saidaEm()          != null) e.setSaidaEm(dto.saidaEm());
        if (dto.ordemProducao()    != null) e.setPedido(buscarPedidoPorOrdemProducao(dto.ordemProducao()));

        return toDTO(expedicaoRepository.save(e));
    }

    @Transactional
    public void deletar(Long id) {
        if (!expedicaoRepository.existsById(id)) {
            throw new EntityNotFoundException("Expedição não encontrada");
        }
        expedicaoRepository.deleteById(id);
    }

    public int buscarPrimeiraPosicaoLivreExp() {
        List<Integer> ocupadas = expedicaoRepository.findAllPosicoesOcupadas();
        for (int i = 1; i <= 12; i++) {
            if (!ocupadas.contains(i)) return i;
        }
        return -1;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ExpedicaoDTO toDTO(Expedicao e) {
        return new ExpedicaoDTO(
                e.getId(),
                e.getPosicaoExpedicao(),
                e.getEntradaEm(),
                e.getSaidaEm(),
                e.getPedido().getOrdemProducao());
    }

    private Pedido buscarPedidoPorOrdemProducao(String ordemProducao) {
        return entityManager
                .createQuery("SELECT p FROM Pedido p WHERE p.ordemProducao = :op", Pedido.class)
                .setParameter("op", ordemProducao)
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Pedido não encontrado com ordemProducao: " + ordemProducao));
    }

    // ─── Processamento de dados do CLP ────────────────────────────────────────

    public void processData(String ip, byte[] dadosClp4) {

        PlcConnector plcConnectorExp = plcConnectionService.getConnection(ip);
        if (plcConnectorExp == null) return;

        // Leitura das variáveis
        recebidoOpExp            = (dadosClp4[0]  & 0x01) != 0;
        recebidoExpedicao        = (dadosClp4[2]  & 0x01) != 0;
        iniciarGuardarExp        = (dadosClp4[2]  & 0x02) != 0;
        posicaoGuardarExp        = ((dadosClp4[4] & 0xFF) << 8) | (dadosClp4[5] & 0xFF);

        int x = 0;
        for (int c = 0; c < 24; c += 2) {
            orderExpedicao[x++] = ((dadosClp4[c + 6] & 0xFF) << 8) | (dadosClp4[c + 7] & 0xFF);
        }

        numeroOPExp  = ((dadosClp4[30] & 0xFF) << 8) | (dadosClp4[31] & 0xFF);
        cancelOPExp  =  (dadosClp4[32] & 0x01) != 0;
        finishOPExp  =  (dadosClp4[32] & 0x02) != 0;
        startOPExp   =  (dadosClp4[32] & 0x04) != 0;
        ocupadoExp   =  (dadosClp4[34] & 0x01) != 0;
        aguardandoExp=  (dadosClp4[34] & 0x02) != 0;
        manualExp    =  (dadosClp4[34] & 0x04) != 0;
        emergenciaExp=  (dadosClp4[34] & 0x08) != 0;

        pedirPosicaoExp          =  (dadosClp4[36] & 0x01) != 0;
        posicaoGuardadoExpedicao = ((dadosClp4[38] & 0xFF) << 8) | (dadosClp4[39] & 0xFF);
        posicaoRemovidoExpedicao = ((dadosClp4[40] & 0xFF) << 8) | (dadosClp4[41] & 0xFF);
        adicionarExpedicao       =  (dadosClp4[42] & 0x01) != 0;
        removerExpedicao         =  (dadosClp4[42] & 0x02) != 0;
        opGuardadoExpedicao      = ((dadosClp4[44] & 0xFF) << 8) | (dadosClp4[45] & 0xFF);

        // ==================================================================
        // DIAGNÓSTICO — agora inclui posicaoExpedicaoConfirmada, o valor
        // estável usado na checagem de conclusão (em vez do posicaoGuardarExp
        // ao vivo, que o CLP zera assim que o handshake termina).
        // ==================================================================
        System.out.println("[DIAG Expedicao] "
                + "startOPExp=" + startOPExp
                + " finishOPExp=" + finishOPExp
                + " cancelOPExp=" + cancelOPExp
                + " recebidoOpExp=" + recebidoOpExp
                + " | ocupadoExp=" + ocupadoExp
                + " aguardandoExp=" + aguardandoExp
                + " | pedirPosicaoExp=" + pedirPosicaoExp
                + " aux_expedicao=" + SmartService.aux_expedicao
                + " | adicionarExpedicao=" + adicionarExpedicao
                + " removerExpedicao=" + removerExpedicao
                + " recebidoExpedicao=" + recebidoExpedicao
                + " | posicaoGuardarExp(live)=" + posicaoGuardarExp
                + " posicaoExpedicaoConfirmada(travada)=" + SmartService.posicaoExpedicaoConfirmada
                + " posicaoGuardadoExpedicao=" + posicaoGuardadoExpedicao
                + " posicaoExpedicaoSolicitada=" + SmartService.posicaoExpedicaoSolicitada
                + " | opGuardadoExpedicao=" + opGuardadoExpedicao
                + " | pedidoEmCurso=" + SmartService.pedidoEmCurso
                + " statusProducao=" + SmartService.statusProducao);

        // Repassa para o rastreamento — usa a posição CONFIRMADA (travada no
        // momento do handshake), não o campo ao vivo do CLP.
        rastreamentoService.processarExpedicao(
                numeroOPExp, startOPExp, finishOPExp,
                SmartService.posicaoExpedicaoConfirmada, ocupadoExp,
                posicaoGuardadoExpedicao, opGuardadoExpedicao);

        // StartOP/FinishOP/CancelOP todos FALSE → RecebidoOPExp = FALSE
        if (!startOPExp && !finishOPExp && !cancelOPExp) {
            if (!SmartService.readOnly) {
                try {
                    plcConnectorExp.writeBit(9, 0, 0, false);
                } catch (Exception e) {
                    System.out.println("ERRO [startOp/finishOp]: Flag RecebidoOPExp [DB9:0.0] FALSE");
                }
            }
        }

        // StartOP TRUE e RecebidoOP FALSE → RecebidoOPExp = TRUE
        if (startOPExp && !recebidoOpExp) {
            if (SmartService.statusProducao == 0 && SmartService.pedidoEmCurso) {
                SmartService.statusExpedicao = 1;
            }
            if (!SmartService.readOnly) {
                try {
                    plcConnectorExp.writeBit(9, 0, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO [startOp]: Flag RecebidoOPExp [DB9:0.0] TRUE");
                }
            }
        }

        // FinishOP TRUE e RecebidoOP FALSE → RecebidoOPExp = TRUE
        if (finishOPExp && !recebidoOpExp) {
            if (!SmartService.readOnly) {
                try {
                    plcConnectorExp.writeBit(9, 0, 0, true);
                    SmartService.blockFinished = true;
                } catch (Exception e) {
                    System.out.println("ERRO [finishOp]: Flag RecebidoOPExp [DB9:0.0] TRUE");
                }
                if (SmartService.statusProducao == 0 && SmartService.pedidoEmCurso) {
                    SmartService.statusExpedicao = 2;
                }
            }
        }

        // pedirPosicaoExp FALSE → reseta aux_expedicao e IniciarGuardar = FALSE
        if (!pedirPosicaoExp) {
            if (!SmartService.readOnly) {
                SmartService.aux_expedicao = false;
                try {
                    plcConnectorExp.writeBit(9, 2, 1, false);
                } catch (Exception e) {
                    System.out.println("ERRO [Pedir Posição]: Flag IniciarGuardar [DB9:2.1] FALSE");
                }
            }
        }

        // pedirPosicaoExp TRUE e aux_expedicao FALSE → fornece posição para guardar
        if (pedirPosicaoExp && !SmartService.aux_expedicao) {
            SmartService.aux_expedicao = true;
            if (!SmartService.readOnly) {
                try {
                    plcConnectorExp.writeInt(9, 4, SmartService.posicaoExpedicaoSolicitada);
                } catch (Exception e) {
                    System.out.println("ERRO: PosicaoGuardarExpedicao [DB9:4]");
                }
                try {
                    plcConnectorExp.writeBit(9, 2, 1, true);
                } catch (Exception e) {
                    System.out.println("ERRO [Pedir Posição]: Flag IniciarGuardar [DB9:2.1] TRUE");
                }
            }
        }

        // adicionarExpedicao ou removerExpedicao FALSE → RecebidoExpedicao = FALSE
        if (!SmartService.readOnly && (!adicionarExpedicao || !removerExpedicao)) {
            try {
                plcConnectorExp.writeBit(9, 2, 0, false);
            } catch (Exception e) {
                System.out.println("ERRO: Flag RecebidoExpedicao [DB9:2.0] FALSE");
            }
        }

        // adicionarExpedicao TRUE e aux_expedicao FALSE → grava OP na posição
        if (adicionarExpedicao && !SmartService.aux_expedicao) {
            SmartService.aux_expedicao = true;
            if (!SmartService.readOnly) {
                try {
                    plcConnectorExp.writeBit(9, 2, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO [Adicionar]: Flag RecebidoExpedicao [DB9:2.0] TRUE");
                }
                System.out.println("Guardando OP em posicaoGuardarExp: " + posicaoGuardarExp);
                if (posicaoGuardarExp > 0) {

                    // ==========================================================
                    // CORREÇÃO: captura a posição AQUI, enquanto o campo do CLP
                    // (posicaoGuardarExp) ainda está válido — é o único momento
                    // em que ele reflete a posição real sendo gravada. O CLP
                    // zera esse campo assim que o handshake termina, então
                    // sem essa captura a checagem de conclusão (mais abaixo,
                    // e também no RastreamentoService) nunca bateria contra
                    // posicaoGuardadoExpedicao quando finishOPExp finalmente
                    // chegasse true.
                    // ==========================================================
                    SmartService.posicaoExpedicaoConfirmada = posicaoGuardarExp;

                    int offset = 6 + (posicaoGuardarExp - 1) * 2;
                    try {
                        plcConnectorExp.writeInt(9, offset, opGuardadoExpedicao);

                        Map<String, Integer> dadosMap = new HashMap<>();
                        dadosMap.put("OP:" + posicaoGuardarExp, opGuardadoExpedicao);

                        boolean sucesso = apiIntegrationService.salvarExpedicao(dadosMap);
                        System.out.println(sucesso
                                ? "Expedição adicionada com sucesso na API."
                                : "Falha ao adicionar expedição na API.");
                    } catch (Exception e) {
                        System.out.println("ERRO: Na tentativa de adicionar na Expedição");
                        e.printStackTrace();
                    }
                }
            }
        }

        // removerExpedicao TRUE e aux_expedicao FALSE → remove OP da posição
        if (removerExpedicao && !SmartService.aux_expedicao) {
            SmartService.aux_expedicao = true;
            if (!SmartService.readOnly) {
                try {
                    plcConnectorExp.writeBit(9, 2, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO [Remover]: Flag RecebidoExpedicao [DB9:2.0] TRUE");
                }
                System.out.println("Removendo OP de posicaoRemovidoExpedicao: " + posicaoRemovidoExpedicao);
                if (posicaoRemovidoExpedicao > 0) {
                    int offset = 6 + (posicaoRemovidoExpedicao - 1) * 2;
                    try {
                        plcConnectorExp.writeInt(9, offset, 0);

                        Map<String, Integer> dadosMap = new HashMap<>();
                        dadosMap.put("OP:" + posicaoRemovidoExpedicao, 0);

                        boolean sucesso = apiIntegrationService.salvarExpedicao(dadosMap);
                        System.out.println(sucesso
                                ? "Expedição removida com sucesso na API."
                                : "Falha ao remover expedição na API.");
                    } catch (Exception e) {
                        System.out.println("ERRO: Na tentativa de remover da Expedição");
                        e.printStackTrace();
                    }
                }
            }
        }

        // Pedido concluído pela expedição — agora compara contra o valor
        // CONFIRMADO (travado), não contra o campo ao vivo do CLP.
        if ((posicaoGuardadoExpedicao == SmartService.posicaoExpedicaoConfirmada)
                && !ocupadoExp && finishOPExp) {
            if (!SmartService.readOnly) {
                System.out.println("statusProducao: " + SmartService.statusProducao);
                System.out.println("pedidoEmCurso: "  + SmartService.pedidoEmCurso);
                if (SmartService.statusProducao == 0 && SmartService.pedidoEmCurso) {
                    SmartService.statusProducao = 1;
                }
                System.out.println("Operação OP:" + opGuardadoExpedicao + " Finalizada.");
            }
        }
    }
}