package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comunicacoes_agendadas", uniqueConstraints = {
    @UniqueConstraint(name = "uk_comunicacao_agendamento_tipo", columnNames = {"agendamento_id", "tipo"})
}, indexes = {
    @Index(name = "idx_comunicacao_empresa_status", columnList = "empresa_id,status,data_hora_envio"),
    @Index(name = "idx_comunicacao_agendamento", columnList = "agendamento_id")
})
public class ComunicacaoAgendada {
    public enum Tipo { CONFIRMACAO, LEMBRETE, POS_ATENDIMENTO }
    public enum Status { PENDENTE, ABERTA, ENVIADA, ENTREGUE, LIDA, ERRO, CANCELADA, SEM_TELEFONE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comunicacao_empresa"))
    private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comunicacao_agendamento"))
    private Agendamento agendamento;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Tipo tipo;
    @Column(name = "data_hora_envio", nullable = false)
    private LocalDateTime dataHoraEnvio;
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDENTE;
    @Column(nullable = false, length = 4000)
    private String mensagem;
    @Column(name = "whatsapp_url", length = 5000)
    private String whatsappUrl;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    @Column(name = "provider_message_id", length = 200, unique = true)
    private String providerMessageId;
    @Column(name = "provider_error", length = 2000)
    private String providerError;

    protected ComunicacaoAgendada() {}
    public ComunicacaoAgendada(Empresa empresa, Agendamento agendamento, Tipo tipo, LocalDateTime dataHoraEnvio, String mensagem, String whatsappUrl) {
        this.empresa = empresa; this.agendamento = agendamento; this.tipo = tipo;
        this.dataHoraEnvio = dataHoraEnvio; this.mensagem = mensagem; this.whatsappUrl = whatsappUrl;
    }
    public Long getId(){return id;} public Empresa getEmpresa(){return empresa;} public Agendamento getAgendamento(){return agendamento;}
    public Tipo getTipo(){return tipo;} public LocalDateTime getDataHoraEnvio(){return dataHoraEnvio;} public Status getStatus(){return status;}
    public String getMensagem(){return mensagem;} public String getWhatsappUrl(){return whatsappUrl;}
    public String getProviderMessageId(){return providerMessageId;} public String getProviderError(){return providerError;}
    public void marcarAberta(){this.status=Status.ABERTA; this.processedAt=LocalDateTime.now();}
    public void marcarEnviada(String providerMessageId){this.status=Status.ENVIADA; this.providerMessageId=providerMessageId; this.providerError=null; this.processedAt=LocalDateTime.now();}
    public void marcarEntregue(){this.status=Status.ENTREGUE;}
    public void marcarLida(){this.status=Status.LIDA;}
    public void marcarErro(String erro){this.status=Status.ERRO; this.providerError=erro == null ? "Erro desconhecido" : erro.substring(0, Math.min(2000, erro.length())); this.processedAt=LocalDateTime.now();}
    public void cancelar(){this.status=Status.CANCELADA; this.processedAt=LocalDateTime.now();}
}
