package com.marceloaleixo.melvora.dto;

import com.marceloaleixo.melvora.entity.enums.WhatsAppIntegrationMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ComunicacaoRequests {
    private ComunicacaoRequests() {}

    public record ConfigForm(
            boolean confirmacaoAtiva,
            @Min(15) @Max(10080) int confirmacaoMinutosAntes,
            boolean lembreteAtivo,
            @Min(15) @Max(10080) int lembreteMinutosAntes,
            boolean posAtendimentoAtivo,
            @Min(0) @Max(10080) int posAtendimentoMinutosDepois) {}

    public record WhatsAppBusinessForm(
            @NotNull WhatsAppIntegrationMode modoIntegracao,
            @Size(max = 100) String phoneNumberId,
            @Size(max = 4096) String accessToken,
            @Pattern(regexp = "v\\d+\\.\\d+", message = "Informe uma versão válida, como v23.0.") String apiVersion,
            @Size(max = 500) String apiBaseUrl,
            @Size(max = 500) String n8nBaseUrl,
            @Size(max = 500) String n8nWebhookPath,
            @Size(max = 4096) String n8nToken,
            boolean ativa) {}
}
