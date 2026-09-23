package com.marceloaleixo.melvora.entity;

import com.marceloaleixo.melvora.entity.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="lancamentos_financeiros", indexes={
 @Index(name="idx_fin_empresa_data", columnList="empresa_id,data_movimento"),
 @Index(name="idx_fin_empresa_status", columnList="empresa_id,status"),
 @Index(name="idx_fin_empresa_cliente", columnList="empresa_id,cliente_id")
})
public class LancamentoFinanceiro {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="empresa_id",nullable=false,foreignKey=@ForeignKey(name="fk_fin_empresa")) private Empresa empresa;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="atendimento_id",foreignKey=@ForeignKey(name="fk_fin_atendimento")) private Atendimento atendimento;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="cliente_id",foreignKey=@ForeignKey(name="fk_fin_cliente")) private Cliente cliente;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TipoLancamentoFinanceiro tipo;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private StatusLancamentoFinanceiro status;
 @Enumerated(EnumType.STRING) @Column(name="forma_pagamento",length=30) private FormaPagamento formaPagamento;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal valor;
 @Column(nullable=false,length=200) private String descricao;
 @Column(name="data_movimento",nullable=false) private LocalDate dataMovimento;
 @Column(name="data_vencimento") private LocalDate dataVencimento;
 @Column(name="pago_em") private LocalDateTime pagoEm;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 protected LancamentoFinanceiro() {}
 public LancamentoFinanceiro(Empresa empresa, Atendimento atendimento, Cliente cliente, BigDecimal valor, String descricao, LocalDate dataMovimento) {
  this.empresa=empresa; this.atendimento=atendimento; this.cliente=cliente; this.tipo=TipoLancamentoFinanceiro.RECEITA;
  this.status=StatusLancamentoFinanceiro.PENDENTE; this.valor=valor; this.descricao=descricao; this.dataMovimento=dataMovimento;
  this.dataVencimento=dataMovimento; this.createdAt=LocalDateTime.now();
 }
 public static LancamentoFinanceiro despesa(Empresa empresa, BigDecimal valor, String descricao, LocalDate movimento, LocalDate vencimento, FormaPagamento forma) {
  LancamentoFinanceiro l=new LancamentoFinanceiro(); l.empresa=empresa; l.tipo=TipoLancamentoFinanceiro.DESPESA; l.status=StatusLancamentoFinanceiro.PENDENTE;
  l.valor=valor; l.descricao=descricao; l.dataMovimento=movimento; l.dataVencimento=vencimento; l.formaPagamento=forma; l.createdAt=LocalDateTime.now(); return l;
 }
 public void pagar(FormaPagamento forma){ if(status==StatusLancamentoFinanceiro.CANCELADO) throw new IllegalStateException("Lançamento cancelado não pode ser pago."); status=StatusLancamentoFinanceiro.PAGO; formaPagamento=forma; pagoEm=LocalDateTime.now(); }
 public void cancelar(){ if(status==StatusLancamentoFinanceiro.PAGO) throw new IllegalStateException("Lançamento pago não pode ser cancelado."); status=StatusLancamentoFinanceiro.CANCELADO; }
 public Long getId(){return id;} public Empresa getEmpresa(){return empresa;} public Atendimento getAtendimento(){return atendimento;} public Cliente getCliente(){return cliente;}
 public TipoLancamentoFinanceiro getTipo(){return tipo;} public StatusLancamentoFinanceiro getStatus(){return status;} public FormaPagamento getFormaPagamento(){return formaPagamento;}
 public BigDecimal getValor(){return valor;} public String getDescricao(){return descricao;} public LocalDate getDataMovimento(){return dataMovimento;} public LocalDate getDataVencimento(){return dataVencimento;} public LocalDateTime getPagoEm(){return pagoEm;} public LocalDateTime getCreatedAt(){return createdAt;}
}
