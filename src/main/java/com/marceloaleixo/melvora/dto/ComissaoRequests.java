package com.marceloaleixo.melvora.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class ComissaoRequests {
    private ComissaoRequests() {}

    public record ConfiguracaoForm(
            @NotNull Long profissionalId,
            @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal percentual) {}
}
