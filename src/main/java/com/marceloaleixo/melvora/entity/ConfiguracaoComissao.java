package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "configuracoes_comissao", uniqueConstraints = {
    @UniqueConstraint(name = "uk_config_comissao_empresa_profissional", columnNames = {"empresa_id", "profissional_id"})
})
public class ConfiguracaoComissao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_config_comissao_empresa"))
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false, foreignKey = @ForeignKey(name = "fk_config_comissao_profissional"))
    private Usuario profissional;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentual;

    @Column(nullable = false)
    private boolean ativo = true;

    protected ConfiguracaoComissao() {}

    public ConfiguracaoComissao(Empresa empresa, Usuario profissional, BigDecimal percentual) {
        this.empresa = empresa;
        this.profissional = profissional;
        this.percentual = percentual;
        this.ativo = true;
    }

    public void atualizar(BigDecimal percentual) { this.percentual = percentual; this.ativo = true; }
    public void desativar() { this.ativo = false; }
    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Usuario getProfissional() { return profissional; }
    public BigDecimal getPercentual() { return percentual; }
    public boolean isAtivo() { return ativo; }
}
