package com.marceloaleixo.melvora.dto;

import com.marceloaleixo.melvora.entity.Agendamento;
import com.marceloaleixo.melvora.entity.Atendimento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Visão agregada da jornada da cliente. Dados sempre filtrados pelo tenant no service. */
public record ClienteCrm360Data(
        Agendamento proximoAtendimento,
        Agendamento ultimoAgendamento,
        long totalAtendimentos,
        long atendimentosConcluidos,
        long atendimentosCancelados,
        List<Atendimento> atendimentosRecentes,
        BigDecimal totalReceitasPagas,
        long quantidadeReceitasPagas,
        Double mediaAvaliacao,
        long avaliacoesRespondidas,
        long mensagensNaoLidas,
        LocalDateTime ultimaMensagem,
        String ultimaMensagemTexto,
        boolean retencaoOptIn,
        boolean retencaoOptOut) {

    public boolean possuiProximoAtendimento() { return proximoAtendimento != null; }
    public boolean possuiUltimoAtendimento() { return ultimoAgendamento != null; }
    public boolean possuiFinanceiro() { return totalReceitasPagas != null; }
    public boolean possuiAvaliacao() { return avaliacoesRespondidas > 0; }
    public boolean possuiWhatsApp() { return ultimaMensagem != null || mensagensNaoLidas > 0; }
}
