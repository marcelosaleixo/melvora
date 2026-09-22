package com.marceloaleixo.melvora.entity;

import com.marceloaleixo.melvora.entity.enums.MetodoMegaHair;
import com.marceloaleixo.melvora.entity.enums.TipoFio;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lotes_mega_hair", uniqueConstraints = @UniqueConstraint(name = "uk_lote_empresa_codigo", columnNames = {"empresa_id", "codigo"}), indexes = {
    @Index(name = "idx_lote_empresa", columnList = "empresa_id"),
    @Index(name = "idx_lote_empresa_produto", columnList = "empresa_id,produto_id")
})
public class LoteMegaHair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lote_empresa"))
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lote_produto"))
    private Produto produto;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String codigo;

    @NotNull
    @Min(1)
    @Column(name = "quantidade_inicial", nullable = false)
    private Integer quantidadeInicial;

    @NotNull
    @Min(0)
    @Column(name = "quantidade_disponivel", nullable = false)
    private Integer quantidadeDisponivel;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "peso_por_unidade_gramas", nullable = false, precision = 10, scale = 2)
    private BigDecimal pesoPorUnidadeGramas;

    @NotNull
    @Min(1)
    @Max(300)
    @Column(name = "comprimento_cm", nullable = false)
    private Integer comprimentoCm;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fio", nullable = false, length = 30)
    private TipoFio tipoFio;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String cor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MetodoMegaHair metodo;

    @Version
    private long version;

    protected LoteMegaHair() {
    }

    public LoteMegaHair(Empresa empresa, Produto produto, String codigo, Integer quantidade, BigDecimal peso, Integer comprimento,
            TipoFio tipoFio, String cor, MetodoMegaHair metodo) {
        this.empresa = empresa;
        this.produto = produto;
        this.codigo = codigo;
        this.quantidadeInicial = quantidade;
        this.quantidadeDisponivel = quantidade;
        this.pesoPorUnidadeGramas = peso;
        this.comprimentoCm = comprimento;
        this.tipoFio = tipoFio;
        this.cor = cor;
        this.metodo = metodo;
    }

    public void retirar(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        if (quantidade > quantidadeDisponivel) {
            throw new IllegalStateException("Estoque insuficiente para o lote " + codigo + ".");
        }
        quantidadeDisponivel -= quantidade;
    }

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public Produto getProduto() {
        return produto;
    }

    public String getCodigo() {
        return codigo;
    }

    public Integer getQuantidadeInicial() {
        return quantidadeInicial;
    }

    public Integer getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    public BigDecimal getPesoPorUnidadeGramas() {
        return pesoPorUnidadeGramas;
    }

    public Integer getComprimentoCm() {
        return comprimentoCm;
    }

    public TipoFio getTipoFio() {
        return tipoFio;
    }

    public String getCor() {
        return cor;
    }

    public MetodoMegaHair getMetodo() {
        return metodo;
    }
}
