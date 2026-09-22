package com.marceloaleixo.melvora.security;

import com.marceloaleixo.melvora.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository repository;

    public CustomUserDetailsService(UsuarioRepository repository) { this.repository = repository; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var usuario = repository.findByEmailIgnoreCase(username.trim())
            .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        boolean empresaAtiva = usuario.getRole() == com.marceloaleixo.melvora.entity.enums.Role.SUPER_ADMIN
                || (usuario.getEmpresa() != null && usuario.getEmpresa().isAtiva());

        return User.withUsername(usuario.getEmail())
            .password(usuario.getSenhaHash())
            .roles(usuario.getRole().name())
            .disabled(!usuario.isAtivo() || !empresaAtiva)
            .build();
    }
}
