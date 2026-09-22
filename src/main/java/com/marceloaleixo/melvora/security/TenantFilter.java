package com.marceloaleixo.melvora.security;

import com.marceloaleixo.melvora.tenant.TenantContext;
import com.marceloaleixo.melvora.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarioRepository;

    public TenantFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails user) {
                usuarioRepository.findByEmailIgnoreCase(user.getUsername())
                    .ifPresent(u -> {
                        if (u.getRole() != com.marceloaleixo.melvora.entity.enums.Role.SUPER_ADMIN
                                && u.getEmpresa() != null
                                && u.getEmpresa().isAtiva()) {
                            TenantContext.set(u.getEmpresa().getId());
                        }
                    });
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
