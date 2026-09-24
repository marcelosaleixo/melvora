package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.entity.Agendamento;
import com.marceloaleixo.melvora.entity.Cliente;
import com.marceloaleixo.melvora.entity.ConfiguracaoWhatsAppAutomacao;
import com.marceloaleixo.melvora.entity.Servico;
import com.marceloaleixo.melvora.entity.WhatsAppMensagem;
import com.marceloaleixo.melvora.entity.WhatsAppReservaPendente;
import com.marceloaleixo.melvora.entity.WhatsAppAgendamentoAcaoPendente;
import com.marceloaleixo.melvora.entity.WhatsAppConfirmacaoPendente;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.entity.enums.WhatsAppIntencao;
import com.marceloaleixo.melvora.repository.AgendamentoRepository;
import com.marceloaleixo.melvora.repository.ConfiguracaoWhatsAppAutomacaoRepository;
import com.marceloaleixo.melvora.repository.ServicoRepository;
import com.marceloaleixo.melvora.repository.WhatsAppMensagemRepository;
import com.marceloaleixo.melvora.repository.WhatsAppReservaPendenteRepository;
import com.marceloaleixo.melvora.repository.WhatsAppAgendamentoAcaoPendenteRepository;
import com.marceloaleixo.melvora.repository.WhatsAppConfirmacaoPendenteRepository;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppAutomacaoService {
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final List<StatusAgendamento> ATIVOS = List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO, StatusAgendamento.EM_ATENDIMENTO);

    private final WhatsAppMensagemRepository mensagemRepository;
    private final ConfiguracaoWhatsAppAutomacaoRepository configRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final WhatsAppBusinessService whatsAppBusinessService;
    private final AgendaDisponibilidadeService disponibilidadeService;
    private final WhatsAppReservaPendenteRepository reservaRepository;
    private final WhatsAppAgendamentoAcaoPendenteRepository acaoRepository;
    private final WhatsAppConfirmacaoPendenteRepository confirmacaoRepository;
    private final AgendaService agendaService;
    private final ObjectMapper objectMapper;

    public WhatsAppAutomacaoService(WhatsAppMensagemRepository mensagemRepository,
                                    ConfiguracaoWhatsAppAutomacaoRepository configRepository,
                                    ServicoRepository servicoRepository,
                                    AgendamentoRepository agendamentoRepository,
                                    WhatsAppBusinessService whatsAppBusinessService,
                                    AgendaDisponibilidadeService disponibilidadeService,
                                    WhatsAppReservaPendenteRepository reservaRepository,
                                    WhatsAppAgendamentoAcaoPendenteRepository acaoRepository,
                                    WhatsAppConfirmacaoPendenteRepository confirmacaoRepository,
                                    AgendaService agendaService,
                                    ObjectMapper objectMapper) {
        this.mensagemRepository = mensagemRepository;
        this.configRepository = configRepository;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.whatsAppBusinessService = whatsAppBusinessService;
        this.disponibilidadeService = disponibilidadeService;
        this.reservaRepository = reservaRepository;
        this.acaoRepository = acaoRepository;
        this.confirmacaoRepository = confirmacaoRepository;
        this.agendaService = agendaService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "PT5M")
    @Transactional
    public void expirarAcoesPendentes() {
        var expiradas = acaoRepository.listarExpiradas(
                List.of(WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_CONFIRMACAO_CANCELAMENTO,
                        WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_DATA_REAGENDAMENTO,
                        WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_HORARIO_REAGENDAMENTO,
                        WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_CONFIRMACAO_REAGENDAMENTO),
                LocalDateTime.now());
        expiradas.forEach(WhatsAppAgendamentoAcaoPendente::expirar);
        if (!expiradas.isEmpty()) acaoRepository.saveAll(expiradas);
    }

    @Scheduled(fixedDelayString = "PT1M")
    @Transactional
    public void expirarConfirmacoes() {
        var expiradas = confirmacaoRepository.listarExpiradas(WhatsAppConfirmacaoPendente.Status.AGUARDANDO_RESPOSTA, LocalDateTime.now());
        expiradas.forEach(WhatsAppConfirmacaoPendente::expirar);
        if (!expiradas.isEmpty()) confirmacaoRepository.saveAll(expiradas);
    }

    @Scheduled(fixedDelayString = "PT30S")
    @Transactional
    public void processarFila() {
        var pagina = mensagemRepository.listarPendentesAutomacao(WhatsAppMensagem.Direcao.ENTRADA, PageRequest.of(0, 20));
        for (WhatsAppMensagem mensagem : pagina.getContent()) {
            processar(mensagem);
        }
    }

    private void processar(WhatsAppMensagem mensagem) {
        Long empresaId = mensagem.getEmpresa().getId();
        ConfiguracaoWhatsAppAutomacao cfg = configRepository.findByEmpresaId(empresaId).orElse(null);
        if (cfg == null || !cfg.isAtiva()) {
            mensagem.registrarProcessamentoAutomacao(classificar(mensagem.getMensagem()), false);
            return;
        }

        WhatsAppConfirmacaoPendente confirmacao = confirmacaoRepository.findAtiva(empresaId, mensagem.getTelefone(),
                List.of(WhatsAppConfirmacaoPendente.Status.AGUARDANDO_RESPOSTA), LocalDateTime.now()).orElse(null);
        if (confirmacao != null) {
            boolean enviada = processarConfirmacao(confirmacao, mensagem);
            mensagem.registrarProcessamentoAutomacao(WhatsAppIntencao.AGENDAMENTO, enviada);
            return;
        }

        WhatsAppReservaPendente reserva = reservaRepository.findAtiva(empresaId, mensagem.getTelefone(),
                List.of(WhatsAppReservaPendente.Status.AGUARDANDO_HORARIO, WhatsAppReservaPendente.Status.AGUARDANDO_CONFIRMACAO),
                LocalDateTime.now()).orElse(null);
        if (reserva != null) {
            boolean enviada = processarReserva(reserva, mensagem);
            mensagem.registrarProcessamentoAutomacao(WhatsAppIntencao.AGENDAMENTO, enviada);
            return;
        }

        WhatsAppAgendamentoAcaoPendente acao = acaoRepository.findAtiva(empresaId, mensagem.getTelefone(),
                List.of(WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_CONFIRMACAO_CANCELAMENTO,
                        WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_DATA_REAGENDAMENTO,
                        WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_HORARIO_REAGENDAMENTO,
                        WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_CONFIRMACAO_REAGENDAMENTO),
                LocalDateTime.now()).orElse(null);
        if (acao != null) {
            boolean enviada = processarAcaoAgendamento(acao, mensagem);
            mensagem.registrarProcessamentoAutomacao(acao.getAcao() == WhatsAppAgendamentoAcaoPendente.Acao.CANCELAR
                    ? WhatsAppIntencao.CANCELAMENTO : WhatsAppIntencao.REMARCACAO, enviada);
            return;
        }

        WhatsAppIntencao intencao = classificar(mensagem.getMensagem());
        if (intencao == WhatsAppIntencao.CANCELAMENTO || intencao == WhatsAppIntencao.REMARCACAO) {
            boolean enviada = iniciarAcaoAgendamento(intencao, mensagem);
            mensagem.registrarProcessamentoAutomacao(intencao, enviada);
            return;
        }
        String resposta = gerarResposta(cfg, mensagem, intencao);
        boolean enviada = false;
        if (resposta != null && !resposta.isBlank()) {
            try {
                whatsAppBusinessService.enviarMensagemAutomatica(empresaId, mensagem.getCliente(), mensagem.getTelefone(), resposta);
                enviada = true;
            } catch (RuntimeException ignored) {
                // A mensagem continua marcada como processada para evitar loop de envio.
            }
        }
        mensagem.registrarProcessamentoAutomacao(intencao, enviada);
    }

    private boolean processarConfirmacao(WhatsAppConfirmacaoPendente confirmacao, WhatsAppMensagem mensagem) {
        String texto = normalizar(mensagem.getMensagem());
        try {
            if (texto.matches("^(sim|s|confirmar|confirmo|pode|ok|okay|1|confirmado)$")) {
                Agendamento a = agendaService.confirmarViaWhatsApp(mensagem.getEmpresa().getId(), confirmacao.getAgendamentoId());
                confirmacao.confirmar();
                confirmacaoRepository.save(confirmacao);
                return enviar(mensagem, "Pronto! 💖 Seu atendimento está confirmado para "
                        + a.getDataHoraInicio().format(DATA_HORA) + ".\n\n"
                        + nomeServico(a) + "\nProfissional: " + a.getProfissional().getNome() + "\n\nEsperamos você! ✨");
            }
            if (texto.matches("^(nao|não|n|nao quero|não quero)$")) {
                confirmacao.recusar();
                confirmacaoRepository.save(confirmacao);
                return enviar(mensagem, "Tudo bem! 💖 Seu horário continua reservado. Se precisar remarcar ou cancelar, é só me avisar.");
            }
            if (texto.matches("^(2|remarcar|quero remarcar|mudar)$")) {
                confirmacao.recusar();
                confirmacaoRepository.save(confirmacao);
                return iniciarAcaoAgendamento(WhatsAppIntencao.REMARCACAO, mensagem);
            }
            if (texto.matches("^(3|cancelar|quero cancelar|desmarcar)$")) {
                confirmacao.recusar();
                confirmacaoRepository.save(confirmacao);
                return iniciarAcaoAgendamento(WhatsAppIntencao.CANCELAMENTO, mensagem);
            }
            return enviar(mensagem, "Responda 1️⃣ para confirmar, 2️⃣ para remarcar ou 3️⃣ para cancelar. 💖");
        } catch (RuntimeException ex) {
            return enviar(mensagem, "Não consegui atualizar esse atendimento porque ele pode ter sido alterado. 💬 Vou deixar sua solicitação para nossa equipe verificar.");
        }
    }

    private boolean iniciarAcaoAgendamento(WhatsAppIntencao intencao, WhatsAppMensagem mensagem) {
        if (mensagem.getCliente() == null) {
            return enviar(mensagem, "Não consegui identificar seu cadastro. 💬 Vou encaminhar você para nossa equipe para localizar seu atendimento.");
        }
        Long empresaId = mensagem.getEmpresa().getId();
        List<Agendamento> proximos = agendamentoRepository.listarProximosDaCliente(
                empresaId, mensagem.getCliente().getId(), LocalDateTime.now(), ATIVOS, PageRequest.of(0, 1));
        if (proximos.isEmpty()) {
            return enviar(mensagem, intencao == WhatsAppIntencao.CANCELAMENTO
                    ? "Não encontrei um próximo atendimento ativo para cancelar. 💖 Se precisar, nossa equipe pode ajudar."
                    : "Não encontrei um próximo atendimento ativo para remarcar. 💖 Se quiser agendar um novo horário, me diga o serviço e a data.");
        }
        Agendamento agendamento = proximos.get(0);
        if (intencao == WhatsAppIntencao.CANCELAMENTO) {
            acaoRepository.save(WhatsAppAgendamentoAcaoPendente.cancelar(
                    mensagem.getEmpresa(), mensagem.getCliente(), agendamento.getId(), mensagem.getTelefone(), LocalDateTime.now().plusMinutes(15)));
            String servico = nomeServico(agendamento);
            return enviar(mensagem, "Encontrei seu próximo atendimento: 💖\n\n" + servico +
                    "\n" + agendamento.getDataHoraInicio().format(DATA_HORA) +
                    "\nProfissional: " + agendamento.getProfissional().getNome() +
                    "\n\nDeseja realmente cancelar este atendimento? Responda SIM para confirmar ou NÃO para manter o horário.");
        }

        WhatsAppAgendamentoAcaoPendente acao = WhatsAppAgendamentoAcaoPendente.remarcar(
                mensagem.getEmpresa(), mensagem.getCliente(), agendamento.getId(), mensagem.getTelefone(), LocalDateTime.now().plusMinutes(15));
        acaoRepository.save(acao);
        LocalDate data = extrairData(normalizar(mensagem.getMensagem()));
        if (data != null) {
            return prepararOpcoesRemarcacao(acao, agendamento, data, mensagem);
        }
        return enviar(mensagem, "Claro! 😊 Encontrei seu atendimento de " +
                agendamento.getDataHoraInicio().format(DATA_HORA) + ".\n\nPara qual data você deseja remarcar? Exemplo: 30/09 ou amanhã.");
    }

    private boolean processarAcaoAgendamento(WhatsAppAgendamentoAcaoPendente acao, WhatsAppMensagem mensagem) {
        String texto = normalizar(mensagem.getMensagem());
        try {
            if (contem(texto, "cancelar", "desistir") && acao.getStatus() != WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_CONFIRMACAO_CANCELAMENTO) {
                acao.cancelar();
                acaoRepository.save(acao);
                return enviar(mensagem, "Tudo bem! 💖 A solicitação foi cancelada e o atendimento continua como está.");
            }
            Agendamento agendamento = agendamentoRepository.findByIdAndEmpresaId(acao.getAgendamentoId(), mensagem.getEmpresa().getId())
                    .orElseThrow(() -> new IllegalStateException("Agendamento não encontrado."));

            if (acao.getStatus() == WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_CONFIRMACAO_CANCELAMENTO) {
                if (ehNao(texto)) {
                    acao.cancelar();
                    acaoRepository.save(acao);
                    return enviar(mensagem, "Perfeito! 💖 Seu atendimento continua confirmado para " +
                            agendamento.getDataHoraInicio().format(DATA_HORA) + ".");
                }
                if (!ehConfirmacao(texto)) {
                    return enviar(mensagem, "Para cancelar, responda SIM. Para manter o horário, responda NÃO.");
                }
                agendaService.cancelarViaWhatsApp(mensagem.getEmpresa().getId(), agendamento.getId());
                acao.concluir();
                acaoRepository.save(acao);
                return enviar(mensagem, "Pronto! 💖 Seu atendimento de " + agendamento.getDataHoraInicio().format(DATA_HORA) +
                        " foi cancelado com sucesso.");
            }

            if (acao.getStatus() == WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_DATA_REAGENDAMENTO) {
                LocalDate data = extrairData(texto);
                if (data == null) {
                    return enviar(mensagem, "Não consegui identificar a data. Informe, por exemplo: 30/09 ou amanhã.");
                }
                return prepararOpcoesRemarcacao(acao, agendamento, data, mensagem);
            }

            if (acao.getStatus() == WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_HORARIO_REAGENDAMENTO) {
                String opcao = encontrarOpcao(acao.getOpcoesJson(), texto);
                if (opcao == null) {
                    return enviar(mensagem, "Não consegui identificar o horário. Responda com o número da opção ou com o horário, por exemplo: 14:00.");
                }
                String[] p = opcao.split("\\|", -1);
                LocalDateTime inicio = LocalDateTime.of(acao.getDataDesejada(), java.time.LocalTime.parse(p[0]));
                LocalDateTime fim = LocalDateTime.parse(p[2]);
                acao.selecionarHorario(Long.valueOf(p[1]), inicio, fim);
                acaoRepository.save(acao);
                return enviar(mensagem, "Perfeito! 😊\n\nNovo horário:\n" +
                        nomeServico(agendamento) + "\n" + inicio.format(DATA_HORA) +
                        "\nProfissional: " + p[3] +
                        "\n\nDeseja confirmar a remarcação? Responda SIM para confirmar ou NÃO para cancelar.");
            }

            if (acao.getStatus() == WhatsAppAgendamentoAcaoPendente.Status.AGUARDANDO_CONFIRMACAO_REAGENDAMENTO) {
                if (ehNao(texto)) {
                    acao.cancelar();
                    acaoRepository.save(acao);
                    return enviar(mensagem, "Tudo bem! 💖 A remarcação foi cancelada e seu horário atual continua mantido.");
                }
                if (!ehConfirmacao(texto)) {
                    return enviar(mensagem, "Para confirmar a remarcação, responda SIM. Para manter o horário atual, responda NÃO.");
                }
                Agendamento atualizado = agendaService.reagendarViaWhatsApp(mensagem.getEmpresa().getId(), agendamento.getId(),
                        acao.getProfissionalId(), acao.getInicioSelecionado(), acao.getFimSelecionado());
                acao.concluir();
                acaoRepository.save(acao);
                return enviar(mensagem, "Remarcação confirmada! 💖\n\n" + nomeServico(atualizado) +
                        "\n" + atualizado.getDataHoraInicio().format(DATA_HORA) +
                        "\nProfissional: " + atualizado.getProfissional().getNome() +
                        "\n\nSua agenda foi atualizada com sucesso. ✨");
            }
        } catch (RuntimeException ex) {
            return enviar(mensagem, "Não consegui concluir essa solicitação porque o atendimento pode ter sido alterado ou o horário pode ter sido ocupado. 💬\n\nSe quiser, posso consultar novamente outros horários.");
        }
        return false;
    }

    private boolean prepararOpcoesRemarcacao(WhatsAppAgendamentoAcaoPendente acao, Agendamento agendamento,
                                              LocalDate data, WhatsAppMensagem mensagem) {
        if (data.isBefore(LocalDate.now())) {
            return enviar(mensagem, "A data informada já passou. Informe uma data futura, por exemplo: 30/09.");
        }
        var servico = agendamento.getServico();
        if (servico == null) {
            return enviar(mensagem, "Esse atendimento não possui um serviço definido para remarcação automática. Vou encaminhar você para nossa equipe. 💬");
        }
        var slots = disponibilidadeService.consultar(mensagem.getEmpresa().getId(), data, servico.getId(), null, 6);
        if (slots.isEmpty()) {
            acao.definirData(data);
            acao.aguardarNovaData();
            acaoRepository.save(acao);
            return enviar(mensagem, "Não encontrei horários livres para " + data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) +
                    ". 💖 Informe outra data e consultarei novamente.");
        }
        StringBuilder resposta = new StringBuilder("Encontrei estes horários para remarcar seu atendimento em " +
                data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) + ":\n\n");
        StringBuilder opcoes = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (i > 0) opcoes.append(';');
            opcoes.append(slot.hora()).append('|').append(slot.profissionalId()).append('|')
                    .append(slot.fim()).append('|').append(slot.profissional());
            resposta.append(i + 1).append(". ").append(slot.texto()).append("\n");
        }
        acao.definirData(data);
        acao.definirOpcoes(opcoes.toString());
        acaoRepository.save(acao);
        resposta.append("\nResponda com o número da opção ou com o horário, por exemplo: 14:00.");
        return enviar(mensagem, resposta.toString());
    }

    private String nomeServico(Agendamento agendamento) {
        return agendamento.getServico() != null ? agendamento.getServico().getNome() : agendamento.getTipo().getNome();
    }

    private boolean ehNao(String texto) {
        return texto.matches("^(nao|não|n|cancelar|desistir|nao quero|não quero)$");
    }

    private boolean processarReserva(WhatsAppReservaPendente reserva, WhatsAppMensagem mensagem) {
        String texto = normalizar(mensagem.getMensagem());
        try {
            if (contem(texto, "cancelar", "nao", "não", "desistir")) {
                reserva.cancelar();
                reservaRepository.save(reserva);
                return enviar(mensagem, "Tudo bem! 💖 Não vou reservar esse horário. Se precisar, é só me chamar novamente.");
            }
            if (reserva.getStatus() == WhatsAppReservaPendente.Status.AGUARDANDO_HORARIO) {
                String opcao = encontrarOpcao(reserva.getOpcoesJson(), texto);
                if (opcao == null) {
                    return enviar(mensagem, "Não consegui identificar o horário. Responda com o número da opção (1, 2, 3...) ou com o horário, por exemplo: 14:00.");
                }
                String[] p = opcao.split("\\|", -1);
                LocalDateTime inicio = LocalDateTime.of(reserva.getDataDesejada(), java.time.LocalTime.parse(p[0]));
                LocalDateTime fim = LocalDateTime.parse(p[2]);
                reserva.selecionar(Long.valueOf(p[1]), inicio, fim);
                reservaRepository.save(reserva);
                return enviar(mensagem, "Perfeito! 😊\n\nServiço: " + reserva.getServico().getNome() +
                        "\nData: " + inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                        "\nHorário: " + inicio.format(DateTimeFormatter.ofPattern("HH:mm")) +
                        "\nProfissional: " + p[3] +
                        "\n\nDeseja confirmar este agendamento? Responda SIM para confirmar ou NÃO para cancelar.");
            }
            if (reserva.getStatus() == WhatsAppReservaPendente.Status.AGUARDANDO_CONFIRMACAO) {
                if (!ehConfirmacao(texto)) {
                    return enviar(mensagem, "Para confirmar este horário, responda SIM. Se mudou de ideia, responda NÃO.");
                }
                Agendamento agendamento = agendaService.criarViaWhatsApp(mensagem.getEmpresa().getId(), reserva.getCliente().getId(),
                        reserva.getProfissionalId(), reserva.getServico().getId(), reserva.getInicioSelecionado(), reserva.getFimSelecionado());
                reserva.confirmar();
                reservaRepository.save(reserva);
                String profissional = agendamento.getProfissional().getNome();
                return enviar(mensagem, "Agendamento confirmado! 💖\n\n" + reserva.getServico().getNome() +
                        "\n" + agendamento.getDataHoraInicio().format(DATA_HORA) +
                        "\nProfissional: " + profissional +
                        "\n\nSeu horário já está registrado na agenda do Melvora. Até lá! ✨");
            }
        } catch (RuntimeException ex) {
            return enviar(mensagem, "Não consegui concluir a reserva porque o horário pode ter sido ocupado. 💬\n\nSe quiser, informe outra opção e consultarei novamente.");
        }
        return false;
    }

    private boolean enviar(WhatsAppMensagem mensagem, String texto) {
        try {
            whatsAppBusinessService.enviarMensagemAutomatica(mensagem.getEmpresa().getId(), mensagem.getCliente(), mensagem.getTelefone(), texto);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean ehConfirmacao(String texto) {
        return texto.matches("^(sim|s|confirmar|confirmo|pode|ok|okay|1|confirmado)$");
    }

    private String encontrarOpcao(String opcoes, String texto) {
        if (opcoes == null || opcoes.isBlank()) return null;
        String[] opcoesArray = opcoes.split(";");
        Matcher numero = Pattern.compile("^(?:opcao\\s*)?(\\d{1,2})$").matcher(texto);
        if (numero.matches()) {
            int idx = Integer.parseInt(numero.group(1));
            return idx >= 1 && idx <= opcoesArray.length ? opcoesArray[idx - 1] : null;
        }
        String hora = extrairHora(texto);
        if (hora != null) {
            for (String opcao : opcoesArray) if (opcao.startsWith(hora + "|")) return opcao;
        }
        return null;
    }

    private String extrairHora(String texto) {
        Matcher m = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b").matcher(texto);
        if (!m.find()) return null;
        int h = Integer.parseInt(m.group(1)); int min = Integer.parseInt(m.group(2));
        if (h > 23 || min > 59) return null;
        return String.format("%02d:%02d", h, min);
    }

    public WhatsAppIntencao classificar(String texto) {
        String t = normalizar(texto);
        if (t.isBlank()) return WhatsAppIntencao.DESCONHECIDA;
        if (contem(t, "atendente", "humano", "pessoa", "recepcao", "recepção", "falar com alguem", "falar com alguém")) return WhatsAppIntencao.HUMANO;
        if (contem(t, "cancelar", "cancela", "cancelamento")) return WhatsAppIntencao.CANCELAMENTO;
        if (contem(t, "remarcar", "remarcacao", "remarcação", "mudar meu horario", "mudar meu horário")) return WhatsAppIntencao.REMARCACAO;
        if (contem(t, "agendar", "agendamento", "marcar horario", "marcar horário", "quero horario", "quero horário") || (t.contains("quero") && (t.contains("amanha") || t.contains("hoje") || Pattern.compile("\\b\\d{1,2}/\\d{1,2}(?:/\\d{4})?\\b").matcher(t).find()))) return WhatsAppIntencao.AGENDAMENTO;
        if (contem(t, "preco", "preço", "quanto custa", "valor", "valores")) return WhatsAppIntencao.PRECO;
        if (contem(t, "servico", "serviços", "servicos", "fazem", "procedimento", "procedimentos")) return WhatsAppIntencao.SERVICOS;
        if (contem(t, "oi", "ola", "olá", "bom dia", "boa tarde", "boa noite", "tudo bem")) return WhatsAppIntencao.SAUDACAO;
        return WhatsAppIntencao.DESCONHECIDA;
    }

    private String gerarResposta(ConfiguracaoWhatsAppAutomacao cfg, WhatsAppMensagem mensagem, WhatsAppIntencao intencao) {
        String nome = mensagem.getCliente() != null ? primeiroNome(mensagem.getCliente().getNome()) : "Olá";
        return switch (intencao) {
            case SAUDACAO -> cfg.isResponderSaudacao() ? saudacao(nome) : null;
            case SERVICOS -> cfg.isResponderServicos() ? listarServicos(mensagem.getEmpresa().getId()) : null;
            case PRECO -> cfg.isResponderPreco() ? listarPrecos(mensagem.getEmpresa().getId()) : null;
            case AGENDAMENTO -> cfg.isResponderAgendamento() ? (temServicoEData(mensagem.getEmpresa().getId(), mensagem.getMensagem()) ? consultarDisponibilidade(mensagem) : respostaAgendamento(nome, mensagem.getCliente())) : null;
            case REMARCACAO -> cfg.isEncaminharHumano() ? "Claro! 💖 Vou localizar seu próximo atendimento e iniciar a remarcação com segurança." : null;
            case CANCELAMENTO -> cfg.isEncaminharHumano() ? "Entendi. Vou localizar seu próximo atendimento e iniciar o cancelamento com segurança." : null;
            case HUMANO -> cfg.isEncaminharHumano() ? "Claro! 💬 Sua conversa foi direcionada para atendimento humano. Nossa equipe continuará por aqui." : null;
            case DESCONHECIDA -> null;
        };
    }

    private String saudacao(String nome) {
        return "Olá, " + nome + "! 😊\n\n" +
                "Sou o atendimento automático do Melvora. Posso ajudar com:\n" +
                "• Serviços e valores\n" +
                "• Agendamento\n" +
                "• Remarcação ou cancelamento\n" +
                "• Atendimento humano\n\n" +
                "É só me dizer o que você precisa. 💖";
    }

    private String listarServicos(Long empresaId) {
        List<Servico> servicos = servicoRepository.findAtivosByEmpresaId(empresaId);
        if (servicos.isEmpty()) return "No momento não há serviços cadastrados para consulta automática. Vou encaminhar você para nossa equipe. 💬";
        StringBuilder sb = new StringBuilder("Estes são alguns dos nossos serviços disponíveis: 💇‍♀️\n\n");
        servicos.stream().limit(15).forEach(s -> sb.append("• ").append(s.getNome()).append(" — ").append(s.getCategoria()).append("\n"));
        sb.append("\nSe quiser, me diga qual serviço você deseja e posso orientar sobre o próximo passo.");
        return sb.toString();
    }

    private String listarPrecos(Long empresaId) {
        List<Servico> servicos = servicoRepository.findAtivosByEmpresaId(empresaId);
        if (servicos.isEmpty()) return "No momento não há serviços com preço cadastrado para consulta automática. Vou encaminhar você para nossa equipe. 💬";
        StringBuilder sb = new StringBuilder("Confira os valores cadastrados: 💰\n\n");
        servicos.stream().limit(15).forEach(s -> sb.append("• ").append(s.getNome()).append(" — R$ ").append(s.getPreco().setScale(2)).append("\n"));
        sb.append("\nOs valores exibidos são os cadastrados no Melvora e podem depender de avaliação. Para confirmar, nossa equipe pode ajudar.");
        return sb.toString();
    }

    private String respostaAgendamento(String nome, Cliente cliente) {
        if (cliente == null) return "Claro! 😊 Para agendar, primeiro precisamos identificar seu cadastro. Nossa equipe pode continuar o atendimento por aqui.";
        List<Agendamento> proximos = agendamentoRepository.listarProximosDaCliente(cliente.getEmpresa().getId(), cliente.getId(), LocalDateTime.now(), ATIVOS, PageRequest.of(0, 1));
        if (!proximos.isEmpty()) {
            Agendamento a = proximos.get(0);
            String servico = a.getServico() != null ? a.getServico().getNome() : a.getTipo().getNome();
            return "Claro, " + nome + "! 😊\n\nVocê já possui um próximo atendimento em " + DATA_HORA.format(a.getDataHoraInicio()) + ".\nServiço: " + servico + ".\n\nSe deseja um novo horário, escreva, por exemplo: \"quero Mega Hair amanhã\". Vou consultar horários livres. A confirmação final continua sendo feita pelo sistema/equipe.";
        }
        String texto = cliente.getNome() == null ? "" : "";
        return "Claro, " + nome + "! 😊\n\nPara consultar horários reais, informe o serviço e a data. Exemplo: \"quero Mega Hair amanhã\" ou \"quero manutenção 28/09\".\n\nNesta etapa eu apenas consulto a agenda; nenhum horário é reservado automaticamente.";
    }

    private String consultarDisponibilidade(WhatsAppMensagem mensagem) {
        String texto = normalizar(mensagem.getMensagem());
        List<Servico> servicos = servicoRepository.findAtivosByEmpresaId(mensagem.getEmpresa().getId());
        Servico escolhido = servicos.stream().filter(s -> texto.contains(normalizar(s.getNome()))).findFirst().orElse(null);
        LocalDate data = extrairData(texto);
        if (escolhido == null || data == null) return "Para consultar horários disponíveis, informe o serviço e a data. Exemplo: \"quero Mega Hair amanhã\" ou \"quero manutenção 28/09\".";
        try {
            var slots = disponibilidadeService.consultar(mensagem.getEmpresa().getId(), data, escolhido.getId(), null, 6);
            if (slots.isEmpty()) return "Não encontrei horários livres para " + escolhido.getNome() + " em " + data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) + ". Se quiser, informe outra data e consultarei novamente. 💖";
            StringBuilder sb = new StringBuilder("Encontrei estes horários para " + escolhido.getNome() + " em " + data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) + ":\n\n");
            StringBuilder opcoes = new StringBuilder();
            for (int i = 0; i < slots.size(); i++) {
                var s = slots.get(i);
                if (i > 0) opcoes.append(';');
                opcoes.append(s.hora()).append('|').append(s.profissionalId()).append('|')
                        .append(s.fim().toString()).append('|').append(s.profissional());
                sb.append(i + 1).append(". ").append(s.texto()).append("\n");
            }
            try {
                if (mensagem.getCliente() != null) {
                    reservaRepository.findAtiva(mensagem.getEmpresa().getId(), mensagem.getTelefone(),
                            List.of(WhatsAppReservaPendente.Status.AGUARDANDO_HORARIO, WhatsAppReservaPendente.Status.AGUARDANDO_CONFIRMACAO), LocalDateTime.now())
                            .ifPresent(r -> { r.cancelar(); reservaRepository.save(r); });
                    reservaRepository.save(new WhatsAppReservaPendente(mensagem.getEmpresa(), mensagem.getCliente(), escolhido, mensagem.getTelefone(),
                            data, opcoes.toString(), LocalDateTime.now().plusMinutes(15)));
                }
            } catch (RuntimeException ignored) { }
            sb.append("\nResponda com o número da opção ou com o horário, por exemplo: 14:00.\n\nO horário ficará reservado para confirmação por 15 minutos.");
            return sb.toString();
        } catch (RuntimeException ex) {
            return "Ainda não consigo consultar horários para essa data. Nossa equipe pode continuar o atendimento por aqui. 💬";
        }
    }

    private LocalDate extrairData(String texto) {
        LocalDate hoje = LocalDate.now();
        if (texto.contains("amanha")) return hoje.plusDays(1);
        if (texto.contains("hoje")) return hoje;
        Matcher m = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}))?\\b").matcher(texto);
        if (!m.find()) return null;
        try { int d=Integer.parseInt(m.group(1)), mes=Integer.parseInt(m.group(2)); int ano=m.group(3)==null?hoje.getYear():Integer.parseInt(m.group(3)); return LocalDate.of(ano,mes,d); }
        catch (DateTimeParseException | NumberFormatException ex) { return null; }
    }

    private boolean temServicoEData(Long empresaId, String texto) {
        if (texto == null) return false;
        String t = normalizar(texto);
        boolean data = t.contains("amanha") || t.contains("hoje") || Pattern.compile("\\b\\d{1,2}/\\d{1,2}(?:/\\d{4})?\\b").matcher(t).find();
        if (!data) return false;
        return servicoRepository.findAtivosByEmpresaId(empresaId).stream().anyMatch(s -> t.contains(normalizar(s.getNome())));
    }

    private String primeiroNome(String nome) {
        if (nome == null || nome.isBlank()) return "";
        return nome.trim().split("\\s+")[0];
    }

    private boolean contem(String texto, String... termos) {
        for (String termo : termos) if (texto.contains(normalizar(termo))) return true;
        return false;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT).trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
