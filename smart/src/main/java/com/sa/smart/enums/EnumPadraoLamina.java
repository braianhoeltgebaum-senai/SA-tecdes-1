package com.sa.smart.enums;

public enum EnumPadraoLamina {

    NENHUM(0),
    CASA(1),
    NAVIO(2),
    ESTRELA(3);

    private final int codigo;

    EnumPadraoLamina(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public static EnumPadraoLamina fromCodigo(int codigo) {

        for (EnumPadraoLamina padrao : values()) {
            if (padrao.codigo == codigo) {
                return padrao;
            }
        }

        throw new IllegalArgumentException("Código de padrão inválido");
    }
}