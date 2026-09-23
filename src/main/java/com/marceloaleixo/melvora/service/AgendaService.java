package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.AgendaRequests;
import com.marceloaleixo.melvora.entity.Agendamento;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.Role;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.repository.AgendamentoRepository;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.repository.UsuarioRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendaService {
    private static final List<StatusAgendamento> STATUS_QUE_OCUPAM_HORARIO = List.of(
            StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO, StatusAgendamento.EM_ATENDIMENTO);

    private final AgendamentoRepository repository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ModuloAcessoService moduloAcessoService;
    private final ServicoService servicoService;
    private final AtendimentoService atendimentoService;

    public AgendaService(AgendamentoRepository repository, ClienteRepository clienteRepository,
                         UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository,
                         ModuloAcessoService moduloAcessoService, ServicoService servicoService, AtendimentoService atendimentoService) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.moduloAcessoService = moduloAcessoService;
        this.servicoService = servicoService;
        this.atendimentoService = atendimentoService;
    }

    @Transactional(readOnly = true)
    public List<Agendamento> listarPeriodo(LocalDate inicio, LocalDate fim) {
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new RegraNegocioException("Período da agenda inválido.");
        }
        return repository.listarNoPeriodo(TenantContext.getRequired(), inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());
    }

    @Transactional
    public Agendamento criar(AgendaRequests.CriarAgendamentoRequest request) {
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        if (request == null) throw new RegraNegocioException("Os dados do agendamento são obrigatórios.");
        validarHorario(request.dataHoraInicio(), request.dataHoraFim());

        Long empresaId = TenantContext.getRequired();
        var empresa = empresaRepository.findById(empresaId)
                .filter(e -> e.isAtiva())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada ou inativa."));
        var cliente = clienteRepository.findByIdAndEmpresaId(request.clienteId(), empresaId)
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado ou inativo."));
        var profissional = usuarioRepository.findByIdAndEmpresaId(request.profissionalId(), empresaId)
                .filter(u -> u.isAtivo() && (u.getRole() == Role.ADMIN || u.getRole() == Role.PROFISSIONAL))
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado ou inativo."));

        var servico = resolverServico(request.servicoId(), profissional.getId());
        LocalDateTime inicio = request.dataHoraInicio();
        LocalDateTime fim = calcularFim(inicio, servico, request.dataHoraFim());
        validarHorario(inicio, fim);

        if (repository.existeConflitoProfissional(empresaId, profissional.getId(), inicio, fim, STATUS_QUE_OCUPAM_HORARIO)) {
            throw new RegraNegocioException("O profissional já possui um atendimento nesse horário.");
        }
        if (repository.existeConflitoCliente(empresaId, cliente.getId(), inicio, fim, STATUS_QUE_OCUPAM_HORARIO)) {
            throw new RegraNegocioException("A cliente já possui um atendimento nesse horário.");
        }

        try {
            Agendamento agendamento = new Agendamento(empresa, cliente, profissional, request.tipo(),
                    inicio, fim, normalizar(request.observacoes()));
            agendamento.definirServico(servico);
            return repository.saveAndFlush(agendamento);
        } catch (DataIntegrityViolationException ex) {
            throw new RegraNegocioException("O horário foi ocupado por outro atendimento. Atualize a agenda e tente novamente.");
        }
    }

    @Transactional
    public Agendamento reagendar(Long id, AgendaRequests.EditarAgendamentoRequest request) {
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        if (request == null) throw new RegraNegocioException("Os dados do agendamento são obrigatórios.");

        Long empresaId = TenantContext.getRequired();
        Agendamento agendamento = repository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO
                && agendamento.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw new RegraNegocioException("Somente agendamentos aguardando atendimento podem ser alterados.");
        }

        var servico = resolverServico(request.servicoId(), agendamento.getProfissional().getId());
        LocalDateTime inicio = request.dataHoraInicio();
        LocalDateTime fim = calcularFim(inicio, servico, request.dataHoraFim());
        validarHorario(inicio, fim);

        if (repository.existeConflitoProfissionalExcluindo(empresaId, agendamento.getProfissional().getId(),
                inicio, fim, STATUS_QUE_OCUPAM_HORARIO, id)) {
            throw new RegraNegocioException("O profissional já possui outro atendimento nesse horário.");
        }
        if (repository.existeConflitoClienteExcluindo(empresaId, agendamento.getCliente().getId(),
                inicio, fim, STATUS_QUE_OCUPAM_HORARIO, id)) {
            throw new RegraNegocioException("A cliente já possui outro atendimento nesse horário.");
        }

        agendamento.reagendar(request.tipo(), servico, inicio, fim, normalizar(request.observacoes()));
        return agendamento;
    }

    @Transactional
    public Agendamento alterarStatus(Long id, StatusAgendamento novoStatus) {
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        if (novoStatus == null) throw new RegraNegocioException("Status inválido.");
        Agendamento agendamento = repository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
        validarTransicao(agendamento.getStatus(), novoStatus);
        agendamento.alterarStatus(novoStatus);
        if (novoStatus == StatusAgendamento.CONCLUIDO) {
            atendimentoService.registrarSeNecessario(agendamento);
        }
        return agendamento;
    }

    @Transactional(readOnly = true)
    public Agendamento buscar(Long id) {
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        return repository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
    }


    private com.marceloaleixo.melvora.entity.Servico resolverServico(Long servicoId, Long profissionalId) {
        if (servicoId == null) return null;
        return servicoService.buscarParaAgendamento(servicoId, profissionalId);
    }

    private LocalDateTime calcularFim(LocalDateTime inicio, com.marceloaleixo.melvora.entity.Servico servico, LocalDateTime fimInformado) {
        if (servico == null) return fimInformado;
        return inicio.plusMinutes(servico.getDuracaoMinutos());
    }

    private void validarHorario(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new RegraNegocioException("O horário final deve ser posterior ao horário inicial.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("Não é possível criar um agendamento no passado.");
        }
        long minutos = Duration.between(inicio, fim).toMinutes();
        if (minutos < 15) throw new RegraNegocioException("O atendimento deve ter pelo menos 15 minutos.");
        if (minutos > 12 * 60) throw new RegraNegocioException("O atendimento não pode ultrapassar 12 horas.");
    }

    private void validarTransicao(StatusAgendamento atual, StatusAgendamento novo) {
        if (atual == StatusAgendamento.CANCELADO || atual == StatusAgendamento.FALTOU || atual == StatusAgendamento.CONCLUIDO) {
            throw new RegraNegocioException("Um agendamento encerrado não pode ter o status alterado.");
        }
        if (novo == StatusAgendamento.AGENDADO && atual != StatusAgendamento.AGENDADO) {
            throw new RegraNegocioException("Não é permitido retornar um atendimento para Agendado.");
        }
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
