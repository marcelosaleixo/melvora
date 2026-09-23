package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.ComissaoRequests;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.service.ComissaoService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ComissaoController {
    private final ComissaoService service;
    private final ModuloAcessoService modulo;
    public ComissaoController(ComissaoService service, ModuloAcessoService modulo) { this.service = service; this.modulo = modulo; }

    @GetMapping("/comissoes")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String index(@RequestParam(defaultValue="0") int page, Model model, Authentication auth) {
        modulo.exigir(ModuloSistema.COMISSOES);
        model.addAttribute("pagina", service.listar(PageRequest.of(Math.max(0,page), 20, Sort.by("dataComissao").descending().and(Sort.by("id").descending())), auth));
        model.addAttribute("configuracoes", service.configuracoes());
        model.addAttribute("profissionais", service.profissionais());
        if (!model.containsAttribute("form")) model.addAttribute("form", new ComissaoRequests.ConfiguracaoForm(null, null));
        return "pages/comissoes";
    }

    @PostMapping("/comissoes/configuracoes")
    @PreAuthorize("hasRole('ADMIN')")
    public String salvar(@Valid @ModelAttribute("form") ComissaoRequests.ConfiguracaoForm form, BindingResult br, Model model, RedirectAttributes ra) {
        modulo.exigir(ModuloSistema.COMISSOES);
        if (br.hasErrors()) { preparar(model); return "pages/comissoes"; }
        try { service.salvarConfiguracao(form); ra.addFlashAttribute("sucesso", "Configuração de comissão salva com sucesso."); return "redirect:/comissoes"; }
        catch (RuntimeException e) { preparar(model); model.addAttribute("erro", mensagem(e)); return "pages/comissoes"; }
    }

    @PostMapping("/comissoes/{id}/pagar")
    @PreAuthorize("hasRole('ADMIN')")
    public String pagar(@PathVariable Long id, RedirectAttributes ra) { try { service.pagar(id); ra.addFlashAttribute("sucesso", "Comissão marcada como paga."); } catch (RuntimeException e) { ra.addFlashAttribute("erro", mensagem(e)); } return "redirect:/comissoes"; }

    @PostMapping("/comissoes/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')")
    public String cancelar(@PathVariable Long id, RedirectAttributes ra) { try { service.cancelar(id); ra.addFlashAttribute("sucesso", "Comissão cancelada."); } catch (RuntimeException e) { ra.addFlashAttribute("erro", mensagem(e)); } return "redirect:/comissoes"; }

    private void preparar(Model model) { model.addAttribute("configuracoes", service.configuracoes()); model.addAttribute("profissionais", service.profissionais()); model.addAttribute("pagina", service.listar(PageRequest.of(0,20,Sort.by("dataComissao").descending()), null)); }
    private String mensagem(RuntimeException e) { return e.getMessage()==null || e.getMessage().isBlank() ? "Não foi possível concluir a operação." : e.getMessage(); }
}
