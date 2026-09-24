package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.service.SatisfacaoDashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SatisfacaoDashboardController {
    private final SatisfacaoDashboardService service;
    private final ModuloAcessoService moduloAcessoService;

    public SatisfacaoDashboardController(SatisfacaoDashboardService service,
                                          ModuloAcessoService moduloAcessoService) {
        this.service = service;
        this.moduloAcessoService = moduloAcessoService;
    }

    @GetMapping("/comunicacao/avaliacoes/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        LocalDate hoje = LocalDate.now();
        if (inicio == null) inicio = hoje.withDayOfMonth(1);
        if (fim == null) fim = hoje;
        model.addAttribute("dashboard", service.gerar(inicio, fim));
        model.addAttribute("activePage", "satisfacao");
        return "pages/satisfacao-dashboard";
    }
}
