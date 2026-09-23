package com.marceloaleixo.melvora.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioData(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal receitasPagas,
        BigDecimal despesasPagas,
        BigDecimal comissoesPagas,
        BigDecimal comissoesPendentes,
        BigDecimal saldo,
        long atendimentos,
        long clientesAtendidos,
        List<Item> servicosMaisRealizados,
        List<Item> profissionaisPorFaturamento) {

    public record Item(String nome, long quantidade, BigDecimal valor) {}
}
