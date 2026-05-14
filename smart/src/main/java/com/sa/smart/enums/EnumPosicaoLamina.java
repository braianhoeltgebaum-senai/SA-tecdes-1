package com.sa.smart.enums;

public enum EnumPosicaoLamina {

    ESQUERDA(1),
    FRENTE(2),
    DIREITA(3);

    private final int codigo;

    EnumPosicaoLamina(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public static EnumPosicaoLamina fromCodigo(int codigo) {

        for (EnumPosicaoLamina posicao : values()) {
            if (posicao.codigo == codigo) {
                return posicao;
            }
        }

        throw new IllegalArgumentException("Código de posição inválido");
    }
}