package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "aplicacoes_mega_hair", indexes = {
    @Index(name = "idx_aplicacao_empresa_cliente", columnList = "empresa_id,cliente_id"),
    @Index(name = "idx_aplicacao_empresa_data", columnList = "empresa_id,data_aplicacao")
})
public class AplicacaoMegaHair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacao_empresa"))
    private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacao_cliente"))
    private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacao_profissional"))
    private Usuario profissional;
    @Column(name = "data_aplicacao", nullable = false)
    private LocalDate dataAplicacao;
    @Size(max = 2000)
    @Column(length = 2000)
    private String observacoes;
    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt;

    @OneToMany(mappedBy = "aplicacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AplicacaoLote> lotes = new ArrayList<>();

    protected AplicacaoMegaHair() {
    }

    public AplicacaoMegaHair(Empresa empresa, Cliente cliente, Usuario profissional, LocalDate dataAplicacao, String observacoes) {
        this.empresa = empresa;
        this.cliente = cliente;
        this.profissional = profissional;
        this.dataAplicacao = dataAplicacao;
        this.observacoes = observacoes;
        this.createdAt = java.time.LocalDateTime.now();
    }

    public void adicionarLote(AplicacaoLote item) {
        lotes.add(item);
        item.setAplicacao(this);
    }

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Usuario getProfissional() {
        return profissional;
    }

    public LocalDate getDataAplicacao() {
        return dataAplicacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public List<AplicacaoLote> getLotes() {
        return Collections.unmodifiableList(lotes);
    }
}
