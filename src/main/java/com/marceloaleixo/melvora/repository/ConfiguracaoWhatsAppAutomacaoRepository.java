package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.ConfiguracaoWhatsAppAutomacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoWhatsAppAutomacaoRepository extends JpaRepository<ConfiguracaoWhatsAppAutomacao, Long> {
    Optional<ConfiguracaoWhatsAppAutomacao> findByEmpresaId(Long empresaId);
}
