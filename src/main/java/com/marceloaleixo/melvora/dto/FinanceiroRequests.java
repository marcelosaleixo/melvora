package com.marceloaleixo.melvora.dto;
import com.marceloaleixo.melvora.entity.enums.FormaPagamento;
import com.marceloaleixo.melvora.entity.enums.TipoLancamentoFinanceiro;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate;
public final class FinanceiroRequests {
 private FinanceiroRequests(){}
 public record LancamentoForm(@NotNull TipoLancamentoFinanceiro tipo,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal valor,@NotBlank @Size(max=200) String descricao,@NotNull LocalDate dataMovimento,LocalDate dataVencimento,FormaPagamento formaPagamento) {}
}
