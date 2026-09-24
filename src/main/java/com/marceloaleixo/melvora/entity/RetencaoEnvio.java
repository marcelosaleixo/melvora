package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "retencao_envios", indexes = {
    @Index(name = "idx_ret_envio_empresa_data", columnList = "empresa_id,data_hora_envio"),
    @Index(name = "idx_ret_envio_empresa_cliente", columnList = "empresa_id,cliente_id,data_hora_envio")
})
public class RetencaoEnvio {
    public enum Status { ENVIADA, ERRO, BLOQUEADA }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="empresa_id", nullable=false, foreignKey=@ForeignKey(name="fk_ret_envio_empresa")) private Empresa empresa;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="cliente_id", nullable=false, foreignKey=@ForeignKey(name="fk_ret_envio_cliente")) private Cliente cliente;
    @Column(nullable=false,length=30) private String telefone;
    @Column(name="data_hora_envio",nullable=false) private LocalDateTime dataHoraEnvio=LocalDateTime.now();
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
    @Column(nullable=false,length=4000) private String mensagem;
    @Column(name="provider_message_id",length=200) private String providerMessageId;
    @Column(name="erro",length=2000) private String erro;
    protected RetencaoEnvio() {}
    public RetencaoEnvio(Empresa empresa, Cliente cliente, String telefone, String mensagem, Status status){this.empresa=empresa;this.cliente=cliente;this.telefone=telefone;this.mensagem=mensagem;this.status=status;}
    public void marcarEnviada(String id){this.status=Status.ENVIADA;this.providerMessageId=id;this.erro=null;this.dataHoraEnvio=LocalDateTime.now();}
    public void marcarErro(String erro){this.status=Status.ERRO;this.erro=erro==null?"Erro desconhecido":erro.substring(0,Math.min(2000,erro.length()));this.dataHoraEnvio=LocalDateTime.now();}
    public Long getId(){return id;} public Empresa getEmpresa(){return empresa;} public Cliente getCliente(){return cliente;} public String getTelefone(){return telefone;} public LocalDateTime getDataHoraEnvio(){return dataHoraEnvio;} public Status getStatus(){return status;} public String getMensagem(){return mensagem;} public String getProviderMessageId(){return providerMessageId;} public String getErro(){return erro;}
}
