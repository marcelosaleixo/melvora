package com.marceloaleixo.melvora.entity;

import com.marceloaleixo.melvora.entity.enums.StatusComissao;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "comissoes", indexes = {
    @Index(name = "idx_comissao_empresa_data", columnList = "empresa_id,data_comissao"),
    @Index(name = "idx_comissao_empresa_profissional", columnList = "empresa_id,profissional_id,status"),
    @Index(name = "idx_comissao_empresa_status", columnList = "empresa_id,status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_comissao_lancamento", columnNames = "lancamento_id")
})
public class Comissao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comissao_empresa"))
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comissao_profissional"))
    private Usuario profissional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lancamento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comissao_lancamento"))
    private LancamentoFinanceiro lancamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendimento_id", foreignKey = @ForeignKey(name = "fk_comissao_atendimento"))
    private Atendimento atendimento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseCalculo;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentual;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusComissao status;

    @Column(name = "data_comissao", nullable = false)
    private LocalDate dataComissao;

    @Column(name = "pago_em")
    private LocalDateTime pagoEm;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Comissao() {}

    public Comissao(Empresa empresa, Usuario profissional, LancamentoFinanceiro lancamento,
                     BigDecimal baseCalculo, BigDecimal percentual, BigDecimal valor, LocalDate dataComissao) {
        this.empresa = empresa;
        this.profissional = profissional;
        this.lancamento = lancamento;
        this.atendimento = lancamento.getAtendimento();
        this.baseCalculo = baseCalculo;
        this.percentual = percentual;
        this.valor = valor;
        this.status = StatusComissao.PENDENTE;
        this.dataComissao = dataComissao;
        this.createdAt = LocalDateTime.now();
    }

    public void pagar() {
        if (status == StatusComissao.CANCELADO) throw new IllegalStateException("Comissão cancelada não pode ser paga.");
        status = StatusComissao.PAGO;
        pagoEm = LocalDateTime.now();
    }

    public void cancelar() {
        if (status == StatusComissao.PAGO) throw new IllegalStateException("Comissão paga não pode ser cancelada.");
        status = StatusComissao.CANCELADO;
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Usuario getProfissional() { return profissional; }
    public LancamentoFinanceiro getLancamento() { return lancamento; }
    public Atendimento getAtendimento() { return atendimento; }
    public BigDecimal getBaseCalculo() { return baseCalculo; }
    public BigDecimal getPercentual() { return percentual; }
    public BigDecimal getValor() { return valor; }
    public StatusComissao getStatus() { return status; }
    public LocalDate getDataComissao() { return dataComissao; }
    public LocalDateTime getPagoEm() { return pagoEm; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
