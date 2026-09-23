package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Cliente;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByIdAndEmpresaId(Long id, Long empresaId);
    Page<Cliente> findByEmpresaIdAndAtivoTrue(Long empresaId, Pageable pageable);
    Page<Cliente> findByEmpresaId(Long empresaId, Pageable pageable);
    long countByEmpresaId(Long empresaId);
    long countByEmpresaIdAndAtivoTrue(Long empresaId);
    long countByAtivo(boolean ativo);
}
