package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.MegaHairRequests;
import com.marceloaleixo.melvora.entity.AplicacaoLote;
import com.marceloaleixo.melvora.entity.AplicacaoMegaHair;
import com.marceloaleixo.melvora.entity.ManutencaoMegaHair;
import com.marceloaleixo.melvora.entity.enums.Role;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.entity.enums.TipoAgendamento;
import com.marceloaleixo.melvora.entity.Agendamento;
import com.marceloaleixo.melvora.repository.AgendamentoRepository;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.repository.AplicacaoMegaHairRepository;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.repository.ManutencaoMegaHairRepository;
import com.marceloaleixo.melvora.repository.UsuarioRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MegaHairService {

    private final AplicacaoMegaHairRepository aplicacaoRepository;
    private final ManutencaoMegaHairRepository manutencaoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstoqueService estoqueService;
    private final ModuloAcessoService moduloAcessoService;
    private final AgendamentoRepository agendamentoRepository;

    public MegaHairService(
            AplicacaoMegaHairRepository aplicacaoRepository,
            ManutencaoMegaHairRepository manutencaoRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            EstoqueService estoqueService,
            ModuloAcessoService moduloAcessoService,
            AgendamentoRepository agendamentoRepository) {
        this.aplicacaoRepository = aplicacaoRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.estoqueService = estoqueService;
        this.moduloAcessoService = moduloAcessoService;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional
    public AplicacaoMegaHair aplicar(
            MegaHairRequests.AplicarRequest request) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.MEGA_HAIR);

        if (request == null) {
            throw new RegraNegocioException(
                "Os dados da aplicação são obrigatórios."
            );
        }

        long empresaId = TenantContext.getRequired();

        var cliente = clienteRepository
            .findByIdAndEmpresaId(
                request.clienteId(),
                empresaId
            )
            .filter(c -> c.isAtivo())
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Cliente não encontrado ou inativo."
                )
            );

        var profissional = usuarioRepository
            .findByIdAndEmpresaId(
                request.profissionalId(),
                empresaId
            )
            .filter(u ->
                u.isAtivo()
                && (u.getRole() == Role.PROFISSIONAL
                    || u.getRole() == Role.ADMIN)
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Profissional não encontrado ou sem permissão."
                )
            );

        Agendamento agendamento = validarAgendamentoParaAplicacao(request.agendamentoId(), cliente.getId(), profissional.getId(), request.dataAplicacao(), empresaId);

        if (request.dataAplicacao().isAfter(LocalDate.now())) {
            throw new RegraNegocioException(
                "A data da aplicação não pode estar no futuro."
            );
        }

        if (request.lotes() == null || request.lotes().isEmpty()) {
            throw new RegraNegocioException(
                "Informe pelo menos um lote para registrar a aplicação."
            );
        }

        if (request.lotes().size() > 20) {
            throw new RegraNegocioException(
                "Uma aplicação pode utilizar no máximo 20 lotes."
            );
        }

        Set<Long> lotesInformados = new HashSet<>();

        for (var item : request.lotes()) {
            if (item == null || item.loteId() == null || item.loteId() <= 0) {
                throw new RegraNegocioException("Lote inválido informado na aplicação.");
            }
            if (item.quantidade() == null || item.quantidade() <= 0) {
                throw new RegraNegocioException("A quantidade de cada lote deve ser maior que zero.");
            }
            if (!lotesInformados.add(item.loteId())) {
                throw new RegraNegocioException(
                    "O mesmo lote não pode ser informado mais de uma vez na aplicação."
                );
            }
        }

        AplicacaoMegaHair aplicacao =
            new AplicacaoMegaHair(
                cliente.getEmpresa(),
                cliente,
                profissional,
                request.dataAplicacao(),
                normalizar(request.observacoes())
            );
        if (agendamento != null) {
            aplicacao.vincularAgendamento(agendamento);
        }

        aplicacaoRepository.save(aplicacao);

        for (var item : request.lotes()) {

            var lote = estoqueService.consumirParaAplicacao(
                item.loteId(),
                item.quantidade()
            );

            if (!lote.getEmpresa().getId().equals(empresaId)) {
                throw new ResourceNotFoundException(
                    "Lote não pertence à empresa atual."
                );
            }

            if (lote.getProduto() == null || !lote.getProduto().isAtivo()) {
                throw new RegraNegocioException(
                    "O produto vinculado ao lote está inativo e não pode ser aplicado."
                );
            }

            var itemAplicacao =
                new AplicacaoLote(
                    lote,
                    item.quantidade()
                );

            aplicacao.adicionarLote(itemAplicacao);

            estoqueService.registrarSaida(
                lote,
                aplicacao,
                item.quantidade()
            );
        }

        return aplicacaoRepository.save(aplicacao);
    }

    @Transactional
    public ManutencaoMegaHair registrarManutencao(
            Long aplicacaoId,
            Long profissionalId,
            MegaHairRequests.ManutencaoRequest request) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.MEGA_HAIR);

        long empresaId = TenantContext.getRequired();

        var aplicacao = aplicacaoRepository
            .findByIdAndEmpresaId(
                aplicacaoId,
                empresaId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Aplicação não encontrada."
                )
            );

        if (request.dataManutencao()
                .isBefore(aplicacao.getDataAplicacao())) {
            throw new RegraNegocioException(
                "A manutenção não pode ser anterior à aplicação."
            );
        }

        var profissional = usuarioRepository
            .findByIdAndEmpresaId(
                profissionalId,
                empresaId
            )
            .filter(u ->
                u.isAtivo()
                && (u.getRole() == Role.PROFISSIONAL
                    || u.getRole() == Role.ADMIN)
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Profissional não encontrado ou sem permissão."
                )
            );

        Agendamento agendamento = validarAgendamentoParaManutencao(request.agendamentoId(), aplicacao.getCliente().getId(), profissional.getId(), request.dataManutencao(), empresaId);
        var manutencao = new ManutencaoMegaHair(
            aplicacao.getEmpresa(),
            aplicacao.getCliente(),
            aplicacao,
            profissional,
            request.dataManutencao(),
            request.tipo(),
            normalizar(request.observacoes())
        );
        if (agendamento != null) manutencao.vincularAgendamento(agendamento);
        return manutencaoRepository.save(manutencao);
    }

    private Agendamento validarAgendamentoParaAplicacao(Long agendamentoId, Long clienteId, Long profissionalId, LocalDate data, long empresaId) {
        if (agendamentoId == null) return null;
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.AGENDA);
        var agendamento = agendamentoRepository.findByIdAndEmpresaId(agendamentoId, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO) throw new RegraNegocioException("O atendimento precisa estar concluído antes de gerar a aplicação.");
        if (agendamento.getTipo() != TipoAgendamento.APLICACAO_MEGA_HAIR) throw new RegraNegocioException("O agendamento selecionado não é de aplicação de Mega Hair.");
        if (!agendamento.getCliente().getId().equals(clienteId) || !agendamento.getProfissional().getId().equals(profissionalId)) throw new RegraNegocioException("Cliente e profissional devem ser os mesmos do agendamento.");
        if (!agendamento.getDataHoraInicio().toLocalDate().equals(data)) throw new RegraNegocioException("A data da aplicação deve ser a mesma data do agendamento.");
        if (aplicacaoRepository.existsByAgendamentoIdAndEmpresaId(agendamentoId, empresaId)) throw new RegraNegocioException("Este agendamento já possui uma aplicação registrada.");
        return agendamento;
    }

    private Agendamento validarAgendamentoParaManutencao(Long agendamentoId, Long clienteId, Long profissionalId, LocalDate data, long empresaId) {
        if (agendamentoId == null) return null;
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.AGENDA);
        var agendamento = agendamentoRepository.findByIdAndEmpresaId(agendamentoId, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO) throw new RegraNegocioException("O atendimento precisa estar concluído antes de gerar a manutenção.");
        if (agendamento.getTipo() != TipoAgendamento.MANUTENCAO_MEGA_HAIR) throw new RegraNegocioException("O agendamento selecionado não é de manutenção de Mega Hair.");
        if (!agendamento.getCliente().getId().equals(clienteId) || !agendamento.getProfissional().getId().equals(profissionalId)) throw new RegraNegocioException("Cliente e profissional devem ser os mesmos do agendamento.");
        if (!agendamento.getDataHoraInicio().toLocalDate().equals(data)) throw new RegraNegocioException("A data da manutenção deve ser a mesma data do agendamento.");
        if (manutencaoRepository.existsByAgendamentoIdAndEmpresaId(agendamentoId, empresaId)) throw new RegraNegocioException("Este agendamento já possui uma manutenção registrada.");
        return agendamento;
    }

    @Transactional(readOnly = true)
    public Long ultimaAplicacaoIdDaCliente(Long clienteId) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.MEGA_HAIR);
        long empresaId = TenantContext.getRequired();
        clienteRepository.findByIdAndEmpresaId(clienteId, empresaId).orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));
        return aplicacaoRepository.findTopByEmpresaIdAndClienteIdOrderByDataAplicacaoDesc(empresaId, clienteId)
            .map(AplicacaoMegaHair::getId)
            .orElseThrow(() -> new RegraNegocioException("A cliente ainda não possui uma aplicação de Mega Hair."));
    }

    @Transactional(readOnly = true)
    public MegaHairRequests.HistoricoResponse historico(
            Long clienteId) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.HISTORICO);

        long empresaId = TenantContext.getRequired();

        clienteRepository
            .findByIdAndEmpresaId(clienteId, empresaId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Cliente não encontrado."
                )
            );

        var aplicacoes = aplicacaoRepository
            .findByEmpresaIdAndClienteIdOrderByDataAplicacaoDesc(
                empresaId,
                clienteId
            )
            .stream()
            .map(MegaHairRequests.AplicacaoResponse::from)
            .toList();

        var manutencoes = manutencaoRepository
            .findByEmpresaIdAndClienteIdOrderByDataManutencaoDesc(
                empresaId,
                clienteId
            )
            .stream()
            .map(MegaHairRequests.ManutencaoResponse::from)
            .toList();

        var timeline = new ArrayList<MegaHairRequests.TimelineItem>();

        aplicacoes.forEach(a -> timeline.add(new MegaHairRequests.TimelineItem(
            a.dataAplicacao(),
            "APLICACAO",
            "Aplicação de Mega Hair",
            resumoLotes(a.lotes()),
            a.profissional(),
            a.id()
        )));

        manutencoes.forEach(m -> timeline.add(new MegaHairRequests.TimelineItem(
            m.dataManutencao(),
            m.tipo(),
            tituloManutencao(m.tipo()),
            m.observacoes(),
            m.profissional(),
            m.id()
        )));

        timeline.sort(Comparator.comparing(MegaHairRequests.TimelineItem::data).reversed());

        var ultimaAplicacao = aplicacoes.stream()
            .map(MegaHairRequests.AplicacaoResponse::dataAplicacao)
            .max(LocalDate::compareTo)
            .orElse(null);

        var ultimaManutencao = manutencoes.stream()
            .map(MegaHairRequests.ManutencaoResponse::dataManutencao)
            .max(LocalDate::compareTo)
            .orElse(null);

        String statusAtual = calcularStatus(aplicacoes, manutencoes);
        String statusDescricao = switch (statusAtual) {
            case "REMOVIDO" -> "A última movimentação registrada foi uma remoção.";
            case "ATIVO" -> "Existe uma aplicação vigente no histórico, sem remoção posterior.";
            default -> "Ainda não existe uma aplicação registrada.";
        };

        var resumo = new MegaHairRequests.ResumoHistorico(
            statusAtual,
            statusDescricao,
            ultimaAplicacao,
            ultimaManutencao,
            aplicacoes.size(),
            manutencoes.size()
        );

        return new MegaHairRequests.HistoricoResponse(
            aplicacoes,
            manutencoes,
            List.copyOf(timeline),
            resumo
        );
    }

    private String resumoLotes(List<MegaHairRequests.ItemResponse> lotes) {
        if (lotes == null || lotes.isEmpty()) {
            return "Nenhum lote informado.";
        }
        int quantidade = lotes.stream().mapToInt(MegaHairRequests.ItemResponse::quantidade).sum();
        return quantidade + " unidade(s) em " + lotes.size() + " lote(s).";
    }

    private String tituloManutencao(String tipo) {
        return switch (tipo) {
            case "REAPLICACAO" -> "Reaplicação de Mega Hair";
            case "REMOCAO" -> "Remoção de Mega Hair";
            case "MANUTENCAO" -> "Manutenção de Mega Hair";
            default -> "Evento de Mega Hair";
        };
    }

    private String calcularStatus(
            List<MegaHairRequests.AplicacaoResponse> aplicacoes,
            List<MegaHairRequests.ManutencaoResponse> manutencoes) {
        var ultimoEvento = new ArrayList<MegaHairRequests.TimelineItem>();

        aplicacoes.forEach(a -> ultimoEvento.add(new MegaHairRequests.TimelineItem(
            a.dataAplicacao(), "APLICACAO", "Aplicação", null, a.profissional(), a.id())));
        manutencoes.forEach(m -> ultimoEvento.add(new MegaHairRequests.TimelineItem(
            m.dataManutencao(), m.tipo(), m.tipo(), null, m.profissional(), m.id())));

        if (ultimoEvento.isEmpty()) return "SEM_APLICACAO";

        return ultimoEvento.stream()
            .max(Comparator.comparing(MegaHairRequests.TimelineItem::data))
            .map(MegaHairRequests.TimelineItem::tipo)
            .filter("REMOCAO"::equals)
            .map(v -> "REMOVIDO")
            .orElse("ATIVO");
    }

    private String normalizar(String value) {
        return value == null ? null : value.trim();
    }
}
