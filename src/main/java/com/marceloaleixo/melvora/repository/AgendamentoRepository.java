package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Agendamento;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("""
        select a from Agendamento a
        join fetch a.cliente c
        join fetch a.profissional p
        left join fetch a.servico s
        where a.empresa.id = :empresaId
          and a.dataHoraInicio < :fim
          and a.dataHoraFim > :inicio
        order by a.dataHoraInicio asc
        """)
    List<Agendamento> listarNoPeriodo(@Param("empresaId") Long empresaId,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    @Query("""
        select case when count(a) > 0 then true else false end
        from Agendamento a
        where a.empresa.id = :empresaId
          and a.profissional.id = :profissionalId
          and a.status in :status
          and a.dataHoraInicio < :fim
          and a.dataHoraFim > :inicio
        """)
    boolean existeConflitoProfissional(@Param("empresaId") Long empresaId,
                                        @Param("profissionalId") Long profissionalId,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fim") LocalDateTime fim,
                                        @Param("status") List<StatusAgendamento> status);

    @Query("""
        select case when count(a) > 0 then true else false end
        from Agendamento a
        where a.empresa.id = :empresaId
          and a.cliente.id = :clienteId
          and a.status in :status
          and a.dataHoraInicio < :fim
          and a.dataHoraFim > :inicio
        """)
    boolean existeConflitoCliente(@Param("empresaId") Long empresaId,
                                  @Param("clienteId") Long clienteId,
                                  @Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim,
                                  @Param("status") List<StatusAgendamento> status);

    @Query("""
        select case when count(a) > 0 then true else false end
        from Agendamento a
        where a.empresa.id = :empresaId
          and a.id <> :id
          and a.profissional.id = :profissionalId
          and a.status in :status
          and a.dataHoraInicio < :fim
          and a.dataHoraFim > :inicio
        """)
    boolean existeConflitoProfissionalExcluindo(@Param("empresaId") Long empresaId,
                                                  @Param("profissionalId") Long profissionalId,
                                                  @Param("inicio") LocalDateTime inicio,
                                                  @Param("fim") LocalDateTime fim,
                                                  @Param("status") List<StatusAgendamento> status,
                                                  @Param("id") Long id);

    @Query("""
        select case when count(a) > 0 then true else false end
        from Agendamento a
        where a.empresa.id = :empresaId
          and a.id <> :id
          and a.cliente.id = :clienteId
          and a.status in :status
          and a.dataHoraInicio < :fim
          and a.dataHoraFim > :inicio
        """)
    boolean existeConflitoClienteExcluindo(@Param("empresaId") Long empresaId,
                                            @Param("clienteId") Long clienteId,
                                            @Param("inicio") LocalDateTime inicio,
                                            @Param("fim") LocalDateTime fim,
                                            @Param("status") List<StatusAgendamento> status,
                                            @Param("id") Long id);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    Optional<Agendamento> findByIdAndEmpresaId(Long id, Long empresaId);

    @Query("""
        select a from Agendamento a
        join fetch a.cliente c
        join fetch a.profissional p
        left join fetch a.servico s
        where a.empresa.id = :empresaId
          and a.cliente.id = :clienteId
          and a.dataHoraInicio >= :agora
          and a.status in :status
        order by a.dataHoraInicio asc
        """)
    List<Agendamento> listarProximosDaCliente(@Param("empresaId") Long empresaId,
                                               @Param("clienteId") Long clienteId,
                                               @Param("agora") LocalDateTime agora,
                                               @Param("status") List<StatusAgendamento> status,
                                               org.springframework.data.domain.Pageable pageable);

    @Query("""
        select a from Agendamento a
        join fetch a.cliente c
        join fetch a.profissional p
        left join fetch a.servico s
        where a.empresa.id = :empresaId
          and a.cliente.id = :clienteId
        order by a.dataHoraInicio desc
        """)
    List<Agendamento> listarRecentesDaCliente(@Param("empresaId") Long empresaId,
                                               @Param("clienteId") Long clienteId,
                                               org.springframework.data.domain.Pageable pageable);

    @Query("""
        select a from Agendamento a
        join fetch a.cliente c
        join fetch a.profissional p
        left join fetch a.servico s
        where a.empresa.id = :empresaId
          and a.dataHoraInicio >= :agora
          and a.dataHoraInicio < :limite
          and a.status in :status
        order by a.dataHoraInicio asc
        """)
    List<Agendamento> listarProximosDaEmpresa(@Param("empresaId") Long empresaId,
                                               @Param("agora") LocalDateTime agora,
                                               @Param("limite") LocalDateTime limite,
                                               @Param("status") List<StatusAgendamento> status,
                                               org.springframework.data.domain.Pageable pageable);

    long countByEmpresaIdAndClienteId(Long empresaId, Long clienteId);
    long countByEmpresaIdAndClienteIdAndStatus(Long empresaId, Long clienteId, StatusAgendamento status);

    long countByEmpresaIdAndStatus(Long empresaId, StatusAgendamento status);
    @Query("""
        select a from Agendamento a
        join fetch a.cliente c
        join fetch a.profissional p
        left join fetch a.servico s
        where a.empresa.id = :empresaId
          and a.status = com.marceloaleixo.melvora.entity.enums.StatusAgendamento.CONCLUIDO
          and a.dataHoraFim >= :inicio and a.dataHoraFim < :fim
        order by a.dataHoraFim asc
        """)
    List<Agendamento> listarRecentesConcluidosDaEmpresa(@Param("empresaId") Long empresaId,
                                                         @Param("inicio") LocalDateTime inicio,
                                                         @Param("fim") LocalDateTime fim);

}
