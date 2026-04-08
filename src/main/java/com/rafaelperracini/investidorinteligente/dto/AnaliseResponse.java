package com.rafaelperracini.investidorinteligente.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resultado da analise fundamentalista de uma acao")
public record AnaliseResponse(

        @Schema(description = "ID da analise")
        Long id,

        @Schema(description = "Ticker da acao", example = "PETR4")
        String ticker,

        @Schema(description = "Nome da empresa", example = "Petrobras S.A.")
        String empresa,

        @Schema(description = "Preco atual da acao", example = "38.50")
        double preco,

        @Schema(description = "Variacao percentual no dia", example = "-1.23")
        double variacao,

        @Schema(description = "Dividend Yield anual (%)", example = "8.5")
        double dividendYield,

        @Schema(description = "Preco/Lucro", example = "5.2")
        double pl,

        @Schema(description = "Recomendacao da IA", example = "COMPRA")
        String recomendacao,

        @Schema(description = "Parecer fundamentalista gerado pela IA")
        String parecer,

        @Schema(description = "Data da analise")
        LocalDateTime criadoEm
) {}
