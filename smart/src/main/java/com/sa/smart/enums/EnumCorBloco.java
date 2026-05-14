package com.sa.smart.enums;

public enum EnumCorBloco {

    PRETO(1, "Preto"),
    VERMELHO(2, "Vermelho"),
    AZUL(3, "Azul");

    private final Integer codigo;
    private final String descricao;

    EnumCorBloco(Integer codigo, String descricao) {

        this.codigo = codigo;
        this.descricao = descricao;

    }

    public Integer getCodigo() {

        return codigo;

    }

    public String getDescricao() {

        return descricao;

    }

    public static EnumCorBloco fromCodigo(Integer codigo) {

        for (EnumCorBloco cor : EnumCorBloco.values()) {

            if (cor.getCodigo().equals(codigo)) {

                return cor;

            }
        }

        throw new IllegalArgumentException("Código de cor inválido: " + codigo + ". Use: 1-Preto, 2-Vermelho, 3-Azul");

    }

    public static boolean isValid(Integer codigo) {

        return codigo != null && (codigo == 1 || codigo == 2 || codigo == 3);

    }
}