package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "configuracoes_comunicacao")
public class ConfiguracaoComunicacao {
    @Id
    private Long empresaId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "empresa_id", foreignKey = @ForeignKey(name = "fk_config_comunicacao_empresa"))
    private Empresa empresa;

    @Column(name = "confirmacao_ativa", nullable = false)
    private boolean confirmacaoAtiva = true;
    @Min(15) @Max(10080)
    @Column(name = "confirmacao_minutos_antes", nullable = false)
    private int confirmacaoMinutosAntes = 1440;

    @Column(name = "lembrete_ativo", nullable = false)
    private boolean lembreteAtivo = true;
    @Min(15) @Max(10080)
    @Column(name = "lembrete_minutos_antes", nullable = false)
    private int lembreteMinutosAntes = 120;

    @Column(name = "pos_atendimento_ativo", nullable = false)
    private boolean posAtendimentoAtivo = false;
    @Min(0) @Max(10080)
    @Column(name = "pos_atendimento_minutos_depois", nullable = false)
    private int posAtendimentoMinutosDepois = 60;

    protected ConfiguracaoComunicacao() {}
    public ConfiguracaoComunicacao(Empresa empresa) { this.empresa = empresa; }

    public Long getEmpresaId() { return empresaId; }
    public Empresa getEmpresa() { return empresa; }
    public boolean isConfirmacaoAtiva() { return confirmacaoAtiva; }
    public int getConfirmacaoMinutosAntes() { return confirmacaoMinutosAntes; }
    public boolean isLembreteAtivo() { return lembreteAtivo; }
    public int getLembreteMinutosAntes() { return lembreteMinutosAntes; }
    public boolean isPosAtendimentoAtivo() { return posAtendimentoAtivo; }
    public int getPosAtendimentoMinutosDepois() { return posAtendimentoMinutosDepois; }

    public void atualizar(boolean confirmacaoAtiva, int confirmacaoMinutosAntes,
                          boolean lembreteAtivo, int lembreteMinutosAntes,
                          boolean posAtendimentoAtivo, int posAtendimentoMinutosDepois) {
        this.confirmacaoAtiva = confirmacaoAtiva;
        this.confirmacaoMinutosAntes = confirmacaoMinutosAntes;
        this.lembreteAtivo = lembreteAtivo;
        this.lembreteMinutosAntes = lembreteMinutosAntes;
        this.posAtendimentoAtivo = posAtendimentoAtivo;
        this.posAtendimentoMinutosDepois = posAtendimentoMinutosDepois;
    }
}
