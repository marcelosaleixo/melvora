package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Atendimento;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import com.marceloaleixo.melvora.dto.RelatorioData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {
    @EntityGraph(attributePaths = {"cliente", "profissional", "agendamento"})
    Page<Atendimento> findByEmpresaId(Long empresaId, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "profissional", "agendamento"})
    Optional<Atendimento> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByAgendamentoIdAndEmpresaId(Long agendamentoId, Long empresaId);
    Optional<Atendimento> findByAgendamentoIdAndEmpresaId(Long agendamentoId, Long empresaId);

    @Query("select count(a) from Atendimento a where a.empresa.id=:empresa and a.dataHoraInicio >= :inicio and a.dataHoraInicio < :fim")
    long countPeriodo(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("select count(distinct a.cliente.id) from Atendimento a where a.empresa.id=:empresa and a.dataHoraInicio >= :inicio and a.dataHoraInicio < :fim")
    long countClientesPeriodo(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("select new com.marceloaleixo.melvora.dto.RelatorioData$Item(coalesce(a.servicoNome, a.tipo), count(a), coalesce(sum(a.valorCobrado),0)) from Atendimento a where a.empresa.id=:empresa and a.dataHoraInicio >= :inicio and a.dataHoraInicio < :fim group by coalesce(a.servicoNome, a.tipo) order by count(a) desc")
    List<RelatorioData.Item> servicosMaisRealizados(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, org.springframework.data.domain.Pageable pageable);

    @Query("select new com.marceloaleixo.melvora.dto.RelatorioData$Item(a.profissional.nome, count(a), coalesce(sum(a.valorCobrado),0)) from Atendimento a where a.empresa.id=:empresa and a.dataHoraInicio >= :inicio and a.dataHoraInicio < :fim group by a.profissional.id, a.profissional.nome order by sum(a.valorCobrado) desc")
    List<RelatorioData.Item> profissionaisPorFaturamento(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, org.springframework.data.domain.Pageable pageable);
    @Query("select a.cliente.id, a.cliente.nome, a.cliente.telefone, max(a.dataHoraInicio), count(a) from Atendimento a where a.empresa.id = :empresa and a.cliente.ativo = true group by a.cliente.id, a.cliente.nome, a.cliente.telefone having max(a.dataHoraInicio) < :limite order by max(a.dataHoraInicio) asc, a.cliente.nome asc")
    List<Object[]> listarOportunidadesRetencao(@Param("empresa") Long empresa, @Param("limite") LocalDateTime limite, org.springframework.data.domain.Pageable pageable);

    @Query("select distinct a.cliente.id from Atendimento a where a.empresa.id=:empresa and a.cliente.ativo=true and lower(a.servicoNome)=lower(:servico) order by a.cliente.id")
    List<Long> clientesQueRealizaramServico(@Param("empresa") Long empresa, @Param("servico") String servico);

}


