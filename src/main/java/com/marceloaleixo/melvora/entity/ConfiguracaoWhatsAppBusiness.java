package com.marceloaleixo.melvora.entity;

import com.marceloaleixo.melvora.entity.enums.WhatsAppIntegrationMode;
import jakarta.persistence.*;

@Entity
@Table(name = "configuracoes_whatsapp_business")
public class ConfiguracaoWhatsAppBusiness {
    @Id
    private Long empresaId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "empresa_id", foreignKey = @ForeignKey(name = "fk_whatsapp_business_empresa"))
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_integracao", nullable = false, length = 20)
    private WhatsAppIntegrationMode modoIntegracao = WhatsAppIntegrationMode.META_CLOUD;

    @Column(name = "phone_number_id", length = 100)
    private String phoneNumberId;

    @Column(name = "access_token_encrypted", length = 4096)
    private String accessTokenEncrypted;

    @Column(name = "api_version", length = 30)
    private String apiVersion = "v23.0";

    @Column(name = "api_base_url", length = 500)
    private String apiBaseUrl = "https://graph.facebook.com";

    @Column(name = "n8n_base_url", length = 500)
    private String n8nBaseUrl;

    @Column(name = "n8n_webhook_path", length = 500)
    private String n8nWebhookPath;

    @Column(name = "n8n_integration_key", length = 100, unique = true)
    private String n8nIntegrationKey;

    @Column(name = "n8n_token_encrypted", length = 4096)
    private String n8nTokenEncrypted;

    @Column(name = "evolution_base_url", length = 500)
    private String evolutionBaseUrl;

    @Column(name = "evolution_api_key_encrypted", length = 4096)
    private String evolutionApiKeyEncrypted;

    @Column(name = "evolution_instance", length = 150)
    private String evolutionInstance;

    @Column(name = "wuzapi_base_url", length = 500)
    private String wuzapiBaseUrl;

    @Column(name = "wuzapi_token_encrypted", length = 4096)
    private String wuzapiTokenEncrypted;

    @Column(name = "wuzapi_integration_key", length = 100, unique = true)
    private String wuzapiIntegrationKey;

    @Column(name = "wuzapi_hmac_secret_encrypted", length = 4096)
    private String wuzapiHmacSecretEncrypted;

    @Column(name = "wuzapi_phone_jid", length = 200)
    private String wuzapiPhoneJid;

    @Column(nullable = false)
    private boolean ativa = false;

    protected ConfiguracaoWhatsAppBusiness() {}

    public ConfiguracaoWhatsAppBusiness(Empresa empresa) {
        this.empresa = empresa;
        this.empresaId = empresa.getId();
    }

    public Long getEmpresaId() { return empresaId; }
    public Empresa getEmpresa() { return empresa; }
    public WhatsAppIntegrationMode getModoIntegracao() { return modoIntegracao; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public String getAccessTokenEncrypted() { return accessTokenEncrypted; }
    public String getApiVersion() { return apiVersion; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public String getN8nBaseUrl() { return n8nBaseUrl; }
    public String getN8nWebhookPath() { return n8nWebhookPath; }
    public String getN8nIntegrationKey() { return n8nIntegrationKey; }
    public String getN8nTokenEncrypted() { return n8nTokenEncrypted; }
    public String getEvolutionBaseUrl() { return evolutionBaseUrl; }
    public String getEvolutionApiKeyEncrypted() { return evolutionApiKeyEncrypted; }
    public String getEvolutionInstance() { return evolutionInstance; }
    public String getWuzapiBaseUrl() { return wuzapiBaseUrl; }
    public String getWuzapiTokenEncrypted() { return wuzapiTokenEncrypted; }
    public String getWuzapiIntegrationKey() { return wuzapiIntegrationKey; }
    public String getWuzapiHmacSecretEncrypted() { return wuzapiHmacSecretEncrypted; }
    public String getWuzapiPhoneJid() { return wuzapiPhoneJid; }
    public boolean isAtiva() { return ativa; }

    public void atualizarMeta(String phoneNumberId, String accessTokenEncrypted, String apiVersion,
                              String apiBaseUrl, boolean ativa) {
        this.modoIntegracao = WhatsAppIntegrationMode.META_CLOUD;
        this.phoneNumberId = phoneNumberId;
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.apiVersion = apiVersion;
        this.apiBaseUrl = apiBaseUrl;
        this.ativa = ativa;
    }

    public void atualizarN8n(String n8nBaseUrl, String n8nWebhookPath, String n8nIntegrationKey,
                             String n8nTokenEncrypted, boolean ativa) {
        this.modoIntegracao = WhatsAppIntegrationMode.N8N;
        this.n8nBaseUrl = n8nBaseUrl;
        this.n8nWebhookPath = n8nWebhookPath;
        this.n8nIntegrationKey = n8nIntegrationKey;
        this.n8nTokenEncrypted = n8nTokenEncrypted;
        this.ativa = ativa;
    }

    public void atualizarEvolution(String baseUrl, String apiKeyEncrypted, String instance, boolean ativa) {
        this.modoIntegracao = WhatsAppIntegrationMode.EVOLUTION_API;
        this.evolutionBaseUrl = baseUrl;
        this.evolutionApiKeyEncrypted = apiKeyEncrypted;
        this.evolutionInstance = instance;
        this.ativa = ativa;
    }

    public void atualizarWuzapi(String baseUrl, String tokenEncrypted, String integrationKey, String hmacSecretEncrypted, boolean ativa) {
        this.modoIntegracao = WhatsAppIntegrationMode.WUZAPI;
        this.wuzapiBaseUrl = baseUrl;
        this.wuzapiTokenEncrypted = tokenEncrypted;
        this.wuzapiIntegrationKey = integrationKey;
        this.wuzapiHmacSecretEncrypted = hmacSecretEncrypted;
        this.ativa = ativa;
    }

    public void definirWuzapiPhoneJid(String phoneJid) {
        this.wuzapiPhoneJid = phoneJid;
    }
}
