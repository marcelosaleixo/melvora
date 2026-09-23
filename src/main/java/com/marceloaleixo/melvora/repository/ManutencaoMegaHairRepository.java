package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.ManutencaoMegaHair;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManutencaoMegaHairRepository extends JpaRepository<ManutencaoMegaHair, Long> {
    List<ManutencaoMegaHair> findByEmpresaIdAndClienteIdOrderByDataManutencaoDesc(Long empresaId, Long clienteId);
    boolean existsByAgendamentoIdAndEmpresaId(Long agendamentoId, Long empresaId);
    long countByEmpresaId(Long empresaId);
}
