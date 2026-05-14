package com.sa.smart.enums;

public enum EnumStatusPedido {

    PENDENTE(1),
    PRODUCAO(2),
    CONCLUIDO(3);

    private final int codigo;

    EnumStatusPedido(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public static EnumStatusPedido fromCodigo(int codigo) {

        for (EnumStatusPedido status : values()) {
            if (status.codigo == codigo) {
                return status;
            }
        }

        throw new IllegalArgumentException("Código de status inválido");
    }
}