package com.marceloaleixo.melvora.entity.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/** Módulos comercializáveis da plataforma. */
public enum ModuloSistema {
    CLIENTES("Clientes", "Cadastro e relacionamento com clientes.", "👩"),
    PRODUTOS("Produtos", "Catálogo de produtos e Mega Hair.", "🛍️"),
    ESTOQUE("Estoque", "Controle de lotes e movimentações.", "📦"),
    MEGA_HAIR("Mega Hair", "Aplicações, lotes e manutenção de Mega Hair.", "💇"),
    EQUIPE("Equipe", "Usuários e permissões da empresa.", "👥"),
    HISTORICO("Histórico", "Acompanhe a jornada e os registros da cliente.", "📋"),
    AGENDA("Agenda", "Agende atendimentos e organize a rotina da equipe.", "📅"),
    FINANCEIRO("Financeiro", "Receitas, despesas e caixa da empresa.", "💰"),
    SERVICOS("Serviços", "Catálogo de serviços, duração, preços e profissionais habilitados.", "✂️"),
    COMISSOES("Comissões", "Controle de percentuais e comissões da equipe.", "🏆"),
    RELATORIOS("Relatórios", "Indicadores gerenciais e análise do desempenho da empresa.", "📊"),
    COMUNICACAO("Comunicação", "WhatsApp e comunicação com clientes.", "💬");

    private final String nome;
    private final String descricao;
    private final String icone;

    ModuloSistema(String nome, String descricao, String icone) {
        this.nome = nome;
        this.descricao = descricao;
        this.icone = icone;
    }

    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getIcone() { return icone; }

    /** Dependências mínimas para manter o módulo funcional. */
    public List<ModuloSistema> dependencias() {
        return switch (this) {
            case MEGA_HAIR -> List.of(CLIENTES, PRODUTOS, ESTOQUE, HISTORICO);
            case ESTOQUE -> List.of(PRODUTOS, MEGA_HAIR);
            case HISTORICO -> List.of(CLIENTES);
            case AGENDA -> List.of(CLIENTES, EQUIPE);
            case SERVICOS -> List.of(CLIENTES, EQUIPE);
            case FINANCEIRO -> List.of(CLIENTES, HISTORICO);
            case COMISSOES -> List.of(FINANCEIRO, EQUIPE, HISTORICO);
            case RELATORIOS -> List.of(FINANCEIRO, CLIENTES, EQUIPE);
            case COMUNICACAO -> List.of(CLIENTES, AGENDA);
            default -> List.of();
        };
    }

    public static EnumSet<ModuloSistema> comDependencias(Iterable<ModuloSistema> selecionados) {
        EnumSet<ModuloSistema> resultado = EnumSet.noneOf(ModuloSistema.class);
        for (ModuloSistema modulo : selecionados) {
            adicionarComDependencias(resultado, modulo);
        }
        return resultado;
    }

    private static void adicionarComDependencias(EnumSet<ModuloSistema> destino, ModuloSistema modulo) {
        if (!destino.add(modulo)) return;
        for (ModuloSistema dependencia : modulo.dependencias()) {
            adicionarComDependencias(destino, dependencia);
        }
    }

    public static boolean valido(String valor) {
        return Arrays.stream(values()).anyMatch(m -> m.name().equals(valor));
    }
}
