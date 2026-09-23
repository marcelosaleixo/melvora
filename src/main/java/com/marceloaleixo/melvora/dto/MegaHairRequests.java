package com.marceloaleixo.melvora.dto;

import com.marceloaleixo.melvora.entity.AplicacaoLote;
import com.marceloaleixo.melvora.entity.AplicacaoMegaHair;
import com.marceloaleixo.melvora.entity.ManutencaoMegaHair;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class MegaHairRequests {
    private MegaHairRequests() {}

    public record AplicarRequest(
            @NotNull Long clienteId,
            @NotNull Long profissionalId,
            Long agendamentoId,
            @NotNull @PastOrPresent LocalDate dataAplicacao,
            @Size(max = 2000) String observacoes,
            @NotEmpty @Size(max = 20) List<@Valid ItemLoteRequest> lotes) {}

    public record ItemLoteRequest(@NotNull Long loteId, @NotNull @Min(1) @Max(10000) Integer quantidade) {}

    public record ManutencaoRequest(
            Long agendamentoId,
            @NotNull @PastOrPresent LocalDate dataManutencao,
            @NotBlank @Pattern(regexp = "REAPLICACAO|REMOCAO|MANUTENCAO") String tipo,
            @Size(max = 2000) String observacoes) {}

    public record AplicacaoResponse(Long id, Long clienteId, String cliente, Long profissionalId, String profissional,
                                    Long agendamentoId, LocalDate dataAplicacao, String observacoes, List<ItemResponse> lotes) {
        public static AplicacaoResponse from(AplicacaoMegaHair a) {
            return new AplicacaoResponse(a.getId(), a.getCliente().getId(), a.getCliente().getNome(), a.getProfissional().getId(),
                    a.getProfissional().getNome(), a.getAgendamento() == null ? null : a.getAgendamento().getId(),
                    a.getDataAplicacao(), a.getObservacoes(), a.getLotes().stream().map(ItemResponse::from).toList());
        }
    }

    public record ItemResponse(Long loteId, String loteCodigo, int quantidade, java.math.BigDecimal pesoTotalGramas,
                               int comprimentoCm, String tipoFio, String cor, String metodo) {
        public static ItemResponse from(AplicacaoLote i) {
            return new ItemResponse(i.getLote().getId(), i.getLote().getCodigo(), i.getQuantidade(), i.getPesoTotalGramas(), i.getComprimentoCm(), i.getTipoFio(), i.getCor(), i.getMetodo());
        }
    }

    public record ManutencaoResponse(Long id, Long aplicacaoId, Long clienteId, String cliente, Long profissionalId,
                                     String profissional, Long agendamentoId, LocalDate dataManutencao, String tipo, String observacoes) {
        public static ManutencaoResponse from(ManutencaoMegaHair m) {
            return new ManutencaoResponse(m.getId(), m.getAplicacao().getId(), m.getCliente().getId(), m.getCliente().getNome(),
                    m.getProfissional().getId(), m.getProfissional().getNome(), m.getAgendamento() == null ? null : m.getAgendamento().getId(),
                    m.getDataManutencao(), m.getTipo(), m.getObservacoes());
        }
    }

    public record HistoricoResponse(
            List<AplicacaoResponse> aplicacoes,
            List<ManutencaoResponse> manutencoes,
            List<TimelineItem> timeline,
            ResumoHistorico resumo) {}

    public record TimelineItem(
            LocalDate data,
            String tipo,
            String titulo,
            String descricao,
            String profissional,
            Long referenciaId) {}

    public record ResumoHistorico(
            String statusAtual,
            String statusDescricao,
            LocalDate ultimaAplicacao,
            LocalDate ultimaManutencao,
            long totalAplicacoes,
            long totalManutencoes) {}
}
