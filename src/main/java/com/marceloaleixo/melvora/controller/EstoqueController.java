package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.service.EstoqueService;
import com.marceloaleixo.melvora.dto.LoteRequests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estoque/lotes")
@Validated
public class EstoqueController {
    private final EstoqueService service;
    public EstoqueController(EstoqueService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public LoteRequests.LoteResponse criar(@Valid @RequestBody LoteRequests.CriarLoteRequest request) {
        return LoteRequests.LoteResponse.from(service.criarLote(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Object listar(@RequestParam(defaultValue = "0") @Min(0) int page,
                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.listar(PageRequest.of(page, size, Sort.by("codigo").ascending())).map(LoteRequests.LoteResponse::from);
    }
}
