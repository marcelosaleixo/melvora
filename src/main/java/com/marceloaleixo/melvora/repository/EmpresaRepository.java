package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Empresa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    long countByAtivaTrue();
    boolean existsByNomeFantasiaIgnoreCase(String nomeFantasia);
}
