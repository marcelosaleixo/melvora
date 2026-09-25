package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.marceloaleixo.melvora.controller")
public class WebViewModelAdvice {
    private final ModuloAcessoService moduloAcessoService;

    @Value("${melvora.environment:DEVELOPMENT}")
    private String melvoraEnvironment;

    public WebViewModelAdvice(ModuloAcessoService moduloAcessoService) {
        this.moduloAcessoService = moduloAcessoService;
    }

    @ModelAttribute("melvoraEnvironment")
    public String melvoraEnvironment() {
        if (melvoraEnvironment == null || melvoraEnvironment.isBlank()) return "DEVELOPMENT";
        return melvoraEnvironment.trim().toUpperCase();
    }

    @ModelAttribute("activePage")
    public String activePage(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (uri == null || uri.isBlank() || "/".equals(uri) || "/dashboard".equals(uri)) return "dashboard";
        if (uri.startsWith("/clientes")) return "clientes";
        if (uri.startsWith("/produtos")) return "produtos";
        if (uri.startsWith("/estoque")) return "estoque";
        if (uri.startsWith("/mega-hair")) return "mega-hair";
        if (uri.startsWith("/agenda")) return "agenda";
        if (uri.startsWith("/usuarios") || uri.startsWith("/super-admin/usuarios")) return "usuarios";
        if (uri.startsWith("/empresas")) return "empresas";
        if (uri.startsWith("/servicos")) return "servicos";
        if (uri.startsWith("/atendimentos")) return "atendimentos";
        if (uri.startsWith("/financeiro")) return "financeiro";
        if (uri.startsWith("/relatorios")) return "relatorios";
        return "";
    }

    @ModelAttribute("modulosAtivos")
    public List<ModuloSistema> modulosAtivos() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || TenantContext.get() == null) {
            return List.of();
        }
        return moduloAcessoService.modulosAtivosDaEmpresa(TenantContext.get());
    }
}
