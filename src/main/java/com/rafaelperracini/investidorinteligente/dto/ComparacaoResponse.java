package com.rafaelperracini.investidorinteligente.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Resultado da comparacao entre acoes")
public record ComparacaoResponse(

        @Schema(description = "Acoes comparadas")
        List<CotacaoResumo> acoes,

        @Schema(description = "Parecer comparativo gerado pela IA")
        String parecer,

        @Schema(description = "Data da comparacao")
        LocalDateTime criadoEm
) {}
