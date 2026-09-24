package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.AvaliacaoAtendimento;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvaliacaoAtendimentoRepository extends JpaRepository<AvaliacaoAtendimento, Long> {
    Optional<AvaliacaoAtendimento> findByPublicToken(String publicToken);
    Optional<AvaliacaoAtendimento> findByAtendimentoIdAndEmpresaId(Long atendimentoId, Long empresaId);

    @EntityGraph(attributePaths = {"cliente", "profissional", "atendimento"})
    Page<AvaliacaoAtendimento> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId, Pageable pageable);

    @Query("select coalesce(avg(a.nota),0) from AvaliacaoAtendimento a where a.empresa.id = :empresa and a.nota is not null")
    Double mediaNota(@Param("empresa") Long empresa);

    @Query("select count(a) from AvaliacaoAtendimento a where a.empresa.id = :empresa and a.nota is not null")
    long countRespondidas(@Param("empresa") Long empresa);

    @Query("select coalesce(avg(a.nota),0) from AvaliacaoAtendimento a where a.empresa.id = :empresa and a.nota is not null and a.respondidaAt >= :inicio and a.respondidaAt < :fimExclusivo")
    Double mediaNotaPeriodo(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fimExclusivo") LocalDateTime fimExclusivo);

    @Query("select count(a) from AvaliacaoAtendimento a where a.empresa.id = :empresa and a.nota is not null and a.respondidaAt >= :inicio and a.respondidaAt < :fimExclusivo")
    long countRespondidasPeriodo(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fimExclusivo") LocalDateTime fimExclusivo);

    @Query("select count(a) from AvaliacaoAtendimento a where a.empresa.id = :empresa and a.nota is null and a.createdAt >= :inicio and a.createdAt < :fimExclusivo")
    long countPendentesPeriodo(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fimExclusivo") LocalDateTime fimExclusivo);

    @Query("select a.nota, count(a) from AvaliacaoAtendimento a where a.empresa.id = :empresa and a.nota is not null and a.respondidaAt >= :inicio and a.respondidaAt < :fimExclusivo group by a.nota order by a.nota desc")
    List<Object[]> distribuicaoNotas(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fimExclusivo") LocalDateTime fimExclusivo);

    @Query("select a.profissional.nome, count(a), coalesce(avg(a.nota),0) from AvaliacaoAtendimento a where a.empresa.id = :empresa and a.nota is not null and a.respondidaAt >= :inicio and a.respondidaAt < :fimExclusivo group by a.profissional.id, a.profissional.nome order by avg(a.nota) desc, count(a) desc")
    List<Object[]> satisfacaoPorProfissional(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fimExclusivo") LocalDateTime fimExclusivo, Pageable pageable);

    @Query("select coalesce(a.atendimento.servicoNome, a.atendimento.tipo), count(a), coalesce(avg(a.nota),0) from AvaliacaoAtendimento a where a.empresa.id = :empresa and a.nota is not null and a.respondidaAt >= :inicio and a.respondidaAt < :fimExclusivo group by coalesce(a.atendimento.servicoNome, a.atendimento.tipo) order by avg(a.nota) desc, count(a) desc")
    List<Object[]> satisfacaoPorServico(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fimExclusivo") LocalDateTime fimExclusivo, Pageable pageable);

    @Query(value = "select date_trunc('month', respondida_at) as mes, count(*) as quantidade, coalesce(avg(nota),0) as media from avaliacoes_atendimento where empresa_id = :empresa and nota is not null and respondida_at >= :inicio and respondida_at < :fimExclusivo group by date_trunc('month', respondida_at) order by mes", nativeQuery = true)
    List<Object[]> tendenciaMensal(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fimExclusivo") LocalDateTime fimExclusivo);

    @Query("select count(a) from AvaliacaoAtendimento a where a.empresa.id = :empresa and a.nota = 5 and a.respondidaAt >= :inicio and a.respondidaAt < :fimExclusivo")
    long countCincoEstrelasPeriodo(@Param("empresa") Long empresa, @Param("inicio") LocalDateTime inicio, @Param("fimExclusivo") LocalDateTime fimExclusivo);
}
