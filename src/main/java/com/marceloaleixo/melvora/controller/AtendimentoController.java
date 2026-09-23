package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.service.AtendimentoService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AtendimentoController {
    private final AtendimentoService service;
    private final ModuloAcessoService moduloAcessoService;

    public AtendimentoController(AtendimentoService service, ModuloAcessoService moduloAcessoService) {
        this.service = service;
        this.moduloAcessoService = moduloAcessoService;
    }

    @GetMapping("/atendimentos")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String listar(@RequestParam(defaultValue = "0") int page, Model model) {
        moduloAcessoService.exigir(ModuloSistema.HISTORICO);
        model.addAttribute("pagina", service.listar(PageRequest.of(Math.max(0, page), 15,
                Sort.by("dataHoraInicio").descending())));
        return "pages/atendimentos";
    }
}
