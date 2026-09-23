package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "atendimentos", indexes = {
        @Index(name = "idx_atendimento_empresa_data", columnList = "empresa_id,data_hora_inicio"),
        @Index(name = "idx_atendimento_empresa_cliente", columnList = "empresa_id,cliente_id,data_hora_inicio"),
        @Index(name = "idx_atendimento_empresa_profissional", columnList = "empresa_id,profissional_id,data_hora_inicio")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_atendimento_agendamento", columnNames = "agendamento_id")
})
public class Atendimento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_atendimento_empresa"))
    private Empresa empresa;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_atendimento_agendamento"))
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_atendimento_cliente"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false, foreignKey = @ForeignKey(name = "fk_atendimento_profissional"))
    private Usuario profissional;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "servico_nome", length = 150)
    private String servicoNome;

    @Column(name = "preco_tabela", precision = 12, scale = 2)
    private BigDecimal precoTabela;

    @Column(name = "valor_cobrado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorCobrado;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private LocalDateTime dataHoraFim;

    @Column(name = "observacoes", length = 2000)
    private String observacoes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Atendimento() {}

    public Atendimento(Empresa empresa, Agendamento agendamento, BigDecimal valorCobrado) {
        this.empresa = empresa;
        this.agendamento = agendamento;
        this.cliente = agendamento.getCliente();
        this.profissional = agendamento.getProfissional();
        this.tipo = agendamento.getTipo().name();
        this.servicoNome = agendamento.getServico() == null ? null : agendamento.getServico().getNome();
        this.precoTabela = agendamento.getServico() == null ? null : agendamento.getServico().getPreco();
        this.valorCobrado = valorCobrado;
        this.duracaoMinutos = (int) java.time.Duration.between(agendamento.getDataHoraInicio(), agendamento.getDataHoraFim()).toMinutes();
        this.dataHoraInicio = agendamento.getDataHoraInicio();
        this.dataHoraFim = agendamento.getDataHoraFim();
        this.observacoes = agendamento.getObservacoes();
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Agendamento getAgendamento() { return agendamento; }
    public Cliente getCliente() { return cliente; }
    public Usuario getProfissional() { return profissional; }
    public String getTipo() { return tipo; }
    public String getServicoNome() { return servicoNome; }
    public BigDecimal getPrecoTabela() { return precoTabela; }
    public BigDecimal getValorCobrado() { return valorCobrado; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public String getObservacoes() { return observacoes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
