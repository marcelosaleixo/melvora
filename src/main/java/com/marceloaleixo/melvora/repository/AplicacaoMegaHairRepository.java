package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.AplicacaoMegaHair;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AplicacaoMegaHairRepository extends JpaRepository<AplicacaoMegaHair, Long> {
    @EntityGraph(attributePaths = {"cliente", "profissional", "lotes", "lotes.lote"})
    Optional<AplicacaoMegaHair> findByIdAndEmpresaId(Long id, Long empresaId);
    @EntityGraph(attributePaths = {"cliente", "profissional", "lotes", "lotes.lote"})
    List<AplicacaoMegaHair> findByEmpresaIdAndClienteIdOrderByDataAplicacaoDesc(Long empresaId, Long clienteId);
    boolean existsByAgendamentoIdAndEmpresaId(Long agendamentoId, Long empresaId);
    Optional<AplicacaoMegaHair> findTopByEmpresaIdAndClienteIdOrderByDataAplicacaoDesc(Long empresaId, Long clienteId);
    long countByEmpresaId(Long empresaId);
}
