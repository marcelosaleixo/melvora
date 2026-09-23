package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.RelatorioData;
import com.marceloaleixo.melvora.entity.enums.*;
import com.marceloaleixo.melvora.repository.AtendimentoRepository;
import com.marceloaleixo.melvora.repository.ComissaoRepository;
import com.marceloaleixo.melvora.repository.LancamentoFinanceiroRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatorioService {
    private final LancamentoFinanceiroRepository financeiro;
    private final ComissaoRepository comissoes;
    private final AtendimentoRepository atendimentos;
    private final ModuloAcessoService modulos;

    public RelatorioService(LancamentoFinanceiroRepository financeiro, ComissaoRepository comissoes, AtendimentoRepository atendimentos, ModuloAcessoService modulos) {
        this.financeiro=financeiro; this.comissoes=comissoes; this.atendimentos=atendimentos; this.modulos=modulos;
    }

    @Transactional(readOnly=true)
    public RelatorioData gerar(LocalDate inicio, LocalDate fim) {
        modulos.exigir(ModuloSistema.RELATORIOS);
        if (inicio == null || fim == null || fim.isBefore(inicio)) throw new IllegalArgumentException("Período inválido.");
        if (ChronoUnit.DAYS.between(inicio, fim) > 366) throw new IllegalArgumentException("O período máximo para análise é de 366 dias.");
        Long empresa=TenantContext.getRequired();
        LocalDateTime ini=inicio.atStartOfDay(); LocalDateTime fimExclusivo=fim.plusDays(1).atStartOfDay();
        BigDecimal receitas=financeiro.total(empresa,TipoLancamentoFinanceiro.RECEITA,StatusLancamentoFinanceiro.PAGO,inicio,fim);
        BigDecimal despesas=financeiro.total(empresa,TipoLancamentoFinanceiro.DESPESA,StatusLancamentoFinanceiro.PAGO,inicio,fim);
        BigDecimal cp=comissoes.total(empresa,StatusComissao.PAGO,inicio,fim);
        BigDecimal cpend=comissoes.total(empresa,StatusComissao.PENDENTE,inicio,fim);
        return new RelatorioData(inicio,fim,receitas,despesas,cp,cpend,receitas.subtract(despesas).subtract(cp),
            atendimentos.countPeriodo(empresa,ini,fimExclusivo), atendimentos.countClientesPeriodo(empresa,ini,fimExclusivo),
            atendimentos.servicosMaisRealizados(empresa,ini,fimExclusivo,PageRequest.of(0,8)),
            atendimentos.profissionaisPorFaturamento(empresa,ini,fimExclusivo,PageRequest.of(0,8)));
    }
}
