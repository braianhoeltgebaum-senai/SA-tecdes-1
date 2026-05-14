package com.sa.smart.enums;

public enum EnumCorLamina {

    VERMELHO(1),
    AZUL(2),
    AMARELO(3),
    VERDE(4),
    PRETO(5),
    BRANCO(6);

    private final int codigo;

    EnumCorLamina(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public static EnumCorLamina fromCodigo(int codigo) {

        for (EnumCorLamina cor : values()) {
            if (cor.codigo == codigo) {
                return cor;
            }
        }

        throw new IllegalArgumentException("Código de cor inválido");
    }
}