package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Usuario;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Query("select u from Usuario u left join fetch u.empresa where lower(u.email) = lower(:email)")
    Optional<Usuario> findByEmailIgnoreCase(@Param("email") String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaIdAndAtivoTrue(Long empresaId);
    List<Usuario> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(Long empresaId);
    org.springframework.data.domain.Page<Usuario> findByEmpresaId(Long empresaId, org.springframework.data.domain.Pageable pageable);
    long countByAtivo(boolean ativo);
    long countByEmpresaIdAndAtivoTrueAndRole(Long empresaId, com.marceloaleixo.melvora.entity.enums.Role role);
}
