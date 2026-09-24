package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "campanhas_comunicacao", indexes = {
    @Index(name = "idx_campanha_empresa_status", columnList = "empresa_id,status"),
    @Index(name = "idx_campanha_empresa_criada", columnList = "empresa_id,created_at")
})
public class CampanhaComunicacao {
    public enum Segmento { INATIVAS, SEM_PROXIMO_AGENDAMENTO, SERVICO_REALIZADO }
    public enum Status { RASCUNHO, ENVIANDO, CONCLUIDA, CANCELADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_campanha_empresa"))
    private Empresa empresa;
    @NotBlank @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private Segmento segmento;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status = Status.RASCUNHO;
    @Min(1) @Max(365)
    @Column(name = "dias_sem_retorno")
    private Integer diasSemRetorno;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", foreignKey = @ForeignKey(name = "fk_campanha_servico"))
    private Servico servico;
    @NotBlank @Size(max = 4000)
    @Column(nullable = false, length = 4000)
    private String mensagem;
    @Min(1) @Max(365)
    @Column(name = "cooldown_dias", nullable = false)
    private Integer cooldownDias = 30;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "started_at") private LocalDateTime startedAt;
    @Column(name = "finished_at") private LocalDateTime finishedAt;

    protected CampanhaComunicacao() {}
    public CampanhaComunicacao(Empresa empresa, String nome, Segmento segmento, Integer diasSemRetorno,
                               Servico servico, String mensagem, Integer cooldownDias) {
        this.empresa = empresa; this.nome = nome; this.segmento = segmento;
        this.diasSemRetorno = diasSemRetorno; this.servico = servico;
        this.mensagem = mensagem; this.cooldownDias = cooldownDias == null ? 30 : cooldownDias;
    }
    public Long getId(){return id;} public Empresa getEmpresa(){return empresa;} public String getNome(){return nome;}
    public Segmento getSegmento(){return segmento;} public Status getStatus(){return status;}
    public Integer getDiasSemRetorno(){return diasSemRetorno;} public Servico getServico(){return servico;}
    public String getMensagem(){return mensagem;} public Integer getCooldownDias(){return cooldownDias;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getStartedAt(){return startedAt;} public LocalDateTime getFinishedAt(){return finishedAt;}
    public void iniciar(){this.status=Status.ENVIANDO; this.startedAt=LocalDateTime.now();}
    public void concluir(){this.status=Status.CONCLUIDA; this.finishedAt=LocalDateTime.now();}
    public void cancelar(){this.status=Status.CANCELADA; this.finishedAt=LocalDateTime.now();}
}
