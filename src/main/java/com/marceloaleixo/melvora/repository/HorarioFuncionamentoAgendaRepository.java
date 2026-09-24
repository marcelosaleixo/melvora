package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.HorarioFuncionamentoAgenda;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioFuncionamentoAgendaRepository extends JpaRepository<HorarioFuncionamentoAgenda, Long> {
    List<HorarioFuncionamentoAgenda> findByEmpresaIdOrderByDiaSemanaAsc(Long empresaId);
    Optional<HorarioFuncionamentoAgenda> findByEmpresaIdAndDiaSemana(Long empresaId, int diaSemana);
}
