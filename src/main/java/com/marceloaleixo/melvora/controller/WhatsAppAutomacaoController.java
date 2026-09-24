package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.ConfiguracaoWhatsAppAutomacao;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.repository.ConfiguracaoWhatsAppAutomacaoRepository;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comunicacao/automacao-inteligente")
public class WhatsAppAutomacaoController {
    private final ConfiguracaoWhatsAppAutomacaoRepository repository;
    private final EmpresaRepository empresaRepository;
    private final ModuloAcessoService moduloAcessoService;

    public WhatsAppAutomacaoController(ConfiguracaoWhatsAppAutomacaoRepository repository,
                                       EmpresaRepository empresaRepository,
                                       ModuloAcessoService moduloAcessoService) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String tela(Model model) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        ConfiguracaoWhatsAppAutomacao cfg = repository.findByEmpresaId(empresaId).orElseGet(() ->
                empresaRepository.findById(empresaId).map(ConfiguracaoWhatsAppAutomacao::new)
                        .orElseThrow(() -> new RegraNegocioException("Empresa não encontrada.")));
        model.addAttribute("config", cfg);
        model.addAttribute("activePage", "comunicacao");
        return "pages/whatsapp-automacao-inteligente";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String salvar(@RequestParam(defaultValue = "false") boolean ativa,
                         @RequestParam(defaultValue = "false") boolean responderSaudacao,
                         @RequestParam(defaultValue = "false") boolean responderServicos,
                         @RequestParam(defaultValue = "false") boolean responderPreco,
                         @RequestParam(defaultValue = "false") boolean responderAgendamento,
                         @RequestParam(defaultValue = "false") boolean encaminharHumano,
                         RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        var empresa = empresaRepository.findById(empresaId).orElseThrow(() -> new RegraNegocioException("Empresa não encontrada."));
        ConfiguracaoWhatsAppAutomacao cfg = repository.findByEmpresaId(empresaId).orElseGet(() -> new ConfiguracaoWhatsAppAutomacao(empresa));
        cfg.atualizar(ativa, responderSaudacao, responderServicos, responderPreco, responderAgendamento, encaminharHumano);
        repository.save(cfg);
        ra.addFlashAttribute("sucesso", ativa ? "Automação inteligente ativada." : "Automação inteligente desativada.");
        return "redirect:/comunicacao/automacao-inteligente";
    }
}
