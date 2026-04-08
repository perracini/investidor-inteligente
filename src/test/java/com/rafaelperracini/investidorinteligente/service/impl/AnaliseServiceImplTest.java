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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnaliseServiceImplTest {

    @Mock
    private CotacaoExternaGateway cotacaoGateway;

    @Mock
    private OllamaGateway ollamaGateway;

    @Mock
    private AnaliseRepository repository;

    @InjectMocks
    private AnaliseServiceImpl service;

    @Test
    void deveAnalisarAcaoComSucesso() {
        CotacaoResumo cotacao = new CotacaoResumo("PETR4", "Petrobras", 38.50, -1.23, 8.5, 5.2);
        when(cotacaoGateway.buscarCotacao("PETR4")).thenReturn(Optional.of(cotacao));
        when(ollamaGateway.chat(anyString(), anyString()))
                .thenReturn("COMPRA\nA acao apresenta bom DY e P/L baixo.");
        when(repository.save(any())).thenAnswer(i -> {
            AnaliseEntity e = i.getArgument(0);
            e.setPreco(e.getPreco()); // simula save
            return e;
        });

        AnaliseResponse response = service.analisar("PETR4");

        assertEquals("PETR4", response.ticker());
        assertEquals("COMPRA", response.recomendacao());
        assertEquals(38.50, response.preco());
        assertNotNull(response.parecer());
        verify(repository).save(any(AnaliseEntity.class));
    }

    @Test
    void deveLancarExceptionQuandoCotacaoIndisponivel() {
        when(cotacaoGateway.buscarCotacao("XYZW3")).thenReturn(Optional.empty());

        assertThrows(CotacaoIndisponivelException.class,
                () -> service.analisar("XYZW3"));
    }

    @Test
    void deveExtrairRecomendacaoVenda() {
        CotacaoResumo cotacao = new CotacaoResumo("OIBR3", "Oi SA", 0.50, -5.0, 0.0, -2.0);
        when(cotacaoGateway.buscarCotacao("OIBR3")).thenReturn(Optional.of(cotacao));
        when(ollamaGateway.chat(anyString(), anyString()))
                .thenReturn("VENDA\nAcao com fundamentos deteriorados.");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        AnaliseResponse response = service.analisar("OIBR3");

        assertEquals("VENDA", response.recomendacao());
    }

    @Test
    void deveExtrairRecomendacaoManter() {
        CotacaoResumo cotacao = new CotacaoResumo("ITUB4", "Itau", 30.0, 0.5, 5.0, 10.0);
        when(cotacaoGateway.buscarCotacao("ITUB4")).thenReturn(Optional.of(cotacao));
        when(ollamaGateway.chat(anyString(), anyString()))
                .thenReturn("MANTER\nAcao estavel, sem grandes catalisadores.");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        AnaliseResponse response = service.analisar("ITUB4");

        assertEquals("MANTER", response.recomendacao());
    }

    @Test
    void deveRetornarIndefinidoQuandoRespostaInvalida() {
        CotacaoResumo cotacao = new CotacaoResumo("VALE3", "Vale", 60.0, 2.0, 7.0, 6.0);
        when(cotacaoGateway.buscarCotacao("VALE3")).thenReturn(Optional.of(cotacao));
        when(ollamaGateway.chat(anyString(), anyString()))
                .thenReturn("Nao tenho certeza sobre esta acao.");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        AnaliseResponse response = service.analisar("VALE3");

        assertEquals("INDEFINIDO", response.recomendacao());
    }

    @Test
    void deveLancarExceptionQuandoAnaliseNaoEncontrada() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AnaliseNaoEncontradaException.class,
                () -> service.buscarPorId(999L));
    }

    @Test
    void deveBuscarAnalisePorId() {
        AnaliseEntity entity = new AnaliseEntity("PETR4", "Petrobras", 38.50, -1.23,
                8.5, 5.2, TipoRecomendacao.COMPRA, "Boa acao", LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        AnaliseResponse response = service.buscarPorId(1L);

        assertEquals("PETR4", response.ticker());
        assertEquals("COMPRA", response.recomendacao());
    }
}
