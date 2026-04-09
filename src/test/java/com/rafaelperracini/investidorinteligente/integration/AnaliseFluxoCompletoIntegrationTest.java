package com.rafaelperracini.investidorinteligente.integration;

import com.rafaelperracini.investidorinteligente.dto.CotacaoResumo;
import com.rafaelperracini.investidorinteligente.gateway.CotacaoExternaGateway;
import com.rafaelperracini.investidorinteligente.gateway.OllamaGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao ponta a ponta:
 *
 * <ul>
 *   <li>Sobe o contexto Spring completo (Web + JPA + Postgres embutido)</li>
 *   <li>Mocka a {@link CotacaoExternaGateway} (brapi.dev) e o {@link OllamaGateway}
 *       — nao dependemos de rede nem do Ollama local no CI</li>
 *   <li>POST /api/analises -> persiste no banco -> GET /api/analises/{id} -> verifica</li>
 *   <li>Valida fluxo HTTP, serializacao JSON, extracao de recomendacao,
 *       paginacao e filtros por ticker</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnaliseFluxoCompletoIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private CotacaoExternaGateway cotacaoGateway;

    @MockitoBean
    private OllamaGateway ollamaGateway;

    @Test
    void deveAnalisarEConsultarAcaoComSucesso() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // Mock da cotacao (brapi.dev)
        CotacaoResumo cotacao = new CotacaoResumo(
                "PETR4", "Petrobras", 38.50, -1.23, 8.5, 5.2);
        when(cotacaoGateway.buscarCotacao("PETR4")).thenReturn(Optional.of(cotacao));

        // Mock do Ollama: primeira linha = recomendacao, resto = parecer
        when(ollamaGateway.chat(anyString(), anyString()))
                .thenReturn("COMPRA\nPreco atrativo, P/L baixo e DY saudavel.");

        // POST /api/analises
        String response = mockMvc.perform(post("/api/analises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"PETR4\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.empresa").value("Petrobras"))
                .andExpect(jsonPath("$.preco").value(38.50))
                .andExpect(jsonPath("$.recomendacao").value("COMPRA"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extrai id e valida que o GET por id funciona (persistencia real)
        int idStart = response.indexOf("\"id\":") + 5;
        int idEnd = response.indexOf(",", idStart);
        String id = response.substring(idStart, idEnd).trim();

        mockMvc.perform(get("/api/analises/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.recomendacao").value("COMPRA"));

        // GET /api/analises?ticker=PETR4 deve listar a analise recem-criada
        mockMvc.perform(get("/api/analises").param("ticker", "PETR4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.totalElements").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void deveRetornar422QuandoCotacaoIndisponivel() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        when(cotacaoGateway.buscarCotacao("XXXX9")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/analises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"XXXX9\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void deveRetornar400QuandoTickerInvalido() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // Ticker nao bate o regex ^[A-Za-z]{4}\d{1,2}$
        mockMvc.perform(post("/api/analises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"INVALIDO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404QuandoAnaliseInexistente() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(get("/api/analises/999999"))
                .andExpect(status().isNotFound());
    }
}
