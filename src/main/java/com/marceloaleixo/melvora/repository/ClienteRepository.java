package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Cliente;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByIdAndEmpresaId(Long id, Long empresaId);
    Page<Cliente> findByEmpresaIdAndAtivoTrue(Long empresaId, Pageable pageable);
    Page<Cliente> findByEmpresaId(Long empresaId, Pageable pageable);
    long countByEmpresaId(Long empresaId);
    long countByEmpresaIdAndAtivoTrue(Long empresaId);
    long countByAtivo(boolean ativo);

    @Query(value = "SELECT * FROM clientes c WHERE c.empresa_id = :empresaId AND c.ativo = true " +
            "AND right(regexp_replace(coalesce(c.telefone, ''), '[^0-9]', '', 'g'), 11) = right(:telefone, 11) " +
            "ORDER BY c.id LIMIT 1", nativeQuery = true)
    Optional<Cliente> findAtivaByEmpresaIdAndTelefone(@Param("empresaId") Long empresaId, @Param("telefone") String telefone);
    @Query(value = "SELECT c.id FROM clientes c WHERE c.empresa_id = :empresaId AND c.ativo = true AND c.telefone IS NOT NULL AND trim(c.telefone) <> '' " +
            "AND NOT EXISTS (SELECT 1 FROM agendamentos a WHERE a.empresa_id = :empresaId AND a.cliente_id = c.id " +
            "AND a.status IN ('AGENDADO','CONFIRMADO','EM_ATENDIMENTO') AND a.data_hora_inicio >= CURRENT_TIMESTAMP) " +
            "ORDER BY c.nome", nativeQuery = true)
    java.util.List<Long> idsSemProximoAgendamento(@Param("empresaId") Long empresaId);

}
