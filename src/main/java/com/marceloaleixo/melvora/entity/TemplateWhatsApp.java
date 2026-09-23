package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "templates_whatsapp", indexes = {
        @Index(name = "idx_template_whatsapp_empresa", columnList = "empresa_id"),
        @Index(name = "idx_template_whatsapp_empresa_ativo", columnList = "empresa_id,ativo")
})
public class TemplateWhatsApp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_template_whatsapp_empresa"))
    private Empresa empresa;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nome;

    @NotBlank
    @Size(max = 4000)
    @Column(nullable = false, length = 4000)
    private String mensagem;

    @Column(nullable = false)
    private boolean ativo = true;

    protected TemplateWhatsApp() {}

    public TemplateWhatsApp(Empresa empresa, String nome, String mensagem) {
        this.empresa = empresa;
        this.nome = nome;
        this.mensagem = mensagem;
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public String getNome() { return nome; }
    public String getMensagem() { return mensagem; }
    public boolean isAtivo() { return ativo; }

    public void atualizar(String nome, String mensagem) {
        this.nome = nome;
        this.mensagem = mensagem;
    }

    public void definirAtivo(boolean ativo) { this.ativo = ativo; }
}
