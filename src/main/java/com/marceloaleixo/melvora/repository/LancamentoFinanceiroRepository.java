package com.marceloaleixo.melvora.repository;
import com.marceloaleixo.melvora.entity.LancamentoFinanceiro;
import com.marceloaleixo.melvora.entity.enums.*;
import java.math.BigDecimal; import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro,Long> {
 @EntityGraph(attributePaths={"cliente","atendimento"}) Page<LancamentoFinanceiro> findByEmpresaId(Long empresaId, Pageable pageable);
 Optional<LancamentoFinanceiro> findByIdAndEmpresaId(Long id, Long empresaId);
 boolean existsByAtendimentoIdAndEmpresaId(Long atendimentoId, Long empresaId);
 @Query("select coalesce(sum(l.valor),0) from LancamentoFinanceiro l where l.empresa.id=:empresa and l.tipo=:tipo and l.status=:status and l.dataMovimento between :inicio and :fim") BigDecimal total(@Param("empresa") Long empresa,@Param("tipo") TipoLancamentoFinanceiro tipo,@Param("status") StatusLancamentoFinanceiro status,@Param("inicio") LocalDate inicio,@Param("fim") LocalDate fim);
 @Query("select count(l) from LancamentoFinanceiro l where l.empresa.id=:empresa and l.tipo=:tipo and l.status=:status and l.dataMovimento between :inicio and :fim") long countByPeriodo(@Param("empresa") Long empresa,@Param("tipo") TipoLancamentoFinanceiro tipo,@Param("status") StatusLancamentoFinanceiro status,@Param("inicio") LocalDate inicio,@Param("fim") LocalDate fim);
}

