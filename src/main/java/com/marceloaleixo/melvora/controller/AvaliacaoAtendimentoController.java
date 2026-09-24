package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.AvaliacaoRequests;
import com.marceloaleixo.melvora.service.AvaliacaoAtendimentoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AvaliacaoAtendimentoController {
    private final AvaliacaoAtendimentoService service;

    public AvaliacaoAtendimentoController(AvaliacaoAtendimentoService service) { this.service = service; }

    @GetMapping("/avaliacao/{token}")
    public String form(@PathVariable String token, Model model) {
        var avaliacao = service.buscarPublica(token);
        model.addAttribute("avaliacao", avaliacao);
        model.addAttribute("respondida", avaliacao.respondida());
        if (!model.containsAttribute("form")) model.addAttribute("form", new AvaliacaoRequests.WebForm(null, ""));
        return "pages/avaliacao-atendimento";
    }

    @PostMapping("/avaliacao/{token}")
    public String responder(@PathVariable String token,
                            @Valid @ModelAttribute("form") AvaliacaoRequests.WebForm form,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("avaliacao", service.buscarPublica(token));
            model.addAttribute("respondida", false);
            return "pages/avaliacao-atendimento";
        }
        try {
            service.responderPublica(token, form);
            ra.addFlashAttribute("sucesso", "Obrigado pelo seu feedback! 💜");
            return "redirect:/avaliacao/" + token;
        } catch (RuntimeException ex) {
            model.addAttribute("avaliacao", service.buscarPublica(token));
            model.addAttribute("respondida", service.buscarPublica(token).respondida());
            model.addAttribute("erro", ex.getMessage());
            return "pages/avaliacao-atendimento";
        }
    }
}
