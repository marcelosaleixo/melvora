package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.entity.Agendamento;
import com.marceloaleixo.melvora.entity.Atendimento;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.repository.AtendimentoRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoService {
    private final AtendimentoRepository repository;
    private final ModuloAcessoService moduloAcessoService;
    private final FinanceiroService financeiroService;

    public AtendimentoService(AtendimentoRepository repository, ModuloAcessoService moduloAcessoService, FinanceiroService financeiroService) {
        this.repository = repository;
        this.moduloAcessoService = moduloAcessoService;
        this.financeiroService = financeiroService;
    }

    @Transactional(readOnly = true)
    public Page<Atendimento> listar(Pageable pageable) {
        moduloAcessoService.exigir(ModuloSistema.HISTORICO);
        return repository.findByEmpresaId(TenantContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public Atendimento buscar(Long id) {
        moduloAcessoService.exigir(ModuloSistema.HISTORICO);
        return repository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Atendimento não encontrado."));
    }

    /** Registra a conclusão uma única vez. É chamado pela Agenda no fechamento do atendimento. */
    @Transactional
    public void registrarSeNecessario(Agendamento agendamento) {
        if (!moduloAcessoService.possui(ModuloSistema.HISTORICO)) return;
        if (agendamento == null || agendamento.getId() == null) return;
        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO) {
            throw new RegraNegocioException("Somente atendimentos concluídos podem entrar no histórico.");
        }
        Long empresaId = TenantContext.getRequired();
        if (agendamento.getEmpresa() == null || !empresaId.equals(agendamento.getEmpresa().getId())) {
            throw new RegraNegocioException("O agendamento não pertence à empresa atual.");
        }
        if (repository.existsByAgendamentoIdAndEmpresaId(agendamento.getId(), empresaId)) return;
        BigDecimal valor = agendamento.getServico() == null ? BigDecimal.ZERO : agendamento.getServico().getPreco();
        Atendimento atendimento = repository.save(new Atendimento(agendamento.getEmpresa(), agendamento, valor));
        financeiroService.registrarReceitaAtendimento(atendimento);
    }
}
