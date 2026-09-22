package com.marceloaleixo.melvora.dto;

import java.util.List;

public record DashboardData(
        String nomeUsuario,
        String emailUsuario,
        String role,
        String empresaNome,
        boolean plataforma,
        long empresas,
        long usuarios,
        long clientes,
        long produtos,
        long lotesMegaHair,
        long aplicacoes,
        long manutencoes,
        List<Modulo> modulos) {

    public record Modulo(String nome, String descricao, String icone, String href) {}
}
