package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.ComunicacaoAgendada;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunicacaoAgendadaRepository extends JpaRepository<ComunicacaoAgendada, Long> {
    boolean existsByAgendamentoIdAndTipo(Long agendamentoId, ComunicacaoAgendada.Tipo tipo);
    @EntityGraph(attributePaths = {"agendamento", "agendamento.cliente", "agendamento.profissional", "agendamento.servico"})
    List<ComunicacaoAgendada> findByEmpresaIdOrderByDataHoraEnvioAsc(Long empresaId, Pageable pageable);
    @EntityGraph(attributePaths = {"agendamento", "agendamento.cliente", "agendamento.profissional", "agendamento.servico"})
    Optional<ComunicacaoAgendada> findByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaIdAndStatus(Long empresaId, ComunicacaoAgendada.Status status);
    java.util.Optional<ComunicacaoAgendada> findByProviderMessageId(String providerMessageId);
    @EntityGraph(attributePaths = {"agendamento", "agendamento.cliente", "agendamento.profissional", "agendamento.servico"})
    List<ComunicacaoAgendada> findByStatusAndDataHoraEnvioLessThanEqual(ComunicacaoAgendada.Status status, LocalDateTime limite);
}
