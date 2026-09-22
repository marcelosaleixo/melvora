package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.EmpresaRequests;
import com.marceloaleixo.melvora.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class EmpresaController {
    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @GetMapping("/empresas")
    public String empresas(@RequestParam(defaultValue = "0") int page, Model model) {
        int safePage = Math.max(0, page);
        model.addAttribute("pagina", service.listar(PageRequest.of(safePage, 15,
                Sort.by("ativa").descending().and(Sort.by("nomeFantasia").ascending()))));
        if (!model.containsAttribute("empresaForm")) {
            model.addAttribute("empresaForm", new EmpresaRequests.Criar(""));
        }
        return "pages/empresas";
    }

    @PostMapping("/empresas")
    public String criar(
            @Valid @ModelAttribute("empresaForm") EmpresaRequests.Criar form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("pagina", service.listar(PageRequest.of(0, 15,
                    Sort.by("ativa").descending().and(Sort.by("nomeFantasia").ascending()))));
            return "pages/empresas";
        }

        try {
            service.criar(form);
            ra.addFlashAttribute("sucesso", "Empresa cadastrada com sucesso.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/empresas";
    }

    @PostMapping("/empresas/{id}/status")
    public String status(
            @PathVariable Long id,
            @RequestParam boolean ativa,
            RedirectAttributes ra) {
        try {
            service.alterarStatus(id, ativa);
            ra.addFlashAttribute("sucesso", ativa ? "Empresa ativada." : "Empresa desativada.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/empresas";
    }

    private String mensagemSegura(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Não foi possível concluir a operação."
                : ex.getMessage();
    }
}
