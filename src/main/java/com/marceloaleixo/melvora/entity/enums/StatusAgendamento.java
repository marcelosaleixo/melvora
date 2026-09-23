package com.marceloaleixo.melvora.entity.enums;

public enum StatusAgendamento {
    AGENDADO("Agendado"),
    CONFIRMADO("Confirmado"),
    EM_ATENDIMENTO("Em atendimento"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado"),
    FALTOU("Faltou");

    private final String nome;

    StatusAgendamento(String nome) { this.nome = nome; }
    public String getNome() { return nome; }

    public boolean ocupaHorario() {
        return this != CANCELADO && this != FALTOU && this != CONCLUIDO;
    }
}
