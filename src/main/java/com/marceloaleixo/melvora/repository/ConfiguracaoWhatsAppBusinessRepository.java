package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.ConfiguracaoWhatsAppBusiness;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoWhatsAppBusinessRepository extends JpaRepository<ConfiguracaoWhatsAppBusiness, Long> {
    Optional<ConfiguracaoWhatsAppBusiness> findByEmpresaId(Long empresaId);
    List<ConfiguracaoWhatsAppBusiness> findByAtivaTrue();
    Optional<ConfiguracaoWhatsAppBusiness> findByN8nIntegrationKey(String n8nIntegrationKey);
    Optional<ConfiguracaoWhatsAppBusiness> findByWuzapiIntegrationKey(String wuzapiIntegrationKey);
}
