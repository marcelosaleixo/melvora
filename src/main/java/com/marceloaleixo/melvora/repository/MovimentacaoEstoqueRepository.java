package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.MovimentacaoEstoque;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    List<MovimentacaoEstoque> findTop100ByEmpresaIdAndLoteIdOrderByCreatedAtDesc(Long empresaId, Long loteId);
}
