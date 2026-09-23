package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.TemplateWhatsApp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateWhatsAppRepository extends JpaRepository<TemplateWhatsApp, Long> {
    Page<TemplateWhatsApp> findByEmpresaId(Long empresaId, Pageable pageable);
    List<TemplateWhatsApp> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(Long empresaId);
    Optional<TemplateWhatsApp> findByIdAndEmpresaId(Long id, Long empresaId);
    Optional<TemplateWhatsApp> findByEmpresaIdAndNomeIgnoreCase(Long empresaId, String nome);
    boolean existsByEmpresaIdAndNomeIgnoreCase(Long empresaId, String nome);
}
