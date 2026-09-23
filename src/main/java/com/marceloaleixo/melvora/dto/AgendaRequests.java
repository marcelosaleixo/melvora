package com.marceloaleixo.melvora.dto;

import com.marceloaleixo.melvora.entity.Agendamento;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.entity.enums.TipoAgendamento;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class AgendaRequests {
    private AgendaRequests() {}

    public record CriarAgendamentoRequest(
            @NotNull Long clienteId,
            @NotNull Long profissionalId,
            @NotNull TipoAgendamento tipo,
            Long servicoId,
            @NotNull @FutureOrPresent LocalDateTime dataHoraInicio,
            @NotNull @Future LocalDateTime dataHoraFim,
            @Size(max = 2000) String observacoes) {}

    public record AlterarStatusRequest(@NotNull StatusAgendamento status) {}

    public record EditarAgendamentoRequest(
            @NotNull TipoAgendamento tipo,
            Long servicoId,
            @NotNull @FutureOrPresent LocalDateTime dataHoraInicio,
            @NotNull @Future LocalDateTime dataHoraFim,
            @Size(max = 2000) String observacoes) {}

    public record AgendamentoResponse(
            Long id,
            Long clienteId,
            String cliente,
            Long profissionalId,
            String profissional,
            TipoAgendamento tipo,
            Long servicoId,
            String servico,
            LocalDateTime dataHoraInicio,
            LocalDateTime dataHoraFim,
            StatusAgendamento status,
            String observacoes) {
        public static AgendamentoResponse from(Agendamento a) {
            return new AgendamentoResponse(a.getId(), a.getCliente().getId(), a.getCliente().getNome(),
                    a.getProfissional().getId(), a.getProfissional().getNome(), a.getTipo(),
                    a.getServico() == null ? null : a.getServico().getId(),
                    a.getServico() == null ? null : a.getServico().getNome(),
                    a.getDataHoraInicio(), a.getDataHoraFim(), a.getStatus(), a.getObservacoes());
        }
    }
}
