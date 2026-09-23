package com.marceloaleixo.melvora.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ServicoRequests {
    private ServicoRequests() {}

    public static class WebForm {
        @NotBlank(message = "Informe o nome do serviço.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        private String nome;

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        private String descricao;

        @NotBlank(message = "Informe a categoria.")
        @Size(max = 80, message = "A categoria deve ter no máximo 80 caracteres.")
        private String categoria;

        @NotNull(message = "Informe a duração.")
        @Min(value = 15, message = "A duração mínima é de 15 minutos.")
        @Max(value = 720, message = "A duração máxima é de 12 horas.")
        private Integer duracaoMinutos;

        @NotNull(message = "Informe o preço.")
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "Preço inválido.")
        private BigDecimal preco;

        private List<Long> profissionalIds = new ArrayList<>();

        public WebForm() {}

        public WebForm(com.marceloaleixo.melvora.entity.Servico servico) {
            this.nome = servico.getNome();
            this.descricao = servico.getDescricao();
            this.categoria = servico.getCategoria();
            this.duracaoMinutos = servico.getDuracaoMinutos();
            this.preco = servico.getPreco();
            this.profissionalIds = servico.getProfissionais().stream().map(p -> p.getId()).toList();
        }

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public Integer getDuracaoMinutos() { return duracaoMinutos; }
        public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
        public BigDecimal getPreco() { return preco; }
        public void setPreco(BigDecimal preco) { this.preco = preco; }
        public List<Long> getProfissionalIds() { return profissionalIds; }
        public void setProfissionalIds(List<Long> profissionalIds) { this.profissionalIds = profissionalIds == null ? new ArrayList<>() : profissionalIds; }
    }
}
