    package com.sa.smart.dto;

    import java.time.LocalDateTime;
    import java.util.List;
    import com.sa.smart.enums.EnumCorBloco;

    public record BlocoDTO(
        Long id,
        Integer andar,                    // ← novo
        EnumCorBloco corBloco,
        List<LaminaDTO> laminas,          // ← novo
        LocalDateTime criadoEm,
        Long estoqueId,
        Long idPedido
    ) {}
