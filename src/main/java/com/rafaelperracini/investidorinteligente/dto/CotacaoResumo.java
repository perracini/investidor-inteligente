package com.rafaelperracini.investidorinteligente.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados resumidos de cotacao de uma acao")
public record CotacaoResumo(

        @Schema(description = "Ticker", example = "PETR4")
        String ticker,

        @Schema(description = "Nome da empresa", example = "Petrobras S.A.")
        String empresa,

        @Schema(description = "Preco atual", example = "38.50")
        double preco,

        @Schema(description = "Variacao no dia (%)", example = "-1.23")
        double variacao,

        @Schema(description = "Dividend Yield (%)", example = "8.5")
        double dividendYield,

        @Schema(description = "P/L", example = "5.2")
        double pl
) {}
