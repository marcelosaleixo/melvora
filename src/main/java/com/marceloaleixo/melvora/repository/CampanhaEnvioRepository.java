package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.CampanhaEnvio;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampanhaEnvioRepository extends JpaRepository<CampanhaEnvio, Long> {
    boolean existsByCampanhaIdAndClienteId(Long campanhaId, Long clienteId);
    long countByCampanhaIdAndStatus(Long campanhaId, CampanhaEnvio.Status status);
    @Query("select max(e.dataHoraEnvio) from CampanhaEnvio e where e.empresa.id=:empresa and e.cliente.id=:cliente and e.status=:status")
    LocalDateTime ultimaEnviada(@Param("empresa") Long empresa, @Param("cliente") Long cliente, @Param("status") CampanhaEnvio.Status status);
    @Query("select count(e) from CampanhaEnvio e where e.empresa.id=:empresa and e.status=:status and e.dataHoraEnvio >= :inicio and e.dataHoraEnvio < :fim")
    long countEnviadasPeriodo(@Param("empresa") Long empresa, @Param("status") CampanhaEnvio.Status status, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
