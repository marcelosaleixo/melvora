package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.ProdutoRequests;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.service.ProdutoService;
import com.marceloaleixo.melvora.tenant.TenantContext;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService service;
    private final EmpresaRepository empresaRepository;

    public ProdutoController(
            ProdutoService service,
            EmpresaRepository empresaRepository) {
        this.service = service;
        this.empresaRepository = empresaRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> criar(
            @Valid @RequestBody ProdutoRequests.Criar request) {

        Long empresaId = TenantContext.getRequired();

        var empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() ->
                new IllegalStateException(
                    "Empresa do usuário não encontrada."
                )
            );

        service.criar(request, empresa);

        return ResponseEntity.status(201).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public ResponseEntity<Void> buscar(@PathVariable Long id) {
        service.buscar(id);
        return ResponseEntity.ok().build();
    }
}
