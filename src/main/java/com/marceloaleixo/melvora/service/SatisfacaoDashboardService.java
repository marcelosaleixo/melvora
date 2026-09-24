package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.SatisfacaoDashboardData;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.repository.AvaliacaoAtendimentoRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SatisfacaoDashboardService {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM/yy", PT_BR);

    private final AvaliacaoAtendimentoRepository repository;
    private final ModuloAcessoService moduloAcessoService;

    public SatisfacaoDashboardService(AvaliacaoAtendimentoRepository repository,
                                      ModuloAcessoService moduloAcessoService) {
        this.repository = repository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional(readOnly = true)
    public SatisfacaoDashboardData gerar(LocalDate inicio, LocalDate fim) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Período inválido.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > 366) {
            throw new IllegalArgumentException("O período máximo para análise é de 366 dias.");
        }

        Long empresaId = TenantContext.getRequired();
        LocalDateTime inicioDt = inicio.atStartOfDay();
        LocalDateTime fimExclusivo = fim.plusDays(1).atStartOfDay();

        long respondidas = repository.countRespondidasPeriodo(empresaId, inicioDt, fimExclusivo);
        long pendentes = repository.countPendentesPeriodo(empresaId, inicioDt, fimExclusivo);
        long cincoEstrelas = repository.countCincoEstrelasPeriodo(empresaId, inicioDt, fimExclusivo);
        double media = arredondar(repository.mediaNotaPeriodo(empresaId, inicioDt, fimExclusivo));
        double percentualCinco = respondidas == 0 ? 0 : arredondar((cincoEstrelas * 100.0) / respondidas);

        List<SatisfacaoDashboardData.Distribuicao> distribuicao = montarDistribuicao(
                repository.distribuicaoNotas(empresaId, inicioDt, fimExclusivo), respondidas);
        List<SatisfacaoDashboardData.Ranking> profissionais = montarRanking(
                repository.satisfacaoPorProfissional(empresaId, inicioDt, fimExclusivo, PageRequest.of(0, 8)));
        List<SatisfacaoDashboardData.Ranking> servicos = montarRanking(
                repository.satisfacaoPorServico(empresaId, inicioDt, fimExclusivo, PageRequest.of(0, 8)));
        List<SatisfacaoDashboardData.Tendencia> tendencia = montarTendencia(
                repository.tendenciaMensal(empresaId, inicioDt, fimExclusivo), inicio, fim);

        return new SatisfacaoDashboardData(inicio, fim, media, respondidas, pendentes,
                percentualCinco, distribuicao, profissionais, servicos, tendencia);
    }

    private List<SatisfacaoDashboardData.Distribuicao> montarDistribuicao(List<Object[]> rows, long total) {
        Map<Integer, Long> mapa = new HashMap<>();
        for (Object[] row : rows) mapa.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        List<SatisfacaoDashboardData.Distribuicao> resultado = new ArrayList<>();
        for (int nota = 5; nota >= 1; nota--) {
            long quantidade = mapa.getOrDefault(nota, 0L);
            double percentual = total == 0 ? 0 : arredondar(quantidade * 100.0 / total);
            resultado.add(new SatisfacaoDashboardData.Distribuicao(nota, quantidade, percentual));
        }
        return resultado;
    }

    private List<SatisfacaoDashboardData.Ranking> montarRanking(List<Object[]> rows) {
        List<SatisfacaoDashboardData.Ranking> resultado = new ArrayList<>();
        for (Object[] row : rows) {
            resultado.add(new SatisfacaoDashboardData.Ranking(
                    String.valueOf(row[0]),
                    ((Number) row[1]).longValue(),
                    arredondar(((Number) row[2]).doubleValue())));
        }
        return resultado;
    }

    private List<SatisfacaoDashboardData.Tendencia> montarTendencia(List<Object[]> rows, LocalDate inicio, LocalDate fim) {
        Map<YearMonth, SatisfacaoDashboardData.Tendencia> mapa = new HashMap<>();
        for (Object[] row : rows) {
            LocalDateTime mes = converterData(row[0]);
            if (mes == null) continue;
            YearMonth ym = YearMonth.from(mes);
            mapa.put(ym, new SatisfacaoDashboardData.Tendencia(
                    ym.format(MONTH_FORMAT),
                    ((Number) row[1]).longValue(),
                    arredondar(((Number) row[2]).doubleValue())));
        }
        YearMonth primeiro = YearMonth.from(inicio);
        YearMonth ultimo = YearMonth.from(fim);
        List<SatisfacaoDashboardData.Tendencia> resultado = new ArrayList<>();
        for (YearMonth cursor = primeiro; !cursor.isAfter(ultimo); cursor = cursor.plusMonths(1)) {
            resultado.add(mapa.getOrDefault(cursor,
                    new SatisfacaoDashboardData.Tendencia(cursor.format(MONTH_FORMAT), 0, 0)));
        }
        return resultado;
    }

    private LocalDateTime converterData(Object value) {
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof java.util.Date date) return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return null;
    }

    private double arredondar(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
