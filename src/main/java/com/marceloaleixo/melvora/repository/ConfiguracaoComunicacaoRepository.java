package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.ConfiguracaoComunicacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoComunicacaoRepository extends JpaRepository<ConfiguracaoComunicacao, Long> {
    Optional<ConfiguracaoComunicacao> findByEmpresaId(Long empresaId);
}
