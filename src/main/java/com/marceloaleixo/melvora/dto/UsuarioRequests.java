package com.marceloaleixo.melvora.dto;

import com.marceloaleixo.melvora.entity.Usuario;
import com.marceloaleixo.melvora.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UsuarioRequests {
    private UsuarioRequests() {}

    public record Criar(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String nome,
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres.")
        String email,
        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 10, max = 72, message = "A senha deve ter entre 10 e 72 caracteres.")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$", message = "A senha deve conter maiúscula, minúscula, número e caractere especial.")
        String senha,
        @NotNull(message = "Perfil é obrigatório.") Role role,
        Long empresaId
    ) {}

    public record Resumo(Long id, String nome, String email, Role role, boolean ativo, Long empresaId, String empresaNome) {
        public static Resumo from(Usuario u) {
            return new Resumo(u.getId(), u.getNome(), u.getEmail(), u.getRole(), u.isAtivo(),
                u.getEmpresa() == null ? null : u.getEmpresa().getId(),
                u.getEmpresa() == null ? "Plataforma" : u.getEmpresa().getNomeFantasia());
        }
    }
}
