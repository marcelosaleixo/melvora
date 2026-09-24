package com.marceloaleixo.melvora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

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

    @Column(name = "retencao_ativa", nullable = false)
    private boolean retencaoAtiva = false;
    @Min(15) @Max(365)
    @Column(name = "retencao_dias_sem_retorno", nullable = false)
    private int retencaoDiasSemRetorno = 60;
    @Column(name = "retencao_mensagem", nullable = false, length = 2000)
    private String retencaoMensagem = "Olá, {cliente}! 💜 Sentimos sua falta no Melvora. Já faz {dias} dias desde seu último atendimento, em {ultima_data}. Quando quiser, estamos aqui para cuidar de você novamente! 😊";

    @Column(name = "retencao_automatica_ativa", nullable = false)
    private boolean retencaoAutomaticaAtiva = false;
    @Min(1) @Max(365)
    @Column(name = "retencao_cooldown_dias", nullable = false)
    private int retencaoCooldownDias = 30;
    @Column(name = "retencao_horario_inicio", nullable = false)
    private LocalTime retencaoHorarioInicio = LocalTime.of(9, 0);
    @Column(name = "retencao_horario_fim", nullable = false)
    private LocalTime retencaoHorarioFim = LocalTime.of(19, 0);
    @Min(1) @Max(500)
    @Column(name = "retencao_max_envios_dia", nullable = false)
    private int retencaoMaxEnviosDia = 20;

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
    public boolean isRetencaoAtiva() { return retencaoAtiva; }
    public int getRetencaoDiasSemRetorno() { return retencaoDiasSemRetorno; }
    public String getRetencaoMensagem() { return retencaoMensagem; }
    public boolean isRetencaoAutomaticaAtiva() { return retencaoAutomaticaAtiva; }
    public int getRetencaoCooldownDias() { return retencaoCooldownDias; }
    public LocalTime getRetencaoHorarioInicio() { return retencaoHorarioInicio; }
    public LocalTime getRetencaoHorarioFim() { return retencaoHorarioFim; }
    public int getRetencaoMaxEnviosDia() { return retencaoMaxEnviosDia; }

    public void atualizar(boolean confirmacaoAtiva, int confirmacaoMinutosAntes,
                          boolean lembreteAtivo, int lembreteMinutosAntes,
                          boolean posAtendimentoAtivo, int posAtendimentoMinutosDepois,
                          boolean retencaoAtiva, int retencaoDiasSemRetorno, String retencaoMensagem,
                          boolean retencaoAutomaticaAtiva, int retencaoCooldownDias, LocalTime retencaoHorarioInicio,
                          LocalTime retencaoHorarioFim, int retencaoMaxEnviosDia) {
        this.confirmacaoAtiva = confirmacaoAtiva;
        this.confirmacaoMinutosAntes = confirmacaoMinutosAntes;
        this.lembreteAtivo = lembreteAtivo;
        this.lembreteMinutosAntes = lembreteMinutosAntes;
        this.posAtendimentoAtivo = posAtendimentoAtivo;
        this.posAtendimentoMinutosDepois = posAtendimentoMinutosDepois;
        this.retencaoAtiva = retencaoAtiva;
        this.retencaoDiasSemRetorno = retencaoDiasSemRetorno;
        this.retencaoMensagem = retencaoMensagem;
        this.retencaoAutomaticaAtiva = retencaoAutomaticaAtiva;
        this.retencaoCooldownDias = retencaoCooldownDias;
        this.retencaoHorarioInicio = retencaoHorarioInicio;
        this.retencaoHorarioFim = retencaoHorarioFim;
        this.retencaoMaxEnviosDia = retencaoMaxEnviosDia;
    }
}
