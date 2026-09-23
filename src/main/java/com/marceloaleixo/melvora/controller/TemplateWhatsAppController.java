package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.TemplateWhatsAppRequests;
import com.marceloaleixo.melvora.service.TemplateWhatsAppService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comunicacao/templates")
public class TemplateWhatsAppController {
    private final TemplateWhatsAppService service;

    public TemplateWhatsAppController(TemplateWhatsAppService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        service.garantirPadroes();
        model.addAttribute("pagina", service.listar(PageRequest.of(Math.max(0, page), 10, Sort.by("nome").ascending())));
        if (!model.containsAttribute("form")) model.addAttribute("form", new TemplateWhatsAppRequests.WebForm("", ""));
        model.addAttribute("activePage", "comunicacao");
        return "pages/comunicacao-templates";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String criar(@Valid @ModelAttribute("form") TemplateWhatsAppRequests.WebForm form,
                        BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pagina", service.listar(PageRequest.of(0, 10, Sort.by("nome").ascending())));
            model.addAttribute("activePage", "comunicacao");
            return "pages/comunicacao-templates";
        }
        try {
            service.criar(form);
            ra.addFlashAttribute("sucesso", "Template criado com sucesso.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/comunicacao/templates";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarForm(@PathVariable Long id, Model model) {
        var template = service.buscar(id);
        model.addAttribute("templateId", id);
        model.addAttribute("form", new TemplateWhatsAppRequests.WebForm(template.getNome(), template.getMensagem()));
        model.addAttribute("activePage", "comunicacao");
        return "pages/comunicacao-template-editar";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMIN')")
    public String editar(@PathVariable Long id,
                         @Valid @ModelAttribute("form") TemplateWhatsAppRequests.WebForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("templateId", id);
            model.addAttribute("activePage", "comunicacao");
            return "pages/comunicacao-template-editar";
        }
        try {
            service.editar(id, form);
            ra.addFlashAttribute("sucesso", "Template atualizado com sucesso.");
            return "redirect:/comunicacao/templates";
        } catch (RuntimeException ex) {
            model.addAttribute("erro", ex.getMessage());
            model.addAttribute("templateId", id);
            model.addAttribute("activePage", "comunicacao");
            return "pages/comunicacao-template-editar";
        }
    }

    @PostMapping("/{id}/alternar-status")
    @PreAuthorize("hasRole('ADMIN')")
    public String alternarStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            boolean ativo = service.alternarStatus(id);
            ra.addFlashAttribute("sucesso", ativo ? "Template ativado." : "Template desativado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/comunicacao/templates";
    }
}
