package com.marceloaleixo.melvora.dto;

import com.marceloaleixo.melvora.entity.Cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ClienteRequests {
    private ClienteRequests() {}

    public record CriarClienteRequest(
            @NotBlank @Size(max = 150) String nome,
            @Size(max = 30) String telefone,
            @Email @Size(max = 180) String email,
            @Size(max = 1000) String observacoes) {}

    public record ClienteResponse(Long id, String nome, String telefone, String email, String observacoes) {
        public static ClienteResponse from(Cliente c) {
            return new ClienteResponse(c.getId(), c.getNome(), c.getTelefone(), c.getEmail(), c.getObservacoes());
        }
    }
}
