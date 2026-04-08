package com.rafaelperracini.investidorinteligente.service.impl;

import com.rafaelperracini.investidorinteligente.dto.AnaliseResponse;
import com.rafaelperracini.investidorinteligente.dto.CotacaoResumo;
import com.rafaelperracini.investidorinteligente.entity.AnaliseEntity;
import com.rafaelperracini.investidorinteligente.entity.TipoRecomendacao;
import com.rafaelperracini.investidorinteligente.exception.AnaliseNaoEncontradaException;
import com.rafaelperracini.investidorinteligente.exception.CotacaoIndisponivelException;
import com.rafaelperracini.investidorinteligente.gateway.CotacaoExternaGateway;
import com.rafaelperracini.investidorinteligente.gateway.OllamaGateway;
import com.rafaelperracini.investidorinteligente.repository.AnaliseRepository;
import com.rafaelperracini.investidorinteligente.service.AnaliseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnaliseServiceImpl implements AnaliseService {

    private static final Logger log = LoggerFactory.getLogger(AnaliseServiceImpl.class);

    private static final String SYSTEM_PROMPT =
            "Voce e um analista fundamentalista de acoes brasileiras. " +
            "Com base nos dados fornecidos, faca uma analise completa e emita um parecer.\n\n" +
            "Sua analise deve conter:\n" +
            "1. Avaliacao do preco atual em relacao ao P/L\n" +
            "2. Avaliacao do Dividend Yield\n" +
            "3. Analise da variacao recente\n" +
            "4. Recomendacao: COMPRA, VENDA ou MANTER\n" +
            "5. Justificativa da recomendacao\n\n" +
            "IMPORTANTE: Na PRIMEIRA linha da resposta, escreva APENAS a recomendacao: COMPRA, VENDA ou MANTER.\n" +
            "Nas linhas seguintes, escreva o parecer completo.";

    private final CotacaoExternaGateway cotacaoGateway;
    private final OllamaGateway ollamaGateway;
    private final AnaliseRepository repository;

    public AnaliseServiceImpl(CotacaoExternaGateway cotacaoGateway,
                              OllamaGateway ollamaGateway,
                              AnaliseRepository repository) {
        this.cotacaoGateway = cotacaoGateway;
        this.ollamaGateway = ollamaGateway;
        this.repository = repository;
    }

    @Override
    @Transactional
    public AnaliseResponse analisar(String ticker) {
        log.info("Iniciando analise fundamentalista de {}", ticker.toUpperCase());

        CotacaoResumo cotacao = cotacaoGateway.buscarCotacao(ticker)
                .orElseThrow(() -> new CotacaoIndisponivelException(ticker));

        String prompt = String.format(
                "Analise fundamentalista da acao %s (%s):\n" +
                "- Preco atual: R$ %.2f\n" +
                "- Variacao no dia: %.2f%%\n" +
                "- Dividend Yield: %.1f%%\n" +
                "- P/L (Preco/Lucro): %.1f",
                cotacao.ticker(), cotacao.empresa(),
                cotacao.preco(), cotacao.variacao(),
                cotacao.dividendYield(), cotacao.pl());

        String resposta = ollamaGateway.chat(SYSTEM_PROMPT, prompt);
        TipoRecomendacao recomendacao = extrairRecomendacao(resposta);
        String parecer = extrairParecer(resposta);

        AnaliseEntity entity = new AnaliseEntity(
                cotacao.ticker(), cotacao.empresa(), cotacao.preco(),
                cotacao.variacao(), cotacao.dividendYield(), cotacao.pl(),
                recomendacao, parecer, LocalDateTime.now());

        entity = repository.save(entity);
        log.info("Analise de {} concluida: {}", ticker.toUpperCase(), recomendacao);

        return toResponse(entity);
    }

    @Override
    public AnaliseResponse buscarPorId(Long id) {
        AnaliseEntity entity = repository.findById(id)
                .orElseThrow(() -> new AnaliseNaoEncontradaException(id));
        return toResponse(entity);
    }

    @Override
    public Page<AnaliseResponse> listarPorTicker(String ticker, Pageable pageable) {
        return repository.findByTickerIgnoreCase(ticker, pageable).map(this::toResponse);
    }

    @Override
    public Page<AnaliseResponse> listarTodas(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    private TipoRecomendacao extrairRecomendacao(String resposta) {
        String primeiraLinha = resposta.split("\n")[0].toUpperCase().trim();
        if (primeiraLinha.contains("COMPRA")) return TipoRecomendacao.COMPRA;
        if (primeiraLinha.contains("VENDA")) return TipoRecomendacao.VENDA;
        if (primeiraLinha.contains("MANTER")) return TipoRecomendacao.MANTER;
        return TipoRecomendacao.INDEFINIDO;
    }

    private String extrairParecer(String resposta) {
        int primeiraQuebra = resposta.indexOf("\n");
        if (primeiraQuebra > 0 && primeiraQuebra < resposta.length() - 1) {
            return resposta.substring(primeiraQuebra + 1).trim();
        }
        return resposta.trim();
    }

    private AnaliseResponse toResponse(AnaliseEntity entity) {
        return new AnaliseResponse(
                entity.getId(), entity.getTicker(), entity.getEmpresa(),
                entity.getPreco(), entity.getVariacao(), entity.getDividendYield(),
                entity.getPl(), entity.getRecomendacao().name(), entity.getParecer(),
                entity.getCriadoEm());
    }
}
