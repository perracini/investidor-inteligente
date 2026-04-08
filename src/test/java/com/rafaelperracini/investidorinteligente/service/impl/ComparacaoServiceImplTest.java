package com.rafaelperracini.investidorinteligente.service.impl;

import com.rafaelperracini.investidorinteligente.dto.ComparacaoResponse;
import com.rafaelperracini.investidorinteligente.dto.CotacaoResumo;
import com.rafaelperracini.investidorinteligente.exception.CotacaoIndisponivelException;
import com.rafaelperracini.investidorinteligente.gateway.CotacaoExternaGateway;
import com.rafaelperracini.investidorinteligente.gateway.OllamaGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComparacaoServiceImplTest {

    @Mock
    private CotacaoExternaGateway cotacaoGateway;

    @Mock
    private OllamaGateway ollamaGateway;

    @InjectMocks
    private ComparacaoServiceImpl service;

    @Test
    void deveCompararDuasAcoesComSucesso() {
        when(cotacaoGateway.buscarCotacao("PETR4"))
                .thenReturn(Optional.of(new CotacaoResumo("PETR4", "Petrobras", 38.50, -1.23, 8.5, 5.2)));
        when(cotacaoGateway.buscarCotacao("VALE3"))
                .thenReturn(Optional.of(new CotacaoResumo("VALE3", "Vale", 60.0, 2.0, 7.0, 6.0)));
        when(ollamaGateway.chat(anyString(), anyString()))
                .thenReturn("PETR4 apresenta melhor DY, VALE3 tem P/L mais atrativo.");

        ComparacaoResponse response = service.comparar(List.of("PETR4", "VALE3"));

        assertEquals(2, response.acoes().size());
        assertNotNull(response.parecer());
        assertNotNull(response.criadoEm());
    }

    @Test
    void deveLancarExceptionQuandoUmTickerInvalido() {
        when(cotacaoGateway.buscarCotacao("PETR4"))
                .thenReturn(Optional.of(new CotacaoResumo("PETR4", "Petrobras", 38.50, -1.23, 8.5, 5.2)));
        when(cotacaoGateway.buscarCotacao("XYZW3"))
                .thenReturn(Optional.empty());

        assertThrows(CotacaoIndisponivelException.class,
                () -> service.comparar(List.of("PETR4", "XYZW3")));
    }

    @Test
    void deveCompararTresAcoes() {
        when(cotacaoGateway.buscarCotacao("PETR4"))
                .thenReturn(Optional.of(new CotacaoResumo("PETR4", "Petrobras", 38.50, -1.23, 8.5, 5.2)));
        when(cotacaoGateway.buscarCotacao("VALE3"))
                .thenReturn(Optional.of(new CotacaoResumo("VALE3", "Vale", 60.0, 2.0, 7.0, 6.0)));
        when(cotacaoGateway.buscarCotacao("ITUB4"))
                .thenReturn(Optional.of(new CotacaoResumo("ITUB4", "Itau", 30.0, 0.5, 5.0, 10.0)));
        when(ollamaGateway.chat(anyString(), anyString()))
                .thenReturn("Ranking: 1) PETR4, 2) VALE3, 3) ITUB4");

        ComparacaoResponse response = service.comparar(List.of("PETR4", "VALE3", "ITUB4"));

        assertEquals(3, response.acoes().size());
    }
}
