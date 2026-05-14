package com.sa.smart.enums;

public enum EnumTipoPedido {

    SIMPLES(1),
    DUPLO(2),
    TRIPLO(3);

    private final int codigo;

    EnumTipoPedido(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public static EnumTipoPedido fromCodigo(int codigo) {

        for (EnumTipoPedido tipo : values()) {
            if (tipo.codigo == codigo) {
                return tipo;
            }
        }

        throw new IllegalArgumentException("Código de tipo inválido");
    }
}