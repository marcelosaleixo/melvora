package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_reservas_pendentes", indexes = {
        @Index(name = "idx_whatsapp_reserva_empresa_telefone_status", columnList = "empresa_id,telefone,status"),
        @Index(name = "idx_whatsapp_reserva_expira", columnList = "status,expira_em")
})
public class WhatsAppReservaPendente {
    public enum Status { AGUARDANDO_HORARIO, AGUARDANDO_CONFIRMACAO, CONFIRMADA, EXPIRADA, CANCELADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_whatsapp_reserva_empresa"))
    private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_whatsapp_reserva_cliente"))
    private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false, foreignKey = @ForeignKey(name = "fk_whatsapp_reserva_servico"))
    private Servico servico;
    @Column(nullable = false, length = 30) private String telefone;
    @Column(name = "data_desejada", nullable = false) private LocalDate dataDesejada;
    @Column(name = "opcoes_json", nullable = false, columnDefinition = "TEXT") private String opcoesJson;
    @Column(name = "profissional_id") private Long profissionalId;
    @Column(name = "inicio_selecionado") private LocalDateTime inicioSelecionado;
    @Column(name = "fim_selecionado") private LocalDateTime fimSelecionado;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "expira_em", nullable = false) private LocalDateTime expiraEm;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected WhatsAppReservaPendente() {}
    public WhatsAppReservaPendente(Empresa empresa, Cliente cliente, Servico servico, String telefone,
                                   LocalDate dataDesejada, String opcoesJson, LocalDateTime expiraEm) {
        this.empresa=empresa; this.cliente=cliente; this.servico=servico; this.telefone=telefone;
        this.dataDesejada=dataDesejada; this.opcoesJson=opcoesJson; this.status=Status.AGUARDANDO_HORARIO;
        this.expiraEm=expiraEm; this.createdAt=LocalDateTime.now(); this.updatedAt=this.createdAt;
    }
    public Long getId(){return id;} public Empresa getEmpresa(){return empresa;} public Cliente getCliente(){return cliente;}
    public Servico getServico(){return servico;} public String getTelefone(){return telefone;} public LocalDate getDataDesejada(){return dataDesejada;}
    public String getOpcoesJson(){return opcoesJson;} public Long getProfissionalId(){return profissionalId;} public LocalDateTime getInicioSelecionado(){return inicioSelecionado;}
    public LocalDateTime getFimSelecionado(){return fimSelecionado;} public Status getStatus(){return status;} public LocalDateTime getExpiraEm(){return expiraEm;}
    public boolean expirada(){return LocalDateTime.now().isAfter(expiraEm);}
    public void selecionar(Long profissionalId, LocalDateTime inicio, LocalDateTime fim){this.profissionalId=profissionalId;this.inicioSelecionado=inicio;this.fimSelecionado=fim;this.status=Status.AGUARDANDO_CONFIRMACAO;this.updatedAt=LocalDateTime.now();}
    public void confirmar(){this.status=Status.CONFIRMADA;this.updatedAt=LocalDateTime.now();}
    public void expirar(){this.status=Status.EXPIRADA;this.updatedAt=LocalDateTime.now();}
    public void cancelar(){this.status=Status.CANCELADA;this.updatedAt=LocalDateTime.now();}
}
