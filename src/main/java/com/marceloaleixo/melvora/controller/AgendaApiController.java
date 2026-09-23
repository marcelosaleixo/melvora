package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.AgendaRequests;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.service.AgendaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agenda")
@Validated
public class AgendaApiController {
    private final AgendaService service;
    public AgendaApiController(AgendaService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public List<AgendaRequests.AgendamentoResponse> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.listarPeriodo(inicio, fim).stream().map(AgendaRequests.AgendamentoResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendaRequests.AgendamentoResponse criar(@Valid @RequestBody AgendaRequests.CriarAgendamentoRequest request) {
        return AgendaRequests.AgendamentoResponse.from(service.criar(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public AgendaRequests.AgendamentoResponse status(@PathVariable Long id,
                                                       @Valid @RequestBody AgendaRequests.AlterarStatusRequest request) {
        return AgendaRequests.AgendamentoResponse.from(service.alterarStatus(id, request.status()));
    }
}
