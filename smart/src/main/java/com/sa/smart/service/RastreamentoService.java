package com.sa.smart.service;

import org.springframework.stereotype.Service;

/**
 * Classe única e isolada responsável por interpretar os bytes já lidos
 * pelos services de cada bancada (Estoque, Processo, Montagem, Expedição)
 * e manter o estado global: em qual bancada está o pedido e qual seu status.
 *
 * NÃO abre conexões próprias com o CLP (evita concorrência no mesmo socket).
 * Lê pedidoEmCurso/statusProducao de SmartService, e agora também é
 * responsável por RESETAR essas flags quando o ciclo do pedido é concluído
 * (ver processarExpedicao), liberando o próximo pedido a ser rastreado.
 * NÃO é chamada pelo ClpController — é chamada pelos próprios services de
 * bancada, que apenas repassam os valores que já leram.
 */
@Service
public class RastreamentoService {

    // ─── Estado isolado (não mexe em SmartService, exceto no reset de ciclo) ──
    public static volatile String bancadaAtual  = "Nenhuma";
    public static volatile int    numeroOpAtual = 0;
    public static volatile byte   statusEstoqueRastreio   = 0;
    public static volatile byte   statusProcessoRastreio  = 0;
    public static volatile byte   statusMontagemRastreio  = 0;
    public static volatile byte   statusExpedicaoRastreio = 0;

    private final PedidoService pedidoService;

    public RastreamentoService(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // ─── Estoque ────────────────────────────────────────────────────────────
    public void processarEstoque(byte[] dados, int numeroOP, boolean start, boolean finish) {
        atualizar("estoque", start, finish, numeroOP);
        if (start)  statusEstoqueRastreio = 1;
        if (finish) statusEstoqueRastreio = 2;
    }

    // ─── Processo ────────────────────────────────────────────────────────────
    public void processarProcesso(int numeroOP, boolean start, boolean finish) {
        atualizar("processo", start, finish, numeroOP);
        if (start)  statusProcessoRastreio = 1;
        if (finish) statusProcessoRastreio = 2;
    }

    // ─── Montagem ────────────────────────────────────────────────────────────
    public void processarMontagem(int numeroOP, boolean start, boolean finish) {
        atualizar("montagem", start, finish, numeroOP);
        if (start)  statusMontagemRastreio = 1;
        if (finish) statusMontagemRastreio = 2;
    }

    // ─── Expedição (com detecção de conclusão real do pedido) ───────────────
    public void processarExpedicao(int numeroOP, boolean start, boolean finish,
                                    int posicaoGuardarExp, boolean ocupadoExp,
                                    int posicaoGuardadoExpedicao, int opGuardadoExpedicao) {

        atualizar("expedicao", start, finish, numeroOP);
        if (start)  statusExpedicaoRastreio = 1;
        if (finish) statusExpedicaoRastreio = 2;

        boolean cicloConcluido = (posicaoGuardadoExpedicao == posicaoGuardarExp)
                && !ocupadoExp
                && finish;

        if (cicloConcluido && SmartService.statusProducao == 0 && SmartService.pedidoEmCurso) {
            numeroOpAtual = opGuardadoExpedicao;
            bancadaAtual  = "expedicao";
            statusExpedicaoRastreio = 2;

            System.out.println("[RastreamentoService] Pedido OP:" + opGuardadoExpedicao + " finalizado.");

            try {
                pedidoService.atualizarStatusParaConcluido((long) opGuardadoExpedicao);
                System.out.println("[RastreamentoService] Pedido #" + opGuardadoExpedicao
                        + " marcado como Concluído no banco.");
            } catch (Exception ex) {
                System.out.println("[RastreamentoService] ERRO ao concluir pedido #"
                        + opGuardadoExpedicao + " automaticamente: " + ex.getMessage());
                // Não reseta o ciclo se a gravação no banco falhou — tenta
                // novamente na próxima leitura do CLP (o CLP continua
                // reportando finish=true até a flag ser tratada).
                return;
            }

            // NOVO: encerra o ciclo de produção deste pedido. Sem isso,
            // pedidoEmCurso continuaria TRUE para sempre e os guards de
            // TODAS as bancadas (que exigem pedidoEmCurso == true e
            // statusX == 0) nunca mais dispararia para o próximo pedido.
            SmartService.pedidoEmCurso   = false;
            SmartService.statusProducao  = 0;
            SmartService.statusEstoque   = 0;
            SmartService.statusProcesso  = 0;
            SmartService.statusMontagem  = 0;
            SmartService.statusExpedicao = 0;

            // Reseta também o estado exibido em /rastreamento, para a tela
            // de Acompanhamento voltar ao "aguardando" até o próximo pedido
            // ser enviado à produção.
            bancadaAtual  = "Nenhuma";
            numeroOpAtual = 0;
            statusEstoqueRastreio   = 0;
            statusProcessoRastreio  = 0;
            statusMontagemRastreio  = 0;
            statusExpedicaoRastreio = 0;
        }
    }

    // ─── Helper comum ────────────────────────────────────────────────────────
    private void atualizar(String nomeBancada, boolean start, boolean finish, int numeroOP) {
        if (numeroOP > 0) {
            numeroOpAtual = numeroOP;
        }
        if (start && SmartService.statusProducao == 0 && SmartService.pedidoEmCurso) {
            bancadaAtual = nomeBancada;
        }
    }

    // ─── Consulta pronta para o frontend ────────────────────────────────────
    public String obterEstadoPedido() {
        if (!SmartService.pedidoEmCurso) return "Pendente";
        if (SmartService.statusProducao == 1) return "Concluido";
        return "Em Producao";
    }
}