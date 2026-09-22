package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.EmpresaModulo;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmpresaModuloRepository extends JpaRepository<EmpresaModulo, Long> {
    List<EmpresaModulo> findByEmpresaIdOrderByModuloAsc(Long empresaId);

    @Query("select em.modulo from EmpresaModulo em where em.empresa.id = :empresaId and em.ativo = true")
    List<ModuloSistema> findModulosAtivos(@Param("empresaId") Long empresaId);

    Optional<EmpresaModulo> findByEmpresaIdAndModulo(Long empresaId, ModuloSistema modulo);
}
