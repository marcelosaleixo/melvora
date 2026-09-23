package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.ComunicacaoRequests;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.service.ComunicacaoAutomacaoService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.tenant.TenantContext;
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
@RequestMapping("/comunicacao/automacao")
public class ComunicacaoAutomacaoController {
    private final ComunicacaoAutomacaoService service;
    private final ModuloAcessoService modulo;
    private final com.marceloaleixo.melvora.repository.EmpresaRepository empresaRepository;

    public ComunicacaoAutomacaoController(ComunicacaoAutomacaoService service, ModuloAcessoService modulo, com.marceloaleixo.melvora.repository.EmpresaRepository empresaRepository) {
        this.service = service; this.modulo = modulo; this.empresaRepository = empresaRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String index(Model model) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        Empresa empresa = empresaRepository.findById(empresaId).orElseThrow();
        var config = service.configuracao(empresaId, empresa);
        model.addAttribute("config", config);
        model.addAttribute("fila", service.fila(empresaId, PageRequest.of(0, 30, Sort.by("dataHoraEnvio").ascending())));
        model.addAttribute("form", new ComunicacaoRequests.ConfigForm(config.isConfirmacaoAtiva(), config.getConfirmacaoMinutosAntes(), config.isLembreteAtivo(), config.getLembreteMinutosAntes(), config.isPosAtendimentoAtivo(), config.getPosAtendimentoMinutosDepois()));
        model.addAttribute("activePage", "automacao");
        return "pages/comunicacao-automacao";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String salvar(@Valid @ModelAttribute("form") ComunicacaoRequests.ConfigForm form, BindingResult result, Model model, RedirectAttributes ra) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        Empresa empresa = empresaRepository.findById(empresaId).orElseThrow();
        if (result.hasErrors()) {
            model.addAttribute("config", service.configuracao(empresaId, empresa));
            model.addAttribute("fila", service.fila(empresaId, PageRequest.of(0, 30, Sort.by("dataHoraEnvio").ascending())));
            model.addAttribute("activePage", "automacao");
            return "pages/comunicacao-automacao";
        }
        service.salvarConfiguracao(empresaId, form, empresa);
        ra.addFlashAttribute("sucesso", "Automação de comunicação atualizada com sucesso.");
        return "redirect:/comunicacao/automacao";
    }

    @GetMapping("/{id}/abrir")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String abrir(@PathVariable Long id) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        return "redirect:" + service.abrir(id, TenantContext.getRequired());
    }
}
