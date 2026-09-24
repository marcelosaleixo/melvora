package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.ClienteCrm360Data;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.repository.ClienteCrmRepository;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.repository.AgendamentoRepository;
import com.marceloaleixo.melvora.repository.PreferenciaComunicacaoContatoRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteCrm360Service {
    private final ClienteRepository clienteRepository;
    private final ClienteCrmRepository crmRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PreferenciaComunicacaoContatoRepository preferenciaRepository;
    private final ModuloAcessoService moduloAcessoService;

    public ClienteCrm360Service(ClienteRepository clienteRepository,
                                 ClienteCrmRepository crmRepository,
                                 AgendamentoRepository agendamentoRepository,
                                 PreferenciaComunicacaoContatoRepository preferenciaRepository,
                                 ModuloAcessoService moduloAcessoService) {
        this.clienteRepository = clienteRepository;
        this.crmRepository = crmRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional(readOnly = true)
    public ClienteCrm360Data gerar(Long clienteId) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        Long empresaId = TenantContext.getRequired();
        clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrada."));

        LocalDateTime agora = LocalDateTime.now();
        List<StatusAgendamento> ocupamAgenda = List.of(
                StatusAgendamento.AGENDADO,
                StatusAgendamento.CONFIRMADO,
                StatusAgendamento.EM_ATENDIMENTO);

        var proximos = moduloAcessoService.possui(empresaId, ModuloSistema.AGENDA)
                ? agendamentoRepository.listarProximosDaCliente(empresaId, clienteId, agora, ocupamAgenda, PageRequest.of(0, 1))
                : List.<com.marceloaleixo.melvora.entity.Agendamento>of();
        var agendamentosRecentes = moduloAcessoService.possui(empresaId, ModuloSistema.AGENDA)
                ? agendamentoRepository.listarRecentesDaCliente(empresaId, clienteId, PageRequest.of(0, 6))
                : List.<com.marceloaleixo.melvora.entity.Agendamento>of();

        var proximo = proximos.isEmpty() ? null : proximos.get(0);
        var ultimoAgendamento = agendamentosRecentes.stream()
                .filter(a -> !a.getDataHoraInicio().isAfter(agora))
                .findFirst().orElse(null);

        var atendimentos = crmRepository.findByEmpresaIdAndClienteIdOrderByDataHoraInicioDesc(
                empresaId, clienteId, PageRequest.of(0, 8));

        boolean agenda = moduloAcessoService.possui(empresaId, ModuloSistema.AGENDA);
        long totalAtendimentos = agenda ? agendamentoRepository.countByEmpresaIdAndClienteId(empresaId, clienteId) : 0;
        long concluidos = agenda ? agendamentoRepository.countByEmpresaIdAndClienteIdAndStatus(empresaId, clienteId, StatusAgendamento.CONCLUIDO) : 0;
        long cancelados = agenda ? agendamentoRepository.countByEmpresaIdAndClienteIdAndStatus(empresaId, clienteId, StatusAgendamento.CANCELADO) : 0;

        boolean financeiro = moduloAcessoService.possui(empresaId, ModuloSistema.FINANCEIRO);
        var totalPago = financeiro ? crmRepository.totalReceitasPagas(empresaId, clienteId) : null;
        long qtdPago = financeiro ? crmRepository.quantidadeReceitasPagas(empresaId, clienteId) : 0;

        boolean comunicacao = moduloAcessoService.possui(empresaId, ModuloSistema.COMUNICACAO);
        Double mediaAvaliacao = comunicacao ? crmRepository.mediaAvaliacao(empresaId, clienteId) : null;
        long qtdAvaliacoes = comunicacao ? crmRepository.quantidadeAvaliacoes(empresaId, clienteId) : 0;
        long naoLidas = comunicacao ? crmRepository.mensagensNaoLidas(empresaId, clienteId, com.marceloaleixo.melvora.entity.WhatsAppMensagem.Direcao.ENTRADA) : 0;
        var ultimaMensagem = comunicacao ? crmRepository.ultimaMensagem(empresaId, clienteId) : null;
        var ultimaMensagemTexto = comunicacao ? crmRepository.ultimaMensagemTexto(empresaId, clienteId) : null;

        boolean optIn = false;
        boolean optOut = false;
        if (comunicacao) {
            var pref = preferenciaRepository.findByEmpresaIdAndClienteId(empresaId, clienteId).orElse(null);
            if (pref != null) {
                optIn = pref.isRetencaoOptIn();
                optOut = pref.getRetencaoOptOutAt() != null;
            }
        }

        return new ClienteCrm360Data(
                proximo,
                ultimoAgendamento,
                totalAtendimentos,
                concluidos,
                cancelados,
                List.copyOf(atendimentos.getContent()),
                totalPago,
                qtdPago,
                mediaAvaliacao,
                qtdAvaliacoes,
                naoLidas,
                ultimaMensagem,
                ultimaMensagemTexto,
                optIn,
                optOut);
    }
}
