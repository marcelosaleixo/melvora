package com.marceloaleixo.melvora.entity;

import com.marceloaleixo.melvora.entity.enums.MetodoMegaHair;
import com.marceloaleixo.melvora.entity.enums.TipoFio;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produtos_mega_hair", uniqueConstraints = @UniqueConstraint(name = "uk_mega_hair_produto", columnNames = "produto_id"))
public class ProdutoMegaHair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mega_hair_produto"))
    private Produto produto;

    @Column(name = "comprimento_cm", nullable = false)
    private Integer comprimentoCm;

    @Column(name = "peso_gramas", nullable = false, precision = 10, scale = 2)
    private BigDecimal pesoGramas;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fio", nullable = false, length = 30)
    private TipoFio tipoFio;

    @Column(nullable = false, length = 80)
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MetodoMegaHair metodo;

    @Column(length = 100)
    private String origem;

    protected ProdutoMegaHair() {
    }

    public ProdutoMegaHair(Produto produto, Integer comprimentoCm, BigDecimal pesoGramas, TipoFio tipoFio, String cor, MetodoMegaHair metodo, String origem) {
        this.produto = produto;
        this.comprimentoCm = comprimentoCm;
        this.pesoGramas = pesoGramas;
        this.tipoFio = tipoFio;
        this.cor = cor;
        this.metodo = metodo;
        this.origem = origem;
    }

    public void atualizar(Integer comprimentoCm, BigDecimal pesoGramas, TipoFio tipoFio, String cor, MetodoMegaHair metodo, String origem) {
        this.comprimentoCm = comprimentoCm;
        this.pesoGramas = pesoGramas;
        this.tipoFio = tipoFio;
        this.cor = cor;
        this.metodo = metodo;
        this.origem = origem;
    }

    public Integer getComprimentoCm() { return comprimentoCm; }
    public BigDecimal getPesoGramas() { return pesoGramas; }
    public TipoFio getTipoFio() { return tipoFio; }
    public String getCor() { return cor; }
    public MetodoMegaHair getMetodo() { return metodo; }
    public String getOrigem() { return origem; }
}
