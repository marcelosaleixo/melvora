package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.LoteMegaHair;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoteMegaHairRepository extends JpaRepository<LoteMegaHair, Long> {
    @EntityGraph(attributePaths = "produto")
    Optional<LoteMegaHair> findByIdAndEmpresaId(Long id, Long empresaId);

    @EntityGraph(attributePaths = "produto")
    Page<LoteMegaHair> findByEmpresaId(Long empresaId, Pageable pageable);
    long countByEmpresaId(Long empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoteMegaHair l where l.id = :id and l.empresa.id = :empresaId")
    Optional<LoteMegaHair> findByIdAndEmpresaIdForUpdate(@Param("id") Long id, @Param("empresaId") Long empresaId);

    boolean existsByEmpresaIdAndCodigo(Long empresaId, String codigo);
    boolean existsByEmpresaIdAndProdutoId(Long empresaId, Long produtoId);
}
