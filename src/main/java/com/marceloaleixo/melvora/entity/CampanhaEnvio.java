package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campanhas_envios", uniqueConstraints = {
    @UniqueConstraint(name = "uk_campanha_envio_cliente", columnNames = {"campanha_id", "cliente_id"})
}, indexes = {
    @Index(name = "idx_campanha_envio_empresa_status", columnList = "empresa_id,status"),
    @Index(name = "idx_campanha_envio_cliente_data", columnList = "empresa_id,cliente_id,data_hora_envio")
})
public class CampanhaEnvio {
    public enum Status { PENDENTE, ENVIADA, ERRO, BLOQUEADA }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="empresa_id", nullable=false, foreignKey=@ForeignKey(name="fk_camp_envio_empresa")) private Empresa empresa;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="campanha_id", nullable=false, foreignKey=@ForeignKey(name="fk_camp_envio_campanha")) private CampanhaComunicacao campanha;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="cliente_id", nullable=false, foreignKey=@ForeignKey(name="fk_camp_envio_cliente")) private Cliente cliente;
    @Column(nullable=false,length=30) private String telefone;
    @Column(nullable=false,length=4000) private String mensagem;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
    @Column(name="data_hora_envio",nullable=false) private LocalDateTime dataHoraEnvio=LocalDateTime.now();
    @Column(name="provider_message_id",length=200) private String providerMessageId;
    @Column(name="erro",length=2000) private String erro;
    protected CampanhaEnvio() {}
    public CampanhaEnvio(Empresa empresa, CampanhaComunicacao campanha, Cliente cliente, String telefone, String mensagem, Status status){this.empresa=empresa;this.campanha=campanha;this.cliente=cliente;this.telefone=telefone;this.mensagem=mensagem;this.status=status;}
    public Long getId(){return id;} public Empresa getEmpresa(){return empresa;} public CampanhaComunicacao getCampanha(){return campanha;} public Cliente getCliente(){return cliente;} public String getTelefone(){return telefone;} public String getMensagem(){return mensagem;} public Status getStatus(){return status;} public LocalDateTime getDataHoraEnvio(){return dataHoraEnvio;} public String getProviderMessageId(){return providerMessageId;} public String getErro(){return erro;}
    public void marcarEnviada(String id){status=Status.ENVIADA;providerMessageId=id;erro=null;dataHoraEnvio=LocalDateTime.now();}
    public void marcarErro(String erro){status=Status.ERRO;this.erro=erro==null?"Erro desconhecido":erro.substring(0,Math.min(2000,erro.length()));dataHoraEnvio=LocalDateTime.now();}
    public void bloquear(String motivo){status=Status.BLOQUEADA;erro=motivo;dataHoraEnvio=LocalDateTime.now();}
}
