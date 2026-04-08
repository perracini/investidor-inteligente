package com.rafaelperracini.investidorinteligente.gateway.impl;

import com.rafaelperracini.investidorinteligente.dto.CotacaoResumo;
import com.rafaelperracini.investidorinteligente.gateway.CotacaoExternaGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class BrapiCotacaoExternaGatewayImpl implements CotacaoExternaGateway {

    private static final Logger log = LoggerFactory.getLogger(BrapiCotacaoExternaGatewayImpl.class);

    private final RestClient restClient;

    public BrapiCotacaoExternaGatewayImpl(@Value("${brapi.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    @Cacheable(value = "cotacoes", key = "#ticker.toUpperCase()")
    public Optional<CotacaoResumo> buscarCotacao(String ticker) {
        log.info("Cache MISS — buscando cotacao na brapi.dev: {}", ticker.toUpperCase());
        try {
            BrapiResponse response = restClient.get()
                    .uri("/quote/{ticker}", ticker.toUpperCase())
                    .retrieve()
                    .body(BrapiResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return Optional.empty();
            }

            BrapiQuote quote = response.results().get(0);

            return Optional.of(new CotacaoResumo(
                    quote.symbol() != null ? quote.symbol() : ticker.toUpperCase(),
                    quote.longName() != null ? quote.longName() : "N/A",
                    quote.regularMarketPrice() != null ? quote.regularMarketPrice() : 0.0,
                    quote.regularMarketChangePercent() != null ? quote.regularMarketChangePercent() : 0.0,
                    quote.dividendYield() != null ? quote.dividendYield() : 0.0,
                    quote.priceEarnings() != null ? quote.priceEarnings() : 0.0
            ));
        } catch (Exception e) {
            log.error("Erro ao buscar cotacao na brapi.dev para {}: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    private record BrapiResponse(List<BrapiQuote> results) {}

    private record BrapiQuote(
            String symbol,
            String longName,
            Double regularMarketPrice,
            Double regularMarketChangePercent,
            Double dividendYield,
            Double priceEarnings
    ) {}
}
