package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "clientes", indexes = {
    @Index(name = "idx_cliente_empresa", columnList = "empresa_id"),
    @Index(name = "idx_cliente_empresa_nome", columnList = "empresa_id,nome")
})
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cliente_empresa"))
    private Empresa empresa;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nome;

    @Size(max = 30)
    @Column(length = 30)
    private String telefone;

    @Email
    @Size(max = 180)
    @Column(length = 180)
    private String email;

    @Size(max = 1000)
    @Column(length = 1000)
    private String observacoes;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Cliente() {
    }

    public Cliente(Empresa empresa, String nome, String telefone, String email, String observacoes) {
        this.empresa = empresa;
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.observacoes = observacoes;
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

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void atualizar(String nome, String telefone, String email, String observacoes) {
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.observacoes = observacoes;
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
