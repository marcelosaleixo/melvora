package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Servico;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
    @EntityGraph(attributePaths = "profissionais")
    Page<Servico> findByEmpresaId(Long empresaId, Pageable pageable);

    @EntityGraph(attributePaths = "profissionais")
    Optional<Servico> findByIdAndEmpresaId(Long id, Long empresaId);

    @Query("select s from Servico s where s.empresa.id = :empresaId and s.ativo = true order by s.categoria asc, s.nome asc")
    List<Servico> findAtivosByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("select s from Servico s join s.profissionais p where s.empresa.id = :empresaId and s.ativo = true and p.id = :profissionalId order by s.categoria asc, s.nome asc")
    List<Servico> findAtivosDoProfissional(@Param("empresaId") Long empresaId, @Param("profissionalId") Long profissionalId);
}
