package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.PreferenciaComunicacaoContato;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenciaComunicacaoContatoRepository extends JpaRepository<PreferenciaComunicacaoContato, Long> {
    Optional<PreferenciaComunicacaoContato> findByEmpresaIdAndTelefone(Long empresaId, String telefone);
    Optional<PreferenciaComunicacaoContato> findByEmpresaIdAndClienteId(Long empresaId, Long clienteId);
}
