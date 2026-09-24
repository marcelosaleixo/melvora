package com.marceloaleixo.melvora.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public final class DisponibilidadeRequests {
    private DisponibilidadeRequests() {}
    public record HorarioForm(@Min(1) @Max(7) int diaSemana, @NotNull LocalTime horaInicio, @NotNull LocalTime horaFim,
                              @Min(5) @Max(240) int intervaloMinutos, boolean ativo) {}
    public record Consulta(LocalDate data, Long servicoId, Long profissionalId) {}
}
