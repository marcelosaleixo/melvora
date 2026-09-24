package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_agendamento_acoes_pendentes", indexes = {
        @Index(name = "idx_wa_agenda_acao_empresa_telefone_status", columnList = "empresa_id,telefone,status"),
        @Index(name = "idx_wa_agenda_acao_expira", columnList = "status,expira_em")
})
public class WhatsAppAgendamentoAcaoPendente {
    public enum Acao { CANCELAR, REMARCAR }
    public enum Status {
        AGUARDANDO_CONFIRMACAO_CANCELAMENTO,
        AGUARDANDO_DATA_REAGENDAMENTO,
        AGUARDANDO_HORARIO_REAGENDAMENTO,
        AGUARDANDO_CONFIRMACAO_REAGENDAMENTO,
        CONCLUIDA,
        EXPIRADA,
        CANCELADA
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wa_agenda_acao_empresa"))
    private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wa_agenda_acao_cliente"))
    private Cliente cliente;
    @Column(name = "agendamento_id", nullable = false)
    private Long agendamentoId;
    @Column(nullable = false, length = 30)
    private String telefone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Acao acao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 45)
    private Status status;
    @Column(name = "data_desejada")
    private java.time.LocalDate dataDesejada;
    @Column(name = "opcoes_json", columnDefinition = "TEXT")
    private String opcoesJson;
    @Column(name = "profissional_id")
    private Long profissionalId;
    @Column(name = "inicio_selecionado")
    private LocalDateTime inicioSelecionado;
    @Column(name = "fim_selecionado")
    private LocalDateTime fimSelecionado;
    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected WhatsAppAgendamentoAcaoPendente() {}

    private WhatsAppAgendamentoAcaoPendente(Empresa empresa, Cliente cliente, Long agendamentoId,
                                             String telefone, Acao acao, Status status,
                                             LocalDateTime expiraEm) {
        this.empresa = empresa;
        this.cliente = cliente;
        this.agendamentoId = agendamentoId;
        this.telefone = telefone;
        this.acao = acao;
        this.status = status;
        this.expiraEm = expiraEm;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static WhatsAppAgendamentoAcaoPendente cancelar(Empresa empresa, Cliente cliente, Long agendamentoId,
                                                            String telefone, LocalDateTime expiraEm) {
        return new WhatsAppAgendamentoAcaoPendente(empresa, cliente, agendamentoId, telefone, Acao.CANCELAR,
                Status.AGUARDANDO_CONFIRMACAO_CANCELAMENTO, expiraEm);
    }

    public static WhatsAppAgendamentoAcaoPendente remarcar(Empresa empresa, Cliente cliente, Long agendamentoId,
                                                            String telefone, LocalDateTime expiraEm) {
        return new WhatsAppAgendamentoAcaoPendente(empresa, cliente, agendamentoId, telefone, Acao.REMARCAR,
                Status.AGUARDANDO_DATA_REAGENDAMENTO, expiraEm);
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Cliente getCliente() { return cliente; }
    public Long getAgendamentoId() { return agendamentoId; }
    public String getTelefone() { return telefone; }
    public Acao getAcao() { return acao; }
    public Status getStatus() { return status; }
    public java.time.LocalDate getDataDesejada() { return dataDesejada; }
    public String getOpcoesJson() { return opcoesJson; }
    public Long getProfissionalId() { return profissionalId; }
    public LocalDateTime getInicioSelecionado() { return inicioSelecionado; }
    public LocalDateTime getFimSelecionado() { return fimSelecionado; }
    public LocalDateTime getExpiraEm() { return expiraEm; }

    public boolean expirada() { return LocalDateTime.now().isAfter(expiraEm); }
    public void definirData(java.time.LocalDate data) { this.dataDesejada = data; this.status = Status.AGUARDANDO_HORARIO_REAGENDAMENTO; this.updatedAt = LocalDateTime.now(); }
    public void aguardarNovaData() { this.status = Status.AGUARDANDO_DATA_REAGENDAMENTO; this.updatedAt = LocalDateTime.now(); }
    public void definirOpcoes(String opcoesJson) { this.opcoesJson = opcoesJson; this.status = Status.AGUARDANDO_HORARIO_REAGENDAMENTO; this.updatedAt = LocalDateTime.now(); }
    public void selecionarHorario(Long profissionalId, LocalDateTime inicio, LocalDateTime fim) {
        this.profissionalId = profissionalId;
        this.inicioSelecionado = inicio;
        this.fimSelecionado = fim;
        this.status = Status.AGUARDANDO_CONFIRMACAO_REAGENDAMENTO;
        this.updatedAt = LocalDateTime.now();
    }
    public void concluir() { this.status = Status.CONCLUIDA; this.updatedAt = LocalDateTime.now(); }
    public void expirar() { this.status = Status.EXPIRADA; this.updatedAt = LocalDateTime.now(); }
    public void cancelar() { this.status = Status.CANCELADA; this.updatedAt = LocalDateTime.now(); }
}
