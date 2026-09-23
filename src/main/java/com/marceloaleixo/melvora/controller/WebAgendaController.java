package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.AgendaRequests;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.entity.enums.TipoAgendamento;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.repository.UsuarioRepository;
import com.marceloaleixo.melvora.service.AgendaService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.service.ServicoService;
import com.marceloaleixo.melvora.tenant.TenantContext;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebAgendaController {
    private final AgendaService agendaService;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloAcessoService moduloAcessoService;
    private final ServicoService servicoService;

    public WebAgendaController(AgendaService agendaService, ClienteRepository clienteRepository,
                               UsuarioRepository usuarioRepository, ModuloAcessoService moduloAcessoService, ServicoService servicoService) {
        this.agendaService = agendaService;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.moduloAcessoService = moduloAcessoService;
        this.servicoService = servicoService;
    }

    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String agenda(@RequestParam(required = false) LocalDate data, @RequestParam(required = false) Long clienteId, Model model) {
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        LocalDate selecionada = data == null ? LocalDate.now() : data;
        LocalDate inicioSemana = selecionada.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fimSemana = inicioSemana.plusDays(6);
        model.addAttribute("dataSelecionada", selecionada);
        model.addAttribute("inicioSemana", inicioSemana);
        model.addAttribute("fimSemana", fimSemana);
        model.addAttribute("agendamentos", agendaService.listarPeriodo(inicioSemana, fimSemana));
        model.addAttribute("clientes", clienteRepository.findByEmpresaIdAndAtivoTrue(TenantContext.getRequired(),
                org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by("nome").ascending())).getContent());
        model.addAttribute("profissionais", usuarioRepository.findProfissionaisAtivos(TenantContext.getRequired()));
        model.addAttribute("tipos", TipoAgendamento.values());
        model.addAttribute("status", StatusAgendamento.values());
        model.addAttribute("megaHairAtivo", moduloAcessoService.possui(ModuloSistema.MEGA_HAIR));
        boolean servicosAtivo = moduloAcessoService.possui(ModuloSistema.SERVICOS);
        model.addAttribute("servicosAtivo", servicosAtivo);
        model.addAttribute("servicos", servicosAtivo ? servicoService.listarAtivos() : java.util.List.of());
        if (!model.containsAttribute("agendaForm")) {
            Long clienteSelecionada = null;
            if (clienteId != null) {
                clienteSelecionada = clienteRepository.findByIdAndEmpresaId(clienteId, TenantContext.getRequired())
                        .filter(c -> c.isAtivo())
                        .map(c -> c.getId())
                        .orElse(null);
            }
            model.addAttribute("agendaForm", new AgendaRequests.CriarAgendamentoRequest(
                    clienteSelecionada, null, null, null,
                    selecionada.equals(LocalDate.now()) ? LocalDateTime.now().plusHours(1).withSecond(0).withNano(0) : selecionada.atTime(9,0),
                    selecionada.equals(LocalDate.now()) ? LocalDateTime.now().plusHours(2).withSecond(0).withNano(0) : selecionada.atTime(10,0), null));
        }
        return "pages/agenda";
    }

    @PostMapping("/agenda")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String criar(@Valid @ModelAttribute("agendaForm") AgendaRequests.CriarAgendamentoRequest form,
                        BindingResult bindingResult, Model model, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        LocalDate data = form.dataHoraInicio() == null ? LocalDate.now() : form.dataHoraInicio().toLocalDate();
        if (bindingResult.hasErrors()) {
            prepararModelo(model, data);
            return "pages/agenda";
        }
        try {
            agendaService.criar(form);
            ra.addFlashAttribute("sucesso", "Agendamento criado com sucesso.");
            return "redirect:/agenda?data=" + data;
        } catch (RuntimeException ex) {
            prepararModelo(model, data);
            model.addAttribute("erro", mensagemSegura(ex));
            return "pages/agenda";
        }
    }

    @GetMapping("/agenda/{id}/editar")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String editar(@PathVariable Long id, Model model) {
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        try {
            var agendamento = agendaService.buscar(id);
            if (agendamento.getStatus() != StatusAgendamento.AGENDADO
                    && agendamento.getStatus() != StatusAgendamento.CONFIRMADO) {
                throw new RegraNegocioException("Somente agendamentos aguardando atendimento podem ser alterados.");
            }
            if (!model.containsAttribute("agendaEdicao")) {
                model.addAttribute("agendaEdicao", new AgendaRequests.EditarAgendamentoRequest(
                        agendamento.getTipo(), agendamento.getServico() == null ? null : agendamento.getServico().getId(),
                        agendamento.getDataHoraInicio(), agendamento.getDataHoraFim(), agendamento.getObservacoes()));
            }
            model.addAttribute("agendamento", agendamento);
            model.addAttribute("tipos", TipoAgendamento.values());
            boolean servicosAtivo = moduloAcessoService.possui(ModuloSistema.SERVICOS);
            model.addAttribute("servicosAtivo", servicosAtivo);
            model.addAttribute("servicos", servicosAtivo ? servicoService.listarAtivos() : java.util.List.of());
            return "pages/agenda-editar";
        } catch (RuntimeException ex) {
            model.addAttribute("erro", mensagemSegura(ex));
            return "redirect:/agenda";
        }
    }

    @PostMapping("/agenda/{id}/editar")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String salvarEdicao(@PathVariable Long id,
                               @Valid @ModelAttribute("agendaEdicao") AgendaRequests.EditarAgendamentoRequest form,
                               BindingResult bindingResult, Model model, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        if (bindingResult.hasErrors()) {
            model.addAttribute("agendamento", agendaService.buscar(id));
            model.addAttribute("tipos", TipoAgendamento.values());
            boolean servicosAtivo = moduloAcessoService.possui(ModuloSistema.SERVICOS);
            model.addAttribute("servicosAtivo", servicosAtivo);
            model.addAttribute("servicos", servicosAtivo ? servicoService.listarAtivos() : java.util.List.of());
            return "pages/agenda-editar";
        }
        try {
            var atualizado = agendaService.reagendar(id, form);
            ra.addFlashAttribute("sucesso", "Agendamento atualizado com sucesso.");
            return "redirect:/agenda?data=" + atualizado.getDataHoraInicio().toLocalDate();
        } catch (RuntimeException ex) {
            model.addAttribute("agendamento", agendaService.buscar(id));
            model.addAttribute("tipos", TipoAgendamento.values());
            boolean servicosAtivo = moduloAcessoService.possui(ModuloSistema.SERVICOS);
            model.addAttribute("servicosAtivo", servicosAtivo);
            model.addAttribute("servicos", servicosAtivo ? servicoService.listarAtivos() : java.util.List.of());
            model.addAttribute("erro", mensagemSegura(ex));
            return "pages/agenda-editar";
        }
    }

    @PostMapping("/agenda/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String status(@PathVariable Long id, @RequestParam StatusAgendamento status,
                         @RequestParam(required = false) LocalDate data, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        try {
            agendaService.alterarStatus(id, status);
            ra.addFlashAttribute("sucesso", "Status do agendamento atualizado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/agenda?data=" + (data == null ? LocalDate.now() : data);
    }

    private void prepararModelo(Model model, LocalDate data) {
        LocalDate inicioSemana = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fimSemana = inicioSemana.plusDays(6);
        model.addAttribute("dataSelecionada", data);
        model.addAttribute("inicioSemana", inicioSemana);
        model.addAttribute("fimSemana", fimSemana);
        model.addAttribute("agendamentos", agendaService.listarPeriodo(inicioSemana, fimSemana));
        model.addAttribute("clientes", clienteRepository.findByEmpresaIdAndAtivoTrue(TenantContext.getRequired(),
                org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by("nome").ascending())).getContent());
        model.addAttribute("profissionais", usuarioRepository.findProfissionaisAtivos(TenantContext.getRequired()));
        model.addAttribute("tipos", TipoAgendamento.values());
        model.addAttribute("status", StatusAgendamento.values());
        model.addAttribute("megaHairAtivo", moduloAcessoService.possui(ModuloSistema.MEGA_HAIR));
        boolean servicosAtivo = moduloAcessoService.possui(ModuloSistema.SERVICOS);
        model.addAttribute("servicosAtivo", servicosAtivo);
        model.addAttribute("servicos", servicosAtivo ? servicoService.listarAtivos() : java.util.List.of());
    }

    private String mensagemSegura(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? "Não foi possível concluir a operação." : ex.getMessage();
    }
}
