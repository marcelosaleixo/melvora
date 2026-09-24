package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_confirmacoes_pendentes", indexes = {
        @Index(name = "idx_wa_confirmacao_empresa_telefone_status", columnList = "empresa_id,telefone,status"),
        @Index(name = "idx_wa_confirmacao_expira", columnList = "status,expira_em")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_wa_confirmacao_agendamento", columnNames = "agendamento_id")
})
public class WhatsAppConfirmacaoPendente {
    public enum Status { AGUARDANDO_RESPOSTA, CONFIRMADA, RECUSADA, EXPIRADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wa_confirmacao_empresa"))
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wa_confirmacao_cliente"))
    private Cliente cliente;

    @Column(name = "agendamento_id", nullable = false)
    private Long agendamentoId;

    @Column(nullable = false, length = 30)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.AGUARDANDO_RESPOSTA;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected WhatsAppConfirmacaoPendente() {}

    public WhatsAppConfirmacaoPendente(Empresa empresa, Cliente cliente, Long agendamentoId,
                                       String telefone, LocalDateTime expiraEm) {
        this.empresa = empresa;
        this.cliente = cliente;
        this.agendamentoId = agendamentoId;
        this.telefone = telefone;
        this.expiraEm = expiraEm;
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Cliente getCliente() { return cliente; }
    public Long getAgendamentoId() { return agendamentoId; }
    public String getTelefone() { return telefone; }
    public Status getStatus() { return status; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public boolean expirada() { return LocalDateTime.now().isAfter(expiraEm); }
    public void confirmar() { status = Status.CONFIRMADA; updatedAt = LocalDateTime.now(); }
    public void recusar() { status = Status.RECUSADA; updatedAt = LocalDateTime.now(); }
    public void expirar() { status = Status.EXPIRADA; updatedAt = LocalDateTime.now(); }
}
