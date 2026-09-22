package com.marceloaleixo.melvora.entity;

import com.marceloaleixo.melvora.entity.enums.TipoProduto;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produtos", indexes = @Index(name = "idx_produto_empresa", columnList = "empresa_id"))
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_produto_empresa"))
    private Empresa empresa;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoProduto tipo;

    @Column(name = "preco_venda", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "preco_custo", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoCusto;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Produto() {
    }

    public Produto(Empresa empresa, String nome, TipoProduto tipo, BigDecimal precoVenda, BigDecimal precoCusto) {
        this.empresa = empresa;
        this.nome = nome;
        this.tipo = tipo;
        this.precoVenda = precoVenda;
        this.precoCusto = precoCusto;
    }

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getNome() {
        return nome;
    }

    public TipoProduto getTipo() {
        return tipo;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public BigDecimal getPrecoCusto() {
        return precoCusto;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void atualizar(String nome, TipoProduto tipo, BigDecimal precoVenda, BigDecimal precoCusto) {
        this.nome = nome;
        this.tipo = tipo;
        this.precoVenda = precoVenda;
        this.precoCusto = precoCusto;
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
