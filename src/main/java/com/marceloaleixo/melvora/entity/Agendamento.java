package com.marceloaleixo.melvora.entity;

import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.entity.enums.TipoAgendamento;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos", indexes = {
        @Index(name = "idx_agendamento_empresa_inicio", columnList = "empresa_id,data_hora_inicio"),
        @Index(name = "idx_agendamento_empresa_profissional_inicio", columnList = "empresa_id,profissional_id,data_hora_inicio"),
        @Index(name = "idx_agendamento_empresa_cliente_inicio", columnList = "empresa_id,cliente_id,data_hora_inicio")
})
public class Agendamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_agendamento_empresa"))
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_agendamento_cliente"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false, foreignKey = @ForeignKey(name = "fk_agendamento_profissional"))
    private Usuario profissional;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoAgendamento tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", foreignKey = @ForeignKey(name = "fk_agendamento_servico"))
    private Servico servico;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private LocalDateTime dataHoraFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    @Size(max = 2000)
    @Column(length = 2000)
    private String observacoes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Agendamento() {}

    public Agendamento(Empresa empresa, Cliente cliente, Usuario profissional, TipoAgendamento tipo,
                       LocalDateTime inicio, LocalDateTime fim, String observacoes) {
        this.empresa = empresa;
        this.cliente = cliente;
        this.profissional = profissional;
        this.tipo = tipo;
        this.dataHoraInicio = inicio;
        this.dataHoraFim = fim;
        this.status = StatusAgendamento.AGENDADO;
        this.observacoes = observacoes;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Cliente getCliente() { return cliente; }
    public Usuario getProfissional() { return profissional; }
    public TipoAgendamento getTipo() { return tipo; }
    public Servico getServico() { return servico; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public StatusAgendamento getStatus() { return status; }
    public String getObservacoes() { return observacoes; }

    public void alterarStatus(StatusAgendamento status) { this.status = status; }

    public void definirServico(Servico servico) { this.servico = servico; }

    public void reagendar(TipoAgendamento tipo, Servico servico, LocalDateTime inicio, LocalDateTime fim, String observacoes) {
        this.tipo = tipo;
        this.servico = servico;
        this.dataHoraInicio = inicio;
        this.dataHoraFim = fim;
        this.observacoes = observacoes;
    }
}
