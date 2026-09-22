package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.MegaHairRequests;
import com.marceloaleixo.melvora.service.MegaHairService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mega-hair")
public class MegaHairController {
    private final MegaHairService service;
    public MegaHairController(MegaHairService service) { this.service = service; }

    @PostMapping("/aplicacoes")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    @ResponseStatus(HttpStatus.CREATED)
    public MegaHairRequests.AplicacaoResponse aplicar(@Valid @RequestBody MegaHairRequests.AplicarRequest request) {
        return MegaHairRequests.AplicacaoResponse.from(service.aplicar(request));
    }

    @PostMapping("/aplicacoes/{aplicacaoId}/manutencoes")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    @ResponseStatus(HttpStatus.CREATED)
    public MegaHairRequests.ManutencaoResponse manutencao(@PathVariable Long aplicacaoId,
                                                          @RequestParam @NotNull Long profissionalId,
                                                          @Valid @RequestBody MegaHairRequests.ManutencaoRequest request) {
        return MegaHairRequests.ManutencaoResponse.from(service.registrarManutencao(aplicacaoId, profissionalId, request));
    }

    @GetMapping("/clientes/{clienteId}/historico")
    @PreAuthorize("isAuthenticated()")
    public MegaHairRequests.HistoricoResponse historico(@PathVariable Long clienteId) {
        return service.historico(clienteId);
    }
}
