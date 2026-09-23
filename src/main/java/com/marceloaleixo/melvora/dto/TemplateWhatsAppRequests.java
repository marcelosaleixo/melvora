package com.marceloaleixo.melvora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class TemplateWhatsAppRequests {
    private TemplateWhatsAppRequests() {}

    public record WebForm(
            @NotBlank(message = "Informe o nome do template.")
            @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
            String nome,
            @NotBlank(message = "Informe a mensagem.")
            @Size(max = 4000, message = "A mensagem deve ter no máximo 4000 caracteres.")
            String mensagem) {}
}
