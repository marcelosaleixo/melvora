package com.marceloaleixo.melvora.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public final class AvaliacaoRequests {
    private AvaliacaoRequests() {}

    public record WebForm(
            @Min(value = 1, message = "Escolha uma nota de 1 a 5.")
            @Max(value = 5, message = "Escolha uma nota de 1 a 5.")
            Integer nota,
            @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres.")
            String comentario
    ) {}
}
