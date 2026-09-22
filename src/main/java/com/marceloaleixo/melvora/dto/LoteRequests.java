package com.marceloaleixo.melvora.dto;

import com.marceloaleixo.melvora.entity.LoteMegaHair;
import com.marceloaleixo.melvora.entity.enums.MetodoMegaHair;
import com.marceloaleixo.melvora.entity.enums.TipoFio;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class LoteRequests {
    private LoteRequests() {}
    public record CriarLoteRequest(
            @NotNull Long produtoId,
            @NotBlank @Size(max = 60) String codigo,
            @NotNull @Min(1) @Max(100000) Integer quantidade,
            @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal pesoPorUnidadeGramas,
            @NotNull @Min(1) @Max(300) Integer comprimentoCm,
            @NotNull TipoFio tipoFio,
            @NotBlank @Size(max = 80) String cor,
            @NotNull MetodoMegaHair metodo) {}

    public record LoteResponse(Long id, Long produtoId, String codigo, int quantidadeInicial, int quantidadeDisponivel,
                               BigDecimal pesoPorUnidadeGramas, int comprimentoCm, TipoFio tipoFio, String cor, MetodoMegaHair metodo) {
        public static LoteResponse from(LoteMegaHair l) {
            return new LoteResponse(l.getId(), l.getProduto().getId(), l.getCodigo(), l.getQuantidadeInicial(), l.getQuantidadeDisponivel(),
                    l.getPesoPorUnidadeGramas(), l.getComprimentoCm(), l.getTipoFio(), l.getCor(), l.getMetodo());
        }
    }
}
