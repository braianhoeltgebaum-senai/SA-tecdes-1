package com.sa.smart.enums;

public enum EnumCorTampa {

    PRETO(1),
    VERMELHO(2),
    AZUL(3);

    private final int codigo;

    EnumCorTampa(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public static EnumCorTampa fromCodigo(int codigo) {

        for (EnumCorTampa cor : values()) {
            if (cor.codigo == codigo) {
                return cor;
            }
        }

        throw new IllegalArgumentException("Código de cor inválido");
    }
}