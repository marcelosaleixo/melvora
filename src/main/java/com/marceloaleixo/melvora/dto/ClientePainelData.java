package com.marceloaleixo.melvora.dto;

import com.marceloaleixo.melvora.entity.Agendamento;
import java.util.List;

/** Dados operacionais agregados da ficha da cliente. Nunca contém empresaId vindo do navegador. */
public record ClientePainelData(
        Agendamento proximoAtendimento,
        Agendamento ultimoAtendimento,
        List<Agendamento> atendimentosRecentes,
        long totalAtendimentos,
        long atendimentosConcluidos,
        long atendimentosCancelados) {
    public boolean possuiProximoAtendimento() { return proximoAtendimento != null; }
}
