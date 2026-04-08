package com.rafaelperracini.investidorinteligente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Requisicao de comparacao entre acoes")
public record ComparacaoRequest(

        @Schema(description = "Lista de tickers para comparar (2 a 5)", example = "[\"PETR4\", \"VALE3\", \"ITUB4\"]")
        @NotEmpty(message = "Informe pelo menos 2 tickers")
        @Size(min = 2, max = 5, message = "Informe entre 2 e 5 tickers")
        List<String> tickers
) {}
