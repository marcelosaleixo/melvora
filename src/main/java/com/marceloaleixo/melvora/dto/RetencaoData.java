package com.marceloaleixo.melvora.dto;

import java.time.LocalDateTime;

public final class RetencaoData {
    private RetencaoData() {}

    public record Oportunidade(
            Long clienteId,
            String clienteNome,
            String telefone,
            LocalDateTime ultimoAtendimento,
            long totalAtendimentos,
            long diasSemRetorno,
            boolean retencaoOptIn,
            boolean retencaoOptOut) {}
}
