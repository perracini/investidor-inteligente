package com.rafaelperracini.investidorinteligente.controller;

import com.rafaelperracini.investidorinteligente.dto.AnaliseRequest;
import com.rafaelperracini.investidorinteligente.dto.AnaliseResponse;
import com.rafaelperracini.investidorinteligente.service.AnaliseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analises", description = "Analise fundamentalista de acoes com IA")
@RestController
@RequestMapping("/api/analises")
public class AnaliseController {

    private final AnaliseService analiseService;

    public AnaliseController(AnaliseService analiseService) {
        this.analiseService = analiseService;
    }

    @Operation(summary = "Analisar acao",
               description = "Busca cotacao em tempo real na brapi.dev, envia para IA analisar " +
                       "e retorna parecer fundamentalista com recomendacao (COMPRA/VENDA/MANTER). " +
                       "A analise e persistida no banco para historico.")
    @ApiResponse(responseCode = "200", description = "Analise concluida")
    @ApiResponse(responseCode = "422", description = "Cotacao indisponivel para o ticker informado")
    @PostMapping
    public AnaliseResponse analisar(@Valid @RequestBody AnaliseRequest request) {
        return analiseService.analisar(request.ticker());
    }

    @Operation(summary = "Buscar analise por ID",
               description = "Retorna uma analise especifica pelo seu ID")
    @ApiResponse(responseCode = "200", description = "Analise encontrada")
    @ApiResponse(responseCode = "404", description = "Analise nao encontrada")
    @GetMapping("/{id}")
    public AnaliseResponse buscarPorId(
            @Parameter(description = "ID da analise") @PathVariable Long id) {
        return analiseService.buscarPorId(id);
    }

    @Operation(summary = "Listar historico de analises",
               description = "Retorna todas as analises ja realizadas, com paginacao. " +
                       "Permite filtrar por ticker.")
    @ApiResponse(responseCode = "200", description = "Lista de analises")
    @GetMapping
    public Page<AnaliseResponse> listar(
            @Parameter(description = "Filtro por ticker (ex: PETR4)")
            @RequestParam(required = false) String ticker,
            @PageableDefault(size = 20, sort = "criadoEm") Pageable pageable) {
        return analiseService.listar(ticker, pageable);
    }
}
