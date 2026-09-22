package com.marceloaleixo.melvora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class EmpresaRequests {
    private EmpresaRequests() {}

    public record Criar(
            @NotBlank(message = "Nome fantasia é obrigatório.")
            @Size(max = 150, message = "Nome fantasia deve ter no máximo 150 caracteres.")
            String nomeFantasia) {
    }
}
