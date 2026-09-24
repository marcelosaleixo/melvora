package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.CampanhaComunicacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampanhaComunicacaoRepository extends JpaRepository<CampanhaComunicacao, Long> {
    List<CampanhaComunicacao> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId);
    Optional<CampanhaComunicacao> findByIdAndEmpresaId(Long id, Long empresaId);
}
