package com.sa.smart.service;

import org.springframework.stereotype.Service;

@Service
public class RastreamentoService {

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

    public void processarEstoque(byte[] dados, int numeroOP, boolean start, boolean finish) {
        atualizar("estoque", start, finish, numeroOP);
        if (start)  statusEstoqueRastreio = 1;
        if (finish) statusEstoqueRastreio = 2;
    }

    public void processarProcesso(int numeroOP, boolean start, boolean finish) {
        atualizar("processo", start, finish, numeroOP);
        if (start)  statusProcessoRastreio = 1;
        if (finish) statusProcessoRastreio = 2;
    }

    public void processarMontagem(int numeroOP, boolean start, boolean finish) {
        atualizar("montagem", start, finish, numeroOP);
        if (start)  statusMontagemRastreio = 1;
        if (finish) statusMontagemRastreio = 2;
    }

    // Este método NÃO é mais chamado pelo ExpedicaoService,
    // mas pode ser mantido para outros fins ou removido.
    public void processarExpedicao(int numeroOP, boolean start, boolean finish,
                                    int posicaoGuardarExp, boolean ocupadoExp,
                                    int posicaoGuardadoExpedicao, int opGuardadoExpedicao) {
        // A lógica de conclusão agora está em ExpedicaoService.
        // Mantido apenas para compatibilidade.
        atualizar("expedicao", start, finish, numeroOP);
        if (start)  statusExpedicaoRastreio = 1;
        if (finish) statusExpedicaoRastreio = 2;
    }

    private void atualizar(String nomeBancada, boolean start, boolean finish, int numeroOP) {
        if (numeroOP > 0) {
            numeroOpAtual = numeroOP;
        }
        if (start && SmartService.statusProducao == 0 && SmartService.pedidoEmCurso) {
            bancadaAtual = nomeBancada;
        }
    }

    public String obterEstadoPedido() {
        if (!SmartService.pedidoEmCurso) return "Pendente";
        if (SmartService.statusProducao == 1) return "Concluido";
        return "Em Producao";
    }
}