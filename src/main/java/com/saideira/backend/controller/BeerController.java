package com.saideira.backend.controller;

import com.saideira.backend.dto.CadastrarCervejaRequest;
import com.saideira.backend.dto.CervejaResponse;
import com.saideira.backend.security.UsuarioAutenticado;
import com.saideira.backend.service.BeerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cervejas")
public class BeerController {

    private final BeerService beerService;

    public BeerController(BeerService beerService) {
        this.beerService = beerService;
    }

    /** Autocomplete da tela de check-in. Busca por nome, cervejaria ou estilo no nome. */
    @GetMapping
    public ResponseEntity<List<CervejaResponse>> buscar(@RequestParam(required = false) String busca) {
        return ResponseEntity.ok(beerService.buscar(busca));
    }

    /** 201 se a cerveja e nova, 200 se ja existia (devolve a existente, sem duplicar). */
    @PostMapping
    public ResponseEntity<CervejaResponse> cadastrar(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @Valid @RequestBody CadastrarCervejaRequest request
    ) {
        BeerService.Cadastro cadastro = beerService.cadastrar(
            request.nome(), request.estilo(), request.cervejaria(), logado.getId()
        );
        return ResponseEntity.status(cadastro.criada() ? HttpStatus.CREATED : HttpStatus.OK)
            .body(cadastro.cerveja());
    }
}
