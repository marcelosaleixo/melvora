package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.entity.*;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.repository.*;
import com.marceloaleixo.melvora.dto.ComunicacaoRequests;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComunicacaoAutomacaoService {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private final EmpresaRepository empresaRepository;
    private final EmpresaModuloRepository empresaModuloRepository;
    private final ConfiguracaoComunicacaoRepository configRepository;
    private final ComunicacaoAgendadaRepository comunicacaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final TemplateWhatsAppRepository templateRepository;

    public ComunicacaoAutomacaoService(EmpresaRepository empresaRepository,
                                       EmpresaModuloRepository empresaModuloRepository,
                                       ConfiguracaoComunicacaoRepository configRepository,
                                       ComunicacaoAgendadaRepository comunicacaoRepository,
                                       AgendamentoRepository agendamentoRepository,
                                       TemplateWhatsAppRepository templateRepository) {
        this.empresaRepository = empresaRepository; this.empresaModuloRepository = empresaModuloRepository;
        this.configRepository = configRepository; this.comunicacaoRepository = comunicacaoRepository;
        this.agendamentoRepository = agendamentoRepository; this.templateRepository = templateRepository;
    }

    @Scheduled(fixedDelayString = "PT5M")
    @Transactional
    public void processarFila() {
        LocalDateTime agora = LocalDateTime.now();
        for (Empresa empresa : empresaRepository.findByAtivaTrue()) {
            if (!empresaModuloRepository.findByEmpresaIdAndModulo(empresa.getId(), ModuloSistema.COMUNICACAO).map(EmpresaModulo::isAtivo).orElse(false)) continue;
            ConfiguracaoComunicacao cfg = configRepository.findByEmpresaId(empresa.getId()).orElseGet(() -> configRepository.save(new ConfiguracaoComunicacao(empresa)));
            List<Agendamento> proximos = agendamentoRepository.listarProximosDaEmpresa(empresa.getId(), agora.minusMinutes(1), agora.plusDays(14), List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO), org.springframework.data.domain.PageRequest.of(0, 200));
            for (Agendamento a : proximos) {
                if (cfg.isConfirmacaoAtiva() && a.getDataHoraInicio().minusMinutes(cfg.getConfirmacaoMinutosAntes()).isBefore(agora.plusSeconds(1))) criar(a, ComunicacaoAgendada.Tipo.CONFIRMACAO, a.getDataHoraInicio().minusMinutes(cfg.getConfirmacaoMinutosAntes()), "Confirmação de agendamento");
                if (cfg.isLembreteAtivo() && a.getDataHoraInicio().minusMinutes(cfg.getLembreteMinutosAntes()).isBefore(agora.plusSeconds(1))) criar(a, ComunicacaoAgendada.Tipo.LEMBRETE, a.getDataHoraInicio().minusMinutes(cfg.getLembreteMinutosAntes()), "Lembrete de atendimento");
            }
            if (cfg.isPosAtendimentoAtivo()) {
                List<Agendamento> concluidos = agendamentoRepository.listarRecentesConcluidosDaEmpresa(empresa.getId(), agora.minusDays(2), agora);
                for (Agendamento a : concluidos) {
                    LocalDateTime quando = a.getDataHoraFim().plusMinutes(cfg.getPosAtendimentoMinutosDepois());
                    if (!quando.isAfter(agora)) criar(a, ComunicacaoAgendada.Tipo.POS_ATENDIMENTO, quando, "Pós-atendimento");
                }
            }
        }
    }

    private void criar(Agendamento a, ComunicacaoAgendada.Tipo tipo, LocalDateTime quando, String templateNome) {
        if (comunicacaoRepository.existsByAgendamentoIdAndTipo(a.getId(), tipo)) return;
        TemplateWhatsApp template = templateRepository.findByEmpresaIdAndNomeIgnoreCase(a.getEmpresa().getId(), templateNome).orElse(null);
        if (template == null || !template.isAtivo()) return;
        String mensagem = template.getMensagem();
        String servico = a.getServico() != null ? a.getServico().getNome() : a.getTipo().getNome();
        mensagem = mensagem.replace("{cliente}", a.getCliente().getNome())
                .replace("{empresa}", a.getEmpresa().getNomeFantasia())
                .replace("{servico}", servico)
                .replace("{data}", DATA.format(a.getDataHoraInicio()))
                .replace("{hora}", HORA.format(a.getDataHoraInicio()))
                .replace("{profissional}", a.getProfissional().getNome());
        ComunicacaoAgendada c = new ComunicacaoAgendada(a.getEmpresa(), a, tipo, quando, mensagem, montarLink(a.getCliente().getTelefone(), mensagem));
        if (a.getCliente().getTelefone() == null || a.getCliente().getTelefone().isBlank()) c.cancelar();
        comunicacaoRepository.save(c);
    }

    private String montarLink(String telefone, String mensagem) {
        if (telefone == null || telefone.isBlank()) return null;
        String numero = telefone.replaceAll("\\D", "");
        if (numero.length() == 10 || numero.length() == 11) numero = "55" + numero;
        if (!numero.startsWith("55") || numero.length() < 12 || numero.length() > 13) return null;
        return "https://wa.me/" + numero + "?text=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Transactional(readOnly = true)
    public List<ComunicacaoAgendada> fila(Long empresaId, org.springframework.data.domain.Pageable pageable) {
        return comunicacaoRepository.findByEmpresaIdOrderByDataHoraEnvioAsc(empresaId, pageable);
    }

    @Transactional(readOnly = true)
    public ConfiguracaoComunicacao configuracao(Long empresaId, Empresa empresa) {
        return configRepository.findByEmpresaId(empresaId).orElseGet(() -> new ConfiguracaoComunicacao(empresa));
    }

    @Transactional
    public void salvarConfiguracao(Long empresaId, ComunicacaoRequests.ConfigForm form, Empresa empresa) {
        ConfiguracaoComunicacao cfg = configRepository.findByEmpresaId(empresaId).orElseGet(() -> new ConfiguracaoComunicacao(empresa));
        cfg.atualizar(form.confirmacaoAtiva(), form.confirmacaoMinutosAntes(), form.lembreteAtivo(), form.lembreteMinutosAntes(), form.posAtendimentoAtivo(), form.posAtendimentoMinutosDepois());
        configRepository.save(cfg);
    }

    @Transactional
    public String abrir(Long id, Long empresaId) {
        ComunicacaoAgendada c = comunicacaoRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(() -> new IllegalArgumentException("Comunicação não encontrada."));
        if (c.getStatus() == ComunicacaoAgendada.Status.CANCELADA || c.getWhatsappUrl() == null) throw new IllegalArgumentException("Esta comunicação não pode ser aberta no WhatsApp.");
        c.marcarAberta();
        return c.getWhatsappUrl();
    }
}
