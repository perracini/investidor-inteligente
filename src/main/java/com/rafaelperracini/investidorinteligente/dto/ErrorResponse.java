package com.rafaelperracini.investidorinteligente.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resposta padronizada de erro")
public record ErrorResponse(

        @Schema(description = "Momento do erro")
        LocalDateTime timestamp,

        @Schema(description = "Codigo HTTP", example = "404")
        int status,

        @Schema(description = "Mensagem de erro")
        String mensagem,

        @Schema(description = "Path da requisicao")
        String path
) {}
