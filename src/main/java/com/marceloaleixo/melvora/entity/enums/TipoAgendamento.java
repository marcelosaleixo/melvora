package com.marceloaleixo.melvora.entity.enums;

public enum TipoAgendamento {
    APLICACAO_MEGA_HAIR("Aplicação Mega Hair"),
    MANUTENCAO_MEGA_HAIR("Manutenção Mega Hair"),
    OUTRO("Outro atendimento");

    private final String nome;
    TipoAgendamento(String nome) { this.nome = nome; }
    public String getNome() { return nome; }
}
