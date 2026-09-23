package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Comissao;
import com.marceloaleixo.melvora.entity.enums.StatusComissao;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface ComissaoRepository extends JpaRepository<Comissao, Long> {
    @EntityGraph(attributePaths = {"profissional", "atendimento", "atendimento.cliente", "lancamento"})
    Page<Comissao> findByEmpresaId(Long empresaId, Pageable pageable);
    @EntityGraph(attributePaths = {"profissional", "atendimento", "atendimento.cliente", "lancamento"})
    Page<Comissao> findByEmpresaIdAndProfissionalId(Long empresaId, Long profissionalId, Pageable pageable);
    Optional<Comissao> findByIdAndEmpresaId(Long id, Long empresaId);
    boolean existsByLancamentoIdAndEmpresaId(Long lancamentoId, Long empresaId);
    @Query("select coalesce(sum(c.valor),0) from Comissao c where c.empresa.id=:empresa and c.status=:status and c.dataComissao between :inicio and :fim")
    BigDecimal total(@Param("empresa") Long empresa, @Param("status") StatusComissao status, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
