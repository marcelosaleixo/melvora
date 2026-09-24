package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.service.AvaliacaoAtendimentoService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.tenant.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AvaliacoesController {
    private final AvaliacaoAtendimentoService service;
    private final ModuloAcessoService moduloAcessoService;

    public AvaliacoesController(AvaliacaoAtendimentoService service, ModuloAcessoService moduloAcessoService) {
        this.service = service;
        this.moduloAcessoService = moduloAcessoService;
    }

    @GetMapping("/comunicacao/avaliacoes")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        model.addAttribute("pagina", service.listar(PageRequest.of(Math.max(0, page), 12, Sort.by("createdAt").descending())));
        model.addAttribute("resumo", service.resumo());
        model.addAttribute("activePage", "comunicacao");
        return "pages/avaliacoes";
    }
}
