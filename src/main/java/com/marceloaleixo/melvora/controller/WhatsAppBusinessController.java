package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.ComunicacaoRequests;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.WhatsAppIntegrationMode;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.service.WhatsAppBusinessService;
import com.marceloaleixo.melvora.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comunicacao/whatsapp-business")
public class WhatsAppBusinessController {
    private final WhatsAppBusinessService service;
    private final ModuloAcessoService modulo;
    private final EmpresaRepository empresaRepository;
    private final String publicBaseUrl;

    public WhatsAppBusinessController(WhatsAppBusinessService service, ModuloAcessoService modulo, EmpresaRepository empresaRepository,
                                      @Value("${melvora.public-base-url:}") String publicBaseUrl) {
        this.service = service; this.modulo = modulo; this.empresaRepository = empresaRepository; this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String index(Model model) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        var empresa = empresaRepository.findById(empresaId).orElseThrow();
        var cfg = service.configuracao(empresaId, empresa);
        adicionarModelo(model, cfg, new ComunicacaoRequests.WhatsAppBusinessForm(
                cfg.getModoIntegracao(),
                value(cfg.getPhoneNumberId()), "", value(cfg.getApiVersion()), value(cfg.getApiBaseUrl()),
                value(cfg.getN8nBaseUrl()), value(cfg.getN8nWebhookPath()), "", cfg.isAtiva()));
        return "pages/whatsapp-business";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String salvar(@Valid @ModelAttribute("form") ComunicacaoRequests.WhatsAppBusinessForm form,
                         BindingResult result, Model model, RedirectAttributes ra) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        var empresa = empresaRepository.findById(empresaId).orElseThrow();
        if (result.hasErrors()) {
            var cfg = service.configuracao(empresaId, empresa);
            adicionarModelo(model, cfg, form);
            return "pages/whatsapp-business";
        }
        try {
            service.salvarConfiguracao(empresaId, empresa, form);
            ra.addFlashAttribute("sucesso", "Integração WhatsApp salva com segurança.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/comunicacao/whatsapp-business";
    }

    @PostMapping("/testar")
    @PreAuthorize("hasRole('ADMIN')")
    public String testar(RedirectAttributes ra) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        try { ra.addFlashAttribute("sucesso", service.testarConexao(TenantContext.getRequired())); }
        catch (RuntimeException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/comunicacao/whatsapp-business";
    }

    @PostMapping("/fila/{id}/enviar")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String enviar(@PathVariable Long id, RedirectAttributes ra) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        try { service.enviar(id, TenantContext.getRequired()); ra.addFlashAttribute("sucesso", "Mensagem enviada ao WhatsApp."); }
        catch (RuntimeException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/comunicacao/automacao";
    }

    private void adicionarModelo(Model model, com.marceloaleixo.melvora.entity.ConfiguracaoWhatsAppBusiness cfg,
                                 ComunicacaoRequests.WhatsAppBusinessForm form) {
        model.addAttribute("form", form);
        model.addAttribute("configurado", cfg.getModoIntegracao() == WhatsAppIntegrationMode.META_CLOUD
                ? !blank(cfg.getAccessTokenEncrypted()) : !blank(cfg.getN8nTokenEncrypted()));
        model.addAttribute("n8nConfigurado", cfg.getModoIntegracao() == WhatsAppIntegrationMode.N8N && !blank(cfg.getN8nIntegrationKey()));
        String callbackPath = service.callbackPath(cfg);
        model.addAttribute("n8nCallbackPath", callbackPath);
        model.addAttribute("n8nCallbackUrl", publicBaseUrl.isBlank() ? callbackPath : publicBaseUrl + callbackPath);
        model.addAttribute("integrationModes", WhatsAppIntegrationMode.values());
        model.addAttribute("activePage", "whatsapp-business");
    }

    private static String value(String v) { return v == null ? "" : v; }
    private static boolean blank(String v) { return v == null || v.isBlank(); }
}
