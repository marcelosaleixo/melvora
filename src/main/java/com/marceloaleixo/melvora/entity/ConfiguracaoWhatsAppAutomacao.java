package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracoes_whatsapp_automacao")
public class ConfiguracaoWhatsAppAutomacao {
    @Id
    private Long empresaId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "empresa_id", foreignKey = @ForeignKey(name = "fk_whatsapp_automacao_empresa"))
    private Empresa empresa;

    @Column(nullable = false)
    private boolean ativa = false;

    @Column(name = "responder_saudacao", nullable = false)
    private boolean responderSaudacao = true;

    @Column(name = "responder_servicos", nullable = false)
    private boolean responderServicos = true;

    @Column(name = "responder_preco", nullable = false)
    private boolean responderPreco = true;

    @Column(name = "responder_agendamento", nullable = false)
    private boolean responderAgendamento = true;

    @Column(name = "encaminhar_humano", nullable = false)
    private boolean encaminharHumano = true;

    protected ConfiguracaoWhatsAppAutomacao() {}

    public ConfiguracaoWhatsAppAutomacao(Empresa empresa) {
        this.empresa = empresa;
        this.empresaId = empresa.getId();
    }

    public Long getEmpresaId() { return empresaId; }
    public Empresa getEmpresa() { return empresa; }
    public boolean isAtiva() { return ativa; }
    public boolean isResponderSaudacao() { return responderSaudacao; }
    public boolean isResponderServicos() { return responderServicos; }
    public boolean isResponderPreco() { return responderPreco; }
    public boolean isResponderAgendamento() { return responderAgendamento; }
    public boolean isEncaminharHumano() { return encaminharHumano; }

    public void atualizar(boolean ativa, boolean responderSaudacao, boolean responderServicos,
                          boolean responderPreco, boolean responderAgendamento, boolean encaminharHumano) {
        this.ativa = ativa;
        this.responderSaudacao = responderSaudacao;
        this.responderServicos = responderServicos;
        this.responderPreco = responderPreco;
        this.responderAgendamento = responderAgendamento;
        this.encaminharHumano = encaminharHumano;
    }
}
