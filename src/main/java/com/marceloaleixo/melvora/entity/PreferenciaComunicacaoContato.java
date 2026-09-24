package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "preferencias_comunicacao_contato", uniqueConstraints = {
    @UniqueConstraint(name = "uk_pref_com_empresa_telefone", columnNames = {"empresa_id", "telefone"})
}, indexes = {
    @Index(name = "idx_pref_com_empresa_cliente", columnList = "empresa_id,cliente_id"),
    @Index(name = "idx_pref_com_empresa_optin", columnList = "empresa_id,retencao_opt_in")
})
public class PreferenciaComunicacaoContato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pref_com_empresa"))
    private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", foreignKey = @ForeignKey(name = "fk_pref_com_cliente"))
    private Cliente cliente;
    @Column(nullable = false, length = 30)
    private String telefone;
    @Column(name = "retencao_opt_in", nullable = false)
    private boolean retencaoOptIn;
    @Column(name = "retencao_opt_in_at")
    private LocalDateTime retencaoOptInAt;
    @Column(name = "retencao_opt_out_at")
    private LocalDateTime retencaoOptOutAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected PreferenciaComunicacaoContato() {}
    public PreferenciaComunicacaoContato(Empresa empresa, Cliente cliente, String telefone) {
        this.empresa=empresa; this.cliente=cliente; this.telefone=telefone;
    }
    public Long getId(){return id;} public Empresa getEmpresa(){return empresa;} public Cliente getCliente(){return cliente;} public String getTelefone(){return telefone;}
    public boolean isRetencaoOptIn(){return retencaoOptIn;} public LocalDateTime getRetencaoOptInAt(){return retencaoOptInAt;} public LocalDateTime getRetencaoOptOutAt(){return retencaoOptOutAt;}
    public void vincularCliente(Cliente cliente){this.cliente=cliente; this.updatedAt=LocalDateTime.now();}
    public void permitirRetencao(){this.retencaoOptIn=true; this.retencaoOptInAt=LocalDateTime.now(); this.retencaoOptOutAt=null; this.updatedAt=LocalDateTime.now();}
    public void bloquearRetencao(){this.retencaoOptIn=false; this.retencaoOptOutAt=LocalDateTime.now(); this.updatedAt=LocalDateTime.now();}
}
