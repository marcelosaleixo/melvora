package com.marceloaleixo.melvora.dto;

import java.time.LocalDate;
import java.util.List;

public record SatisfacaoDashboardData(
        LocalDate inicio,
        LocalDate fim,
        double media,
        long respondidas,
        long pendentes,
        double percentualCincoEstrelas,
        List<Distribuicao> distribuicao,
        List<Ranking> profissionais,
        List<Ranking> servicos,
        List<Tendencia> tendencia) {

    public record Distribuicao(int nota, long quantidade, double percentual) {}

    public record Ranking(String nome, long quantidade, double media) {}

    public record Tendencia(String mes, long quantidade, double media) {}

    public String mediaFormatada() {
        return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", media);
    }

    public String percentualCincoEstrelasFormatado() {
        return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.0f%%", percentualCincoEstrelas);
    }
}
