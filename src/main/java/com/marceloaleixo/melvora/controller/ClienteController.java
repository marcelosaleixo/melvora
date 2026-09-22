package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.ClienteRequests;
import com.marceloaleixo.melvora.service.ClienteService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@Validated
public class ClienteController {
    private final ClienteService service;
    public ClienteController(ClienteService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public ResponseEntity<ClienteRequests.ClienteResponse> criar(@Valid @RequestBody ClienteRequests.CriarClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteRequests.ClienteResponse.from(service.criar(request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Object listar(@RequestParam(defaultValue = "0") @Min(0) int page,
                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.listar(PageRequest.of(page, size, Sort.by("nome").ascending()))
                .map(ClienteRequests.ClienteResponse::from);
    }
}
