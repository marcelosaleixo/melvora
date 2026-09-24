package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "avaliacoes_atendimento", indexes = {
        @Index(name = "idx_avaliacao_empresa_data", columnList = "empresa_id,created_at"),
        @Index(name = "idx_avaliacao_empresa_nota", columnList = "empresa_id,nota")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_avaliacao_atendimento", columnNames = "atendimento_id"),
        @UniqueConstraint(name = "uk_avaliacao_token", columnNames = "public_token")
})
public class AvaliacaoAtendimento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_avaliacao_empresa"))
    private Empresa empresa;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_avaliacao_atendimento"))
    private Atendimento atendimento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_avaliacao_cliente"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false, foreignKey = @ForeignKey(name = "fk_avaliacao_profissional"))
    private Usuario profissional;

    @Column(name = "public_token", nullable = false, length = 80)
    private String publicToken = UUID.randomUUID().toString();

    @Min(1) @Max(5)
    @Column(nullable = true)
    private Integer nota;

    @Size(max = 1000)
    @Column(length = 1000)
    private String comentario;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "respondida_at")
    private LocalDateTime respondidaAt;

    protected AvaliacaoAtendimento() {}

    public AvaliacaoAtendimento(Empresa empresa, Atendimento atendimento) {
        this.empresa = empresa;
        this.atendimento = atendimento;
        this.cliente = atendimento.getCliente();
        this.profissional = atendimento.getProfissional();
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Atendimento getAtendimento() { return atendimento; }
    public Cliente getCliente() { return cliente; }
    public Usuario getProfissional() { return profissional; }
    public String getPublicToken() { return publicToken; }
    public Integer getNota() { return nota; }
    public String getComentario() { return comentario; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getRespondidaAt() { return respondidaAt; }

    public boolean respondida() { return nota != null; }
    public void responder(Integer nota, String comentario) {
        if (nota == null || nota < 1 || nota > 5) throw new IllegalArgumentException("A nota deve estar entre 1 e 5.");
        this.nota = nota;
        this.comentario = comentario == null ? null : comentario.trim();
        this.respondidaAt = LocalDateTime.now();
    }
}
