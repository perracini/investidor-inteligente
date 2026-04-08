package com.rafaelperracini.investidorinteligente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Requisicao de analise de acao")
public record AnaliseRequest(

        @Schema(description = "Ticker da acao (ex: PETR4, VALE3, ITUB4)", example = "PETR4")
        @NotBlank(message = "Ticker e obrigatorio")
        @Pattern(regexp = "^[A-Za-z]{4}\\d{1,2}$", message = "Ticker invalido. Use o formato: 4 letras + 1 ou 2 digitos (ex: PETR4, BOVA11)")
        String ticker
) {}
