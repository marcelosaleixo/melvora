package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "servicos", indexes = {
        @Index(name = "idx_servico_empresa", columnList = "empresa_id"),
        @Index(name = "idx_servico_empresa_ativo", columnList = "empresa_id,ativo")
})
public class Servico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_servico_empresa"))
    private Empresa empresa;

    @Column(nullable = false, length = 150)
    private String nome;

    @Size(max = 1000)
    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "servicos_profissionais",
            joinColumns = @JoinColumn(name = "servico_id", foreignKey = @ForeignKey(name = "fk_servico_profissional_servico")),
            inverseJoinColumns = @JoinColumn(name = "profissional_id", foreignKey = @ForeignKey(name = "fk_servico_profissional_usuario")),
            uniqueConstraints = @UniqueConstraint(name = "uk_servico_profissional", columnNames = {"servico_id", "profissional_id"}))
    private Set<Usuario> profissionais = new LinkedHashSet<>();

    protected Servico() {}

    public Servico(Empresa empresa, String nome, String descricao, String categoria,
                   Integer duracaoMinutos, BigDecimal preco) {
        this.empresa = empresa;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.duracaoMinutos = duracaoMinutos;
        this.preco = preco;
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public BigDecimal getPreco() { return preco; }
    public boolean isAtivo() { return ativo; }
    public Set<Usuario> getProfissionais() { return profissionais; }

    public void atualizar(String nome, String descricao, String categoria,
                          Integer duracaoMinutos, BigDecimal preco) {
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.duracaoMinutos = duracaoMinutos;
        this.preco = preco;
    }

    public void definirAtivo(boolean ativo) { this.ativo = ativo; }

    public void substituirProfissionais(Set<Usuario> profissionais) {
        this.profissionais.clear();
        if (profissionais != null) this.profissionais.addAll(profissionais);
    }

    public boolean podeSerExecutadoPor(Long profissionalId) {
        return profissionais.isEmpty() || profissionais.stream().anyMatch(p -> p.getId().equals(profissionalId));
    }
}
