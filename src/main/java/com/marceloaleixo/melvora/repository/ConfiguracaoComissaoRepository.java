package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.ConfiguracaoComissao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ConfiguracaoComissaoRepository extends JpaRepository<ConfiguracaoComissao, Long> {
    @EntityGraph(attributePaths = {"profissional"})
    List<ConfiguracaoComissao> findByEmpresaIdOrderByProfissionalNomeAsc(Long empresaId);
    @EntityGraph(attributePaths = {"profissional"})
    Optional<ConfiguracaoComissao> findByEmpresaIdAndProfissionalId(Long empresaId, Long profissionalId);
}
