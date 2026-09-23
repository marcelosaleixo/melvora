package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "manutencoes_mega_hair", indexes = {
    @Index(name = "idx_manut_empresa_cliente", columnList = "empresa_id,cliente_id"),
    @Index(name = "idx_manut_aplicacao", columnList = "aplicacao_id")
})
public class ManutencaoMegaHair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_manut_empresa"))
    private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_manut_cliente"))
    private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aplicacao_id", nullable = false, foreignKey = @ForeignKey(name = "fk_manut_aplicacao"))
    private AplicacaoMegaHair aplicacao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false, foreignKey = @ForeignKey(name = "fk_manut_profissional"))
    private Usuario profissional;
    @Column(name = "data_manutencao", nullable = false)
    private LocalDate dataManutencao;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", foreignKey = @ForeignKey(name = "fk_manutencao_agendamento"))
    private Agendamento agendamento;
    @Column(nullable = false, length = 30)
    private String tipo;
    @Size(max = 2000)
    @Column(length = 2000)
    private String observacoes;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ManutencaoMegaHair() {
    }

    public ManutencaoMegaHair(Empresa empresa, Cliente cliente, AplicacaoMegaHair aplicacao, Usuario profissional, LocalDate data, String tipo, String observacoes) {
        this.empresa = empresa;
        this.cliente = cliente;
        this.aplicacao = aplicacao;
        this.profissional = profissional;
        this.dataManutencao = data;
        this.tipo = tipo;
        this.observacoes = observacoes;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public AplicacaoMegaHair getAplicacao() {
        return aplicacao;
    }

    public Usuario getProfissional() {
        return profissional;
    }

    public LocalDate getDataManutencao() {
        return dataManutencao;
    }

    public Agendamento getAgendamento() { return agendamento; }

    public void vincularAgendamento(Agendamento agendamento) { this.agendamento = agendamento; }

    public String getTipo() {
        return tipo;
    }

    public String getObservacoes() {
        return observacoes;
    }
}
