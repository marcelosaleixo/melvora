package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "aplicacao_lotes", uniqueConstraints = @UniqueConstraint(name = "uk_aplicacao_lote", columnNames = {"aplicacao_id", "lote_id"}))
public class AplicacaoLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aplicacao_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacao_lote_aplicacao"))
    private AplicacaoMegaHair aplicacao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacao_lote_lote"))
    private LoteMegaHair lote;
    @Column(nullable = false)
    private int quantidade;
    @Column(name = "peso_total_gramas", nullable = false, precision = 12, scale = 2)
    private BigDecimal pesoTotalGramas;
    @Column(name = "comprimento_cm", nullable = false)
    private int comprimentoCm;
    @Column(name = "tipo_fio", nullable = false, length = 30)
    private String tipoFio;
    @Column(nullable = false, length = 80)
    private String cor;
    @Column(nullable = false, length = 40)
    private String metodo;

    protected AplicacaoLote() {
    }

    public AplicacaoLote(LoteMegaHair lote, int quantidade) {
        this.lote = lote;
        this.quantidade = quantidade;
        this.pesoTotalGramas = lote.getPesoPorUnidadeGramas().multiply(BigDecimal.valueOf(quantidade));
        this.comprimentoCm = lote.getComprimentoCm();
        this.tipoFio = lote.getTipoFio().name();
        this.cor = lote.getCor();
        this.metodo = lote.getMetodo().name();
    }

    void setAplicacao(AplicacaoMegaHair aplicacao) {
        this.aplicacao = aplicacao;
    }

    public Long getId() {
        return id;
    }

    public LoteMegaHair getLote() {
        return lote;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPesoTotalGramas() {
        return pesoTotalGramas;
    }

    public int getComprimentoCm() {
        return comprimentoCm;
    }

    public String getTipoFio() {
        return tipoFio;
    }

    public String getCor() {
        return cor;
    }

    public String getMetodo() {
        return metodo;
    }
}
