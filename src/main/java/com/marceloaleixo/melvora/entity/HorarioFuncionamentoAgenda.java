package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "agenda_horarios_funcionamento", uniqueConstraints = @UniqueConstraint(name = "uk_agenda_horario_empresa_dia", columnNames = {"empresa_id", "dia_semana"}), indexes = @Index(name = "idx_agenda_horario_empresa", columnList = "empresa_id"))
public class HorarioFuncionamentoAgenda {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_agenda_horario_empresa"))
    private Empresa empresa;
    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;
    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;
    @Column(name = "intervalo_minutos", nullable = false)
    private int intervaloMinutos = 15;
    @Column(nullable = false)
    private boolean ativo = true;
    protected HorarioFuncionamentoAgenda() {}
    public HorarioFuncionamentoAgenda(Empresa empresa, DayOfWeek dia, LocalTime inicio, LocalTime fim, int intervalo, boolean ativo) {
        this.empresa=empresa; this.diaSemana=dia.getValue(); this.horaInicio=inicio; this.horaFim=fim; this.intervaloMinutos=intervalo; this.ativo=ativo;
    }
    public Long getId(){return id;} public Empresa getEmpresa(){return empresa;} public int getDiaSemana(){return diaSemana;}
    public DayOfWeek getDia(){return DayOfWeek.of(diaSemana);} public LocalTime getHoraInicio(){return horaInicio;} public LocalTime getHoraFim(){return horaFim;}
    public int getIntervaloMinutos(){return intervaloMinutos;} public boolean isAtivo(){return ativo;}
    public void atualizar(LocalTime inicio, LocalTime fim, int intervalo, boolean ativo){this.horaInicio=inicio;this.horaFim=fim;this.intervaloMinutos=intervalo;this.ativo=ativo;}
}
