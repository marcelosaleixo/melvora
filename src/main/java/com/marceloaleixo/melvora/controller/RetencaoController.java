package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.service.ComunicacaoAutomacaoService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.service.WhatsAppService;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RetencaoController {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ModuloAcessoService modulo;
    private final ComunicacaoAutomacaoService comunicacao;
    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final WhatsAppService whatsAppService;
    private final com.marceloaleixo.melvora.repository.PreferenciaComunicacaoContatoRepository preferenciaRepository;

    public RetencaoController(ModuloAcessoService modulo, ComunicacaoAutomacaoService comunicacao,
                              EmpresaRepository empresaRepository, ClienteRepository clienteRepository,
                              WhatsAppService whatsAppService,
                              com.marceloaleixo.melvora.repository.PreferenciaComunicacaoContatoRepository preferenciaRepository) {
        this.modulo = modulo;
        this.comunicacao = comunicacao;
        this.empresaRepository = empresaRepository;
        this.clienteRepository = clienteRepository;
        this.whatsAppService = whatsAppService;
        this.preferenciaRepository = preferenciaRepository;
    }

    @GetMapping("/comunicacao/retencao")
    @PreAuthorize("hasRole('ADMIN')")
    public String index(Model model) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        Empresa empresa = empresaRepository.findById(empresaId).orElseThrow();
        var config = comunicacao.configuracao(empresaId, empresa);
        var oportunidades = comunicacao.oportunidadesRetencao(empresaId, config.getRetencaoDiasSemRetorno(), PageRequest.of(0, 100));
        model.addAttribute("config", config);
        model.addAttribute("oportunidades", oportunidades);
        model.addAttribute("activePage", "retencao");
        return "pages/retencao";
    }

    @GetMapping("/comunicacao/retencao/cliente/{id}/abrir")
    @PreAuthorize("hasRole('ADMIN')")
    public String abrir(@PathVariable Long id, RedirectAttributes ra) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        var cliente = clienteRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrada."));
        Empresa empresa = empresaRepository.findById(empresaId).orElseThrow();
        var config = comunicacao.configuracao(empresaId, empresa);
        if (!config.isRetencaoAtiva()) {
            ra.addFlashAttribute("erro", "A retenção está desativada para esta empresa.");
            return "redirect:/comunicacao/retencao";
        }
        String mensagem = config.getRetencaoMensagem();
        var oportunidades = comunicacao.oportunidadesRetencao(empresaId, config.getRetencaoDiasSemRetorno(), PageRequest.of(0, 100));
        var oportunidade = oportunidades.stream().filter(o -> o.clienteId().equals(cliente.getId())).findFirst().orElse(null);
        if (oportunidade == null) {
            ra.addFlashAttribute("erro", "A cliente não está elegível para retenção no momento.");
            return "redirect:/comunicacao/retencao";
        }
        mensagem = mensagem.replace("{cliente}", oportunidade.clienteNome())
                .replace("{dias}", String.valueOf(oportunidade.diasSemRetorno()))
                .replace("{ultima_data}", DATA.format(oportunidade.ultimoAtendimento()));
        return "redirect:" + whatsAppService.linkParaCliente(cliente, mensagem);
    }
    @PostMapping("/cliente/{id}/consentimento")
    @PreAuthorize("hasRole('ADMIN')")
    public String consentimento(@PathVariable Long id, @RequestParam boolean permitir, RedirectAttributes ra) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        var cliente = clienteRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrada."));
        if (cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
            ra.addFlashAttribute("erro", "Cadastre um telefone antes de autorizar a retenção.");
            return "redirect:/comunicacao/retencao";
        }
        String telefone = cliente.getTelefone().replaceAll("\\D", "");
        if (telefone.length() == 10 || telefone.length() == 11) telefone = "55" + telefone;
        final String canonical = telefone;
        var pref = preferenciaRepository.findByEmpresaIdAndTelefone(empresaId, canonical)
                .orElseGet(() -> preferenciaRepository.save(new com.marceloaleixo.melvora.entity.PreferenciaComunicacaoContato(cliente.getEmpresa(), cliente, canonical)));
        pref.vincularCliente(cliente);
        if (permitir) { pref.permitirRetencao(); ra.addFlashAttribute("sucesso", "Consentimento para mensagens de retenção ativado."); }
        else { pref.bloquearRetencao(); ra.addFlashAttribute("sucesso", "Cliente removida das mensagens automáticas de retenção."); }
        preferenciaRepository.save(pref);
        return "redirect:/comunicacao/retencao";
    }

}
