package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.marceloaleixo.melvora.entity.enums.WhatsAppIntencao;

@Entity
@Table(name = "whatsapp_mensagens", indexes = {
        @Index(name = "idx_whatsapp_mensagem_empresa_data", columnList = "empresa_id,recebido_em"),
        @Index(name = "idx_whatsapp_mensagem_empresa_cliente", columnList = "empresa_id,cliente_id,recebido_em"),
        @Index(name = "idx_whatsapp_mensagem_empresa_telefone", columnList = "empresa_id,telefone")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_whatsapp_mensagem_provider", columnNames = {"empresa_id", "provider_message_id"})
})
public class WhatsAppMensagem {
    public enum Direcao { ENTRADA, SAIDA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_whatsapp_mensagem_empresa"))
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", foreignKey = @ForeignKey(name = "fk_whatsapp_mensagem_cliente"))
    private Cliente cliente;

    @Column(name = "provider_message_id", nullable = false, length = 200)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direcao", nullable = false, length = 10)
    private Direcao direcao;

    @Column(name = "telefone", nullable = false, length = 30)
    private String telefone;

    @Column(name = "push_name", length = 150)
    private String pushName;

    @Column(name = "mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "recebido_em", nullable = false)
    private LocalDateTime recebidoEm;

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "automacao_intencao", length = 30)
    private WhatsAppIntencao automacaoIntencao;

    @Column(name = "automacao_processada_em")
    private LocalDateTime automacaoProcessadaEm;

    @Column(name = "automacao_resposta_enviada", nullable = false)
    private boolean automacaoRespostaEnviada = false;

    protected WhatsAppMensagem() {}

    public WhatsAppMensagem(Empresa empresa, Cliente cliente, String providerMessageId,
                            Direcao direcao, String telefone, String pushName, String mensagem,
                            String rawPayload, LocalDateTime recebidoEm) {
        this.empresa = empresa;
        this.cliente = cliente;
        this.providerMessageId = providerMessageId;
        this.direcao = direcao;
        this.telefone = telefone;
        this.pushName = pushName;
        this.mensagem = mensagem;
        this.rawPayload = rawPayload;
        this.recebidoEm = recebidoEm;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Cliente getCliente() { return cliente; }
    public String getProviderMessageId() { return providerMessageId; }
    public Direcao getDirecao() { return direcao; }
    public String getTelefone() { return telefone; }
    public String getPushName() { return pushName; }
    public String getMensagem() { return mensagem; }
    public String getRawPayload() { return rawPayload; }
    public LocalDateTime getRecebidoEm() { return recebidoEm; }
    public LocalDateTime getLidaEm() { return lidaEm; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public WhatsAppIntencao getAutomacaoIntencao() { return automacaoIntencao; }
    public LocalDateTime getAutomacaoProcessadaEm() { return automacaoProcessadaEm; }
    public boolean isAutomacaoRespostaEnviada() { return automacaoRespostaEnviada; }

    public void registrarProcessamentoAutomacao(WhatsAppIntencao intencao, boolean respostaEnviada) {
        this.automacaoIntencao = intencao;
        this.automacaoProcessadaEm = LocalDateTime.now();
        this.automacaoRespostaEnviada = respostaEnviada;
    }

    public void marcarComoLida() {
        if (this.lidaEm == null) this.lidaEm = LocalDateTime.now();
    }
}
