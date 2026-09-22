package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Produto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    Optional<Produto> findByIdAndEmpresaId(Long id, Long empresaId);
    Page<Produto> findByEmpresaId(Long empresaId, Pageable pageable);
    List<Produto> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(Long empresaId);
    long countByEmpresaIdAndAtivoTrue(Long empresaId);
    long countByAtivo(boolean ativo);
    boolean existsByEmpresaIdAndNomeIgnoreCase(Long empresaId, String nome);
    boolean existsByEmpresaIdAndNomeIgnoreCaseAndIdNot(Long empresaId, String nome, Long id);
}
