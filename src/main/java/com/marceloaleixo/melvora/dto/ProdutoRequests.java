package com.marceloaleixo.melvora.dto;

import com.marceloaleixo.melvora.entity.enums.MetodoMegaHair;
import com.marceloaleixo.melvora.entity.enums.TipoFio;
import com.marceloaleixo.melvora.entity.enums.TipoProduto;
import com.marceloaleixo.melvora.entity.Produto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class ProdutoRequests {
    private ProdutoRequests() {}

    public record MegaHair(
        @NotNull @Min(1) @Max(300) Integer comprimentoCm,
        @NotNull @DecimalMin("0.01") @Digits(integer=8, fraction=2) BigDecimal pesoGramas,
        @NotNull TipoFio tipoFio,
        @NotBlank @Size(max=80) String cor,
        @NotNull MetodoMegaHair metodo,
        @Size(max=100) String origem) {}

    public record Criar(
        @NotBlank @Size(max=150) String nome,
        @NotNull TipoProduto tipo,
        @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal precoVenda,
        @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal precoCusto,
        @Valid MegaHair megaHair) {}

    public record Editar(
        @NotBlank @Size(max=150) String nome,
        @NotNull TipoProduto tipo,
        @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal precoVenda,
        @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal precoCusto,
        @Valid MegaHair megaHair) {}

    /** Formulário web plano para evitar binding de entidades e manter validação explícita. */
    public static class WebForm {
        @NotBlank(message = "Informe o nome do produto.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        private String nome;

        @NotNull(message = "Selecione o tipo do produto.")
        private TipoProduto tipo;

        @NotNull(message = "Informe o preço de custo.")
        @DecimalMin(value = "0.00", message = "O preço de custo não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "Preço de custo inválido.")
        private BigDecimal precoCusto;

        @NotNull(message = "Informe o preço de venda.")
        @DecimalMin(value = "0.00", message = "O preço de venda não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "Preço de venda inválido.")
        private BigDecimal precoVenda;

        @Min(value = 1, message = "O comprimento deve ser de 1 a 300 cm.")
        @Max(value = 300, message = "O comprimento deve ser de 1 a 300 cm.")
        private Integer comprimentoCm;

        @DecimalMin(value = "0.01", message = "O peso deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Peso inválido.")
        private BigDecimal pesoGramas;

        private TipoFio tipoFio;

        @Size(max = 80, message = "A cor deve ter no máximo 80 caracteres.")
        private String cor;

        private MetodoMegaHair metodo;

        @Size(max = 100, message = "A origem deve ter no máximo 100 caracteres.")
        private String origem;

        public WebForm() {}

        public WebForm(Produto produto, ProdutoMegaHairData megaHair) {
            this.nome = produto.getNome();
            this.tipo = produto.getTipo();
            this.precoCusto = produto.getPrecoCusto();
            this.precoVenda = produto.getPrecoVenda();
            if (megaHair != null) {
                this.comprimentoCm = megaHair.comprimentoCm();
                this.pesoGramas = megaHair.pesoGramas();
                this.tipoFio = megaHair.tipoFio();
                this.cor = megaHair.cor();
                this.metodo = megaHair.metodo();
                this.origem = megaHair.origem();
            }
        }

        public Criar toCriar() {
            return new Criar(nome, tipo, precoVenda, precoCusto, megaHairOrNull());
        }

        public Editar toEditar() {
            return new Editar(nome, tipo, precoVenda, precoCusto, megaHairOrNull());
        }

        private MegaHair megaHairOrNull() {
            if (tipo != TipoProduto.MEGA_HAIR) return null;
            return new MegaHair(comprimentoCm, pesoGramas, tipoFio, cor, metodo, origem);
        }

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public TipoProduto getTipo() { return tipo; }
        public void setTipo(TipoProduto tipo) { this.tipo = tipo; }
        public BigDecimal getPrecoCusto() { return precoCusto; }
        public void setPrecoCusto(BigDecimal precoCusto) { this.precoCusto = precoCusto; }
        public BigDecimal getPrecoVenda() { return precoVenda; }
        public void setPrecoVenda(BigDecimal precoVenda) { this.precoVenda = precoVenda; }
        public Integer getComprimentoCm() { return comprimentoCm; }
        public void setComprimentoCm(Integer comprimentoCm) { this.comprimentoCm = comprimentoCm; }
        public BigDecimal getPesoGramas() { return pesoGramas; }
        public void setPesoGramas(BigDecimal pesoGramas) { this.pesoGramas = pesoGramas; }
        public TipoFio getTipoFio() { return tipoFio; }
        public void setTipoFio(TipoFio tipoFio) { this.tipoFio = tipoFio; }
        public String getCor() { return cor; }
        public void setCor(String cor) { this.cor = cor; }
        public MetodoMegaHair getMetodo() { return metodo; }
        public void setMetodo(MetodoMegaHair metodo) { this.metodo = metodo; }
        public String getOrigem() { return origem; }
        public void setOrigem(String origem) { this.origem = origem; }
    }

    public record ProdutoMegaHairData(
        Integer comprimentoCm, BigDecimal pesoGramas, TipoFio tipoFio,
        String cor, MetodoMegaHair metodo, String origem) {}
}
