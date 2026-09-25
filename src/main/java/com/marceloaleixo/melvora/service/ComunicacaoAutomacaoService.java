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
    private final AtendimentoRepository atendimentoRepository;
    private final TemplateWhatsAppRepository templateRepository;
    private final WhatsAppConfirmacaoPendenteRepository confirmacaoRepository;
    private final AvaliacaoAtendimentoService avaliacaoAtendimentoService;
    private final PreferenciaComunicacaoContatoRepository preferenciaRepository;
    private final RetencaoEnvioRepository retencaoEnvioRepository;
    private final WhatsAppBusinessService whatsAppBusinessService;
    private final ClienteRepository clienteRepository;

    public ComunicacaoAutomacaoService(EmpresaRepository empresaRepository,
                                       EmpresaModuloRepository empresaModuloRepository,
                                       ConfiguracaoComunicacaoRepository configRepository,
                                       ComunicacaoAgendadaRepository comunicacaoRepository,
                                       AgendamentoRepository agendamentoRepository,
                                       AtendimentoRepository atendimentoRepository,
                                       TemplateWhatsAppRepository templateRepository,
                                       WhatsAppConfirmacaoPendenteRepository confirmacaoRepository,
                                       AvaliacaoAtendimentoService avaliacaoAtendimentoService,
                                       PreferenciaComunicacaoContatoRepository preferenciaRepository,
                                       RetencaoEnvioRepository retencaoEnvioRepository,
                                       WhatsAppBusinessService whatsAppBusinessService,
                                       ClienteRepository clienteRepository) {
        this.empresaRepository = empresaRepository; this.empresaModuloRepository = empresaModuloRepository;
        this.configRepository = configRepository; this.comunicacaoRepository = comunicacaoRepository;
        this.agendamentoRepository = agendamentoRepository; this.atendimentoRepository = atendimentoRepository; this.templateRepository = templateRepository;
        this.confirmacaoRepository = confirmacaoRepository;
        this.avaliacaoAtendimentoService = avaliacaoAtendimentoService;
        this.preferenciaRepository = preferenciaRepository;
        this.retencaoEnvioRepository = retencaoEnvioRepository;
        this.whatsAppBusinessService = whatsAppBusinessService;
        this.clienteRepository = clienteRepository;
    }

    @Scheduled(fixedDelayString = "PT10M")
    @Transactional
    public void processarRetencaoAutomatica() {
        LocalDateTime agora = LocalDateTime.now();
        java.time.LocalTime hora = agora.toLocalTime();
        for (Empresa empresa : empresaRepository.findByAtivaTrue()) {
            Long empresaId = empresa.getId();
            if (!empresaModuloRepository.findByEmpresaIdAndModulo(empresaId, ModuloSistema.COMUNICACAO).map(EmpresaModulo::isAtivo).orElse(false)) continue;
            ConfiguracaoComunicacao cfg = configRepository.findByEmpresaId(empresaId).orElse(null);
            if (cfg == null || !cfg.isRetencaoAtiva() || !cfg.isRetencaoAutomaticaAtiva()) continue;
            if (hora.isBefore(cfg.getRetencaoHorarioInicio()) || !hora.isBefore(cfg.getRetencaoHorarioFim())) continue;
            LocalDateTime inicioDia = agora.toLocalDate().atStartOfDay();
            LocalDateTime fimDia = inicioDia.plusDays(1);
            long enviadosHoje = retencaoEnvioRepository.countByEmpresaIdAndStatusAndDataHoraEnvioGreaterThanEqualAndDataHoraEnvioLessThan(empresaId, RetencaoEnvio.Status.ENVIADA, inicioDia, fimDia);
            if (enviadosHoje >= cfg.getRetencaoMaxEnviosDia()) continue;
            var oportunidades = oportunidadesRetencao(empresaId, cfg.getRetencaoDiasSemRetorno(), org.springframework.data.domain.PageRequest.of(0, 100));
            for (var oportunidade : oportunidades) {
                if (enviadosHoje >= cfg.getRetencaoMaxEnviosDia()) break;
                if (oportunidade.telefone() == null || oportunidade.telefone().isBlank() || !oportunidade.retencaoOptIn() || oportunidade.retencaoOptOut()) continue;
                var ultima = retencaoEnvioRepository.findFirstByEmpresaIdAndClienteIdAndStatusOrderByDataHoraEnvioDesc(empresaId, oportunidade.clienteId(), RetencaoEnvio.Status.ENVIADA).orElse(null);
                if (ultima != null && ultima.getDataHoraEnvio().plusDays(cfg.getRetencaoCooldownDias()).isAfter(agora)) continue;
                Cliente cliente = clienteRepositoryPorTenant(empresaId, oportunidade.clienteId());
                if (cliente == null) continue;
                String mensagem = cfg.getRetencaoMensagem()
                        .replace("{cliente}", oportunidade.clienteNome())
                        .replace("{dias}", String.valueOf(oportunidade.diasSemRetorno()))
                        .replace("{ultima_data}", DATA.format(oportunidade.ultimoAtendimento()));
                RetencaoEnvio envio = new RetencaoEnvio(empresa, cliente, oportunidade.telefone(), mensagem, RetencaoEnvio.Status.ERRO);
                try {
                    String providerId = whatsAppBusinessService.enviarMensagemAutomaticaInterna(empresaId, cliente, oportunidade.telefone(), mensagem);
                    envio.marcarEnviada(providerId);
                    enviadosHoje++;
                } catch (Exception ex) {
                    envio.marcarErro(ex.getMessage());
                }
                retencaoEnvioRepository.save(envio);
            }
        }
    }

    private Cliente clienteRepositoryPorTenant(Long empresaId, Long clienteId) {
        return clienteRepository.findByIdAndEmpresaId(clienteId, empresaId).orElse(null);
    }

    @Scheduled(fixedDelayString = "PT1M")
    @Transactional
    public void expirarConfirmacoes() {
        var expiradas = confirmacaoRepository.listarExpiradas(WhatsAppConfirmacaoPendente.Status.AGUARDANDO_RESPOSTA, LocalDateTime.now());
        expiradas.forEach(WhatsAppConfirmacaoPendente::expirar);
        if (!expiradas.isEmpty()) confirmacaoRepository.saveAll(expiradas);
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
                if (cfg.isConfirmacaoAtiva() && a.getStatus() == StatusAgendamento.AGENDADO
                        && a.getDataHoraInicio().minusMinutes(cfg.getConfirmacaoMinutosAntes()).isBefore(agora.plusSeconds(1))) {
                    criar(a, ComunicacaoAgendada.Tipo.CONFIRMACAO, a.getDataHoraInicio().minusMinutes(cfg.getConfirmacaoMinutosAntes()), "Confirmação de agendamento");
                }
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
        if (tipo == ComunicacaoAgendada.Tipo.CONFIRMACAO && a.getStatus() != StatusAgendamento.AGENDADO) return;
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
        if (tipo == ComunicacaoAgendada.Tipo.POS_ATENDIMENTO) {
            var atendimento = a.getId() == null ? null : atendimentoRepository.findByAgendamentoIdAndEmpresaId(a.getId(), a.getEmpresa().getId()).orElse(null);
            if (atendimento == null) return;
            String avaliacaoLink = avaliacaoAtendimentoService.linkPublico(
                    avaliacaoAtendimentoService.garantirParaAtendimento(atendimento));
            if (mensagem.contains("{avaliacao_link}")) {
                mensagem = mensagem.replace("{avaliacao_link}", avaliacaoLink);
            } else {
                mensagem = mensagem + "\n\nComo foi sua experiência? 💜\n" + avaliacaoLink;
            }
        }
        ComunicacaoAgendada c = new ComunicacaoAgendada(a.getEmpresa(), a, tipo, quando, mensagem, montarLink(a.getCliente().getTelefone(), mensagem));
        if (a.getCliente().getTelefone() == null || a.getCliente().getTelefone().isBlank()) {
            c.cancelar();
        }
        comunicacaoRepository.save(c);
        if (tipo == ComunicacaoAgendada.Tipo.CONFIRMACAO && a.getCliente().getTelefone() != null && !a.getCliente().getTelefone().isBlank()) {
            confirmacaoRepository.findAtiva(a.getEmpresa().getId(), a.getCliente().getTelefone(),
                    List.of(WhatsAppConfirmacaoPendente.Status.AGUARDANDO_RESPOSTA), LocalDateTime.now())
                    .ifPresent(existing -> { existing.recusar(); confirmacaoRepository.save(existing); });
            confirmacaoRepository.save(new WhatsAppConfirmacaoPendente(a.getEmpresa(), a.getCliente(), a.getId(),
                    a.getCliente().getTelefone(), a.getDataHoraInicio()));
        }
    }

    private String montarLink(String telefone, String mensagem) {
        if (telefone == null || telefone.isBlank()) return null;
        String numero = telefone.replaceAll("\\D", "");
        if (numero.length() == 10 || numero.length() == 11) numero = "55" + numero;
        if (!numero.startsWith("55") || numero.length() < 12 || numero.length() > 13) return null;
        return "https://wa.me/" + numero + "?text=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Transactional(readOnly = true)
    public List<com.marceloaleixo.melvora.dto.RetencaoData.Oportunidade> oportunidadesRetencao(Long empresaId, int diasSemRetorno, org.springframework.data.domain.Pageable pageable) {
        LocalDateTime limite = LocalDateTime.now().minusDays(diasSemRetorno);
        return atendimentoRepository.buscarOportunidadesRetencaoV2(empresaId, limite, pageable).stream().map(row -> {
            LocalDateTime ultimo = (LocalDateTime) row[3];
            long dias = java.time.temporal.ChronoUnit.DAYS.between(ultimo.toLocalDate(), LocalDateTime.now().toLocalDate());
            String telefone = (String) row[2];
            String telefoneCanonical = telefone == null ? null : telefone.replaceAll("\\D", "");
            if (telefoneCanonical != null && (telefoneCanonical.length() == 10 || telefoneCanonical.length() == 11)) telefoneCanonical = "55" + telefoneCanonical;
            var pref = telefoneCanonical == null ? java.util.Optional.<PreferenciaComunicacaoContato>empty() : preferenciaRepository.findByEmpresaIdAndTelefone(empresaId, telefoneCanonical);
            return new com.marceloaleixo.melvora.dto.RetencaoData.Oportunidade(
                    ((Number) row[0]).longValue(), (String) row[1], telefone, ultimo,
                    ((Number) row[4]).longValue(), Math.max(0, dias),
                    pref.map(PreferenciaComunicacaoContato::isRetencaoOptIn).orElse(false),
                    pref.map(p -> !p.isRetencaoOptIn() && p.getRetencaoOptOutAt() != null).orElse(false));
        }).toList();
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
        cfg.atualizar(form.confirmacaoAtiva(), form.confirmacaoMinutosAntes(), form.lembreteAtivo(), form.lembreteMinutosAntes(), form.posAtendimentoAtivo(), form.posAtendimentoMinutosDepois(), form.retencaoAtiva(), form.retencaoDiasSemRetorno(), form.retencaoMensagem(), form.retencaoAutomaticaAtiva(), form.retencaoCooldownDias(), form.retencaoHorarioInicio(), form.retencaoHorarioFim(), form.retencaoMaxEnviosDia());
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
