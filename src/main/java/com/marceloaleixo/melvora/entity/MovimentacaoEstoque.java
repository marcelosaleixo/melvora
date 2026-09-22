package com.marceloaleixo.melvora.entity;

import com.marceloaleixo.melvora.entity.enums.TipoMovimentacaoEstoque;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes_estoque", indexes = {
    @Index(name = "idx_mov_empresa_data", columnList = "empresa_id,created_at"),
    @Index(name = "idx_mov_lote", columnList = "lote_id")
})
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mov_empresa"))
    private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mov_lote"))
    private LoteMegaHair lote;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aplicacao_id", foreignKey = @ForeignKey(name = "fk_mov_aplicacao"))
    private AplicacaoMegaHair aplicacao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacaoEstoque tipo;
    @Column(nullable = false)
    private int quantidade;
    @Column(name = "saldo_apos", nullable = false)
    private int saldoApos;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MovimentacaoEstoque() {
    }

    public MovimentacaoEstoque(Empresa empresa, LoteMegaHair lote, AplicacaoMegaHair aplicacao, TipoMovimentacaoEstoque tipo, int quantidade, int saldoApos) {
        this.empresa = empresa;
        this.lote = lote;
        this.aplicacao = aplicacao;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.saldoApos = saldoApos;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LoteMegaHair getLote() {
        return lote;
    }

    public TipoMovimentacaoEstoque getTipo() {
        return tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public int getSaldoApos() {
        return saldoApos;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
