package com.marceloaleixo.melvora.entity;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import jakarta.persistence.*;

@Entity
@Table(name = "empresa_modulos",
       uniqueConstraints = @UniqueConstraint(name = "uk_empresa_modulo", columnNames = {"empresa_id", "modulo"}),
       indexes = @Index(name = "idx_empresa_modulo_empresa", columnList = "empresa_id"))
public class EmpresaModulo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_empresa_modulo_empresa"))
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ModuloSistema modulo;

    @Column(nullable = false)
    private boolean ativo = true;

    protected EmpresaModulo() {}

    public EmpresaModulo(Empresa empresa, ModuloSistema modulo, boolean ativo) {
        this.empresa = empresa;
        this.modulo = modulo;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public ModuloSistema getModulo() { return modulo; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
