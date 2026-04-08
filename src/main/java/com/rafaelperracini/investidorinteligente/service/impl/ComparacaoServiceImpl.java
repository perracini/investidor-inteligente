package com.rafaelperracini.investidorinteligente.service.impl;

import com.rafaelperracini.investidorinteligente.dto.ComparacaoResponse;
import com.rafaelperracini.investidorinteligente.dto.CotacaoResumo;
import com.rafaelperracini.investidorinteligente.exception.CotacaoIndisponivelException;
import com.rafaelperracini.investidorinteligente.gateway.CotacaoExternaGateway;
import com.rafaelperracini.investidorinteligente.gateway.OllamaGateway;
import com.rafaelperracini.investidorinteligente.service.ComparacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComparacaoServiceImpl implements ComparacaoService {

    private static final Logger log = LoggerFactory.getLogger(ComparacaoServiceImpl.class);

    private static final String SYSTEM_PROMPT =
            "Voce e um analista fundamentalista de acoes brasileiras. " +
            "Compare as acoes fornecidas e indique qual e a melhor opcao de investimento.\n\n" +
            "Sua comparacao deve:\n" +
            "1. Analisar cada acao individualmente (preco, P/L, DY, variacao)\n" +
            "2. Comparar os indicadores entre as acoes\n" +
            "3. Identificar pontos fortes e fracos de cada uma\n" +
            "4. Indicar qual acao se destaca e por que\n" +
            "5. Concluir com um ranking de preferencia\n\n" +
            "Seja objetivo e base sua analise nos dados fornecidos.";

    private final CotacaoExternaGateway cotacaoGateway;
    private final OllamaGateway ollamaGateway;

    public ComparacaoServiceImpl(CotacaoExternaGateway cotacaoGateway,
                                 OllamaGateway ollamaGateway) {
        this.cotacaoGateway = cotacaoGateway;
        this.ollamaGateway = ollamaGateway;
    }

    @Override
    public ComparacaoResponse comparar(List<String> tickers) {
        log.info("Iniciando comparacao entre: {}", tickers);

        List<CotacaoResumo> cotacoes = new ArrayList<>();
        for (String ticker : tickers) {
            CotacaoResumo cotacao = cotacaoGateway.buscarCotacao(ticker)
                    .orElseThrow(() -> new CotacaoIndisponivelException(ticker));
            cotacoes.add(cotacao);
        }

        String dadosComparacao = cotacoes.stream()
                .map(c -> String.format(
                        "- %s (%s): Preco R$ %.2f | Variacao %.2f%% | DY %.1f%% | P/L %.1f",
                        c.ticker(), c.empresa(), c.preco(), c.variacao(),
                        c.dividendYield(), c.pl()))
                .collect(Collectors.joining("\n"));

        String prompt = "Compare estas acoes e indique a melhor opcao:\n" + dadosComparacao;

        String parecer = ollamaGateway.chat(SYSTEM_PROMPT, prompt);
        log.info("Comparacao concluida entre {} acoes", cotacoes.size());

        return new ComparacaoResponse(cotacoes, parecer, LocalDateTime.now());
    }
}
