package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.RetencaoEnvio;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetencaoEnvioRepository extends JpaRepository<RetencaoEnvio, Long> {
    Optional<RetencaoEnvio> findFirstByEmpresaIdAndClienteIdAndStatusOrderByDataHoraEnvioDesc(Long empresaId, Long clienteId, RetencaoEnvio.Status status);
    long countByEmpresaIdAndStatusAndDataHoraEnvioGreaterThanEqualAndDataHoraEnvioLessThan(Long empresaId, RetencaoEnvio.Status status, LocalDateTime inicio, LocalDateTime fim);
}
