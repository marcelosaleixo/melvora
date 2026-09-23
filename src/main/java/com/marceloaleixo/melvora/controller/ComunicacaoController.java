package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.repository.AgendamentoRepository;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.service.TemplateWhatsAppService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.service.WhatsAppService;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ComunicacaoController {
    private static final List<StatusAgendamento> STATUS_ATIVOS = List.of(
            StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO);

    private final ModuloAcessoService moduloAcessoService;
    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final WhatsAppService whatsAppService;
    private final TemplateWhatsAppService templateWhatsAppService;

    public ComunicacaoController(ModuloAcessoService moduloAcessoService,
                                 AgendamentoRepository agendamentoRepository,
                                 ClienteRepository clienteRepository,
                                 WhatsAppService whatsAppService,
                                 TemplateWhatsAppService templateWhatsAppService) {
        this.moduloAcessoService = moduloAcessoService;
        this.agendamentoRepository = agendamentoRepository;
        this.clienteRepository = clienteRepository;
        this.whatsAppService = whatsAppService;
        this.templateWhatsAppService = templateWhatsAppService;
    }

    @GetMapping("/comunicacao")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String index(Model model) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        LocalDateTime agora = LocalDateTime.now();
        var proximos = agendamentoRepository.listarProximosDaEmpresa(
                empresaId, agora, agora.plusDays(7), STATUS_ATIVOS,
                PageRequest.of(0, 50, Sort.by("dataHoraInicio").ascending()));
        var clientes = clienteRepository.findByEmpresaIdAndAtivoTrue(
                empresaId, PageRequest.of(0, 50, Sort.by("nome").ascending())).getContent();
        model.addAttribute("agendamentos", proximos);
        model.addAttribute("clientes", clientes);
        model.addAttribute("templates", templateWhatsAppService.ativos());
        model.addAttribute("activePage", "comunicacao");
        return "pages/comunicacao";
    }

    @GetMapping("/comunicacao/whatsapp/cliente/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String cliente(@PathVariable Long id, @RequestParam(defaultValue = "Olá! Gostaria de falar com você sobre seu atendimento no Melvora. 😊") String mensagem) {
        var cliente = clienteRepository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrada."));
        return "redirect:" + whatsAppService.linkParaCliente(cliente, mensagem);
    }

    @GetMapping("/comunicacao/whatsapp/agendamento/{id}/confirmacao")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String confirmacao(@PathVariable Long id) {
        var agendamento = agendamentoRepository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));
        return "redirect:" + whatsAppService.linkConfirmacao(agendamento);
    }

    @GetMapping("/comunicacao/whatsapp/agendamento/{id}/template/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String template(@PathVariable Long id, @PathVariable Long templateId) {
        var agendamento = agendamentoRepository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));
        var template = templateWhatsAppService.buscar(templateId);
        return "redirect:" + whatsAppService.linkComTemplate(agendamento, template);
    }

    @GetMapping("/comunicacao/whatsapp/agendamento/{id}/lembrete")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String lembrete(@PathVariable Long id) {
        var agendamento = agendamentoRepository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));
        return "redirect:" + whatsAppService.linkLembrete(agendamento);
    }
}
