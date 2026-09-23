package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Usuario;

import java.util.Optional;
import java.util.List;
import java.util.Collection;
import com.marceloaleixo.melvora.entity.enums.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Query("select u from Usuario u left join fetch u.empresa where lower(u.email) = lower(:email)")
    Optional<Usuario> findByEmailIgnoreCase(@Param("email") String email);
    List<Usuario> findByEmpresaIdAndAtivoTrueAndRoleInOrderByNomeAsc(Long empresaId, Collection<Role> roles);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaIdAndAtivoTrue(Long empresaId);
    List<Usuario> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(Long empresaId);
    @Query("select u from Usuario u where u.empresa.id = :empresaId and u.ativo = true and u.role in (com.marceloaleixo.melvora.entity.enums.Role.ADMIN, com.marceloaleixo.melvora.entity.enums.Role.PROFISSIONAL) order by u.nome asc")
    java.util.List<Usuario> findProfissionaisAtivos(@Param("empresaId") Long empresaId);

    // Carrega a empresa junto com o usuário para evitar LazyInitializationException durante o render Thymeleaf.
    @EntityGraph(attributePaths = "empresa")
    org.springframework.data.domain.Page<Usuario> findByEmpresaId(Long empresaId, org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = "empresa")
    @Query("select u from Usuario u")
    org.springframework.data.domain.Page<Usuario> findAllWithEmpresa(org.springframework.data.domain.Pageable pageable);
    long countByAtivo(boolean ativo);
    long countByEmpresaIdAndAtivoTrueAndRole(Long empresaId, com.marceloaleixo.melvora.entity.enums.Role role);
}
