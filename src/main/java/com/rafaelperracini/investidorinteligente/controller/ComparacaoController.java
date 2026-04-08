package com.rafaelperracini.investidorinteligente.controller;

import com.rafaelperracini.investidorinteligente.dto.ComparacaoRequest;
import com.rafaelperracini.investidorinteligente.dto.ComparacaoResponse;
import com.rafaelperracini.investidorinteligente.service.ComparacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comparacoes", description = "Comparacao fundamentalista entre acoes")
@RestController
@RequestMapping("/api/comparacoes")
public class ComparacaoController {

    private final ComparacaoService comparacaoService;

    public ComparacaoController(ComparacaoService comparacaoService) {
        this.comparacaoService = comparacaoService;
    }

    @Operation(summary = "Comparar acoes",
               description = "Busca cotacoes em tempo real de 2 a 5 acoes, " +
                       "envia para IA comparar e retorna parecer com ranking de preferencia.")
    @ApiResponse(responseCode = "200", description = "Comparacao concluida")
    @ApiResponse(responseCode = "422", description = "Cotacao indisponivel para algum ticker")
    @PostMapping
    public ComparacaoResponse comparar(@Valid @RequestBody ComparacaoRequest request) {
        return comparacaoService.comparar(request.tickers());
    }
}
