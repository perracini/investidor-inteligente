# Investidor Inteligente

> **Projeto de portfolio** — arquitetura profissional com SOLID, testes, observabilidade,
> resiliencia e documentacao OpenAPI. Desenvolvido para demonstrar dominio em integracao
> Java + IA + APIs externas em cenario realista de investimentos.

API de analise fundamentalista de acoes brasileiras com IA. Busca dados em tempo real
na brapi.dev, envia para IA local (Ollama/Llama 3.2) analisar e emite pareceres de
investimento com recomendacao (COMPRA / VENDA / MANTER).

Tambem permite **comparar** multiplas acoes lado a lado com ranking de preferencia.

**Sem Docker.** PostgreSQL sobe automaticamente com a aplicacao (embarcado).

## Stack

- Java 17+ / Spring Boot 3.5.0
- Spring AI 1.1.4 + Ollama com Llama 3.2 (3B)
- Spring Data JPA + PostgreSQL embarcado (io.zonky.test:embedded-postgres)
- Spring Boot Actuator + Micrometer (observabilidade)
- Spring Retry (retry com backoff exponencial + fallback)
- Caffeine Cache (cache de cotacoes da brapi.dev)
- Semaphore-based Rate Limiter (limite de chamadas concorrentes)
- SpringDoc OpenAPI / Swagger UI (documentacao interativa)
- Bean Validation (jakarta.validation)
- `@RestControllerAdvice` (tratamento global de erros)
- `@Transactional` (consistencia na persistencia)
- Paginacao com Spring Data (`Pageable`, `Page`)
- Testes unitarios (Mockito + JUnit 5)
- Maven (wrapper incluso)

## Como rodar

1. Certifique-se de ter o modelo do Ollama:
   ```
   ollama pull llama3.2
   ```

2. Inicie o Ollama (se nao estiver rodando):
   ```
   ollama serve
   ```

3. Suba a aplicacao:
   ```
   ./mvnw spring-boot:run
   ```
   PostgreSQL embarcado sobe automaticamente.

4. Acesse a documentacao Swagger:
   ```
   http://localhost:8084/swagger-ui.html
   ```

5. Teste via curl ou Postman em `http://localhost:8084`

**Porta 8084** — diferente dos outros projetos (8080-8083).

---

## Conceito: Analise fundamentalista com IA

O projeto combina **dados financeiros reais** com **IA local** para gerar pareceres:

```
POST /api/analises (ticker: "PETR4")
       |
  Gateway busca cotacao em tempo real na brapi.dev
       |
  Service monta prompt com dados reais:
    - Preco atual: R$ 46.46
    - Variacao: -4.23%
    - Dividend Yield: 0.0%
    - P/L: 5.4
       |
  OllamaGateway envia para Llama 3.2 analisar
       |
  IA retorna: recomendacao + parecer completo
       |
  Persistido no PostgreSQL (historico)
       |
  Retorna para o cliente
```

### Diferenca de um chatbot generico

Um chatbot responderia com base no conhecimento treinado (dados antigos).
Este projeto busca **dados em tempo real** e alimenta a IA com eles.
A IA analisa os numeros atuais, nao inventa — o grounding vem da brapi.dev.

---

## Endpoints

### POST /api/analises — Analisar acao

Busca cotacao em tempo real, envia para IA analisar e retorna parecer fundamentalista.
A analise e **persistida no banco** para consulta futura.

**Body:**
```json
{
  "ticker": "PETR4"
}
```

**Resposta:**
```json
{
  "id": 1,
  "ticker": "PETR4",
  "empresa": "Petroleo Brasileiro SA Pfd",
  "preco": 46.46,
  "variacao": -4.23,
  "dividendYield": 0.0,
  "pl": 5.44,
  "recomendacao": "COMPRA",
  "parecer": "O preco atual da PETR4 esta abaixo do P/L historico...",
  "criadoEm": "2026-04-08T16:29:25"
}
```

**Validacao do ticker:** regex `^[A-Za-z]{4}\d{1,2}$` — aceita PETR4, BOVA11, rejeita "INVALIDO".

### POST /api/comparacoes — Comparar acoes

Busca cotacao de 2 a 5 acoes e pede para a IA comparar com ranking de preferencia.

**Body:**
```json
{
  "tickers": ["PETR4", "VALE3", "ITUB4"]
}
```

**Resposta:**
```json
{
  "acoes": [
    { "ticker": "PETR4", "empresa": "Petrobras", "preco": 46.46, "variacao": -4.23, "dividendYield": 0.0, "pl": 5.4 },
    { "ticker": "VALE3", "empresa": "Vale S.A.", "preco": 85.66, "variacao": 2.35, "dividendYield": 0.0, "pl": 26.6 },
    { "ticker": "ITUB4", "empresa": "Itau Unibanco", "preco": 30.00, "variacao": 0.5, "dividendYield": 5.0, "pl": 10.0 }
  ],
  "parecer": "Ranking: 1) VALE3, 2) PETR4, 3) ITUB4...",
  "criadoEm": "2026-04-08T16:34:51"
}
```

### GET /api/analises/{id} — Buscar analise por ID

Retorna uma analise especifica pelo ID.

### GET /api/analises — Listar historico (paginado)

Retorna todas as analises ja realizadas, com paginacao.

**Parametros:**
- `ticker` (opcional): filtrar por ticker
- `page` (default 0): numero da pagina
- `size` (default 20): itens por pagina
- `sort` (default criadoEm): campo de ordenacao

**Exemplos:**
```
GET /api/analises
GET /api/analises?ticker=PETR4
GET /api/analises?page=0&size=10&sort=criadoEm,desc
```

### GET /api/metricas/ia — Metricas da IA

Resumo consolidado das metricas de chamadas ao Ollama.

### Swagger UI

```
http://localhost:8084/swagger-ui.html
```

### Actuator

- `GET /actuator/health` — status da aplicacao (PostgreSQL, disco)
- `GET /actuator/metrics` — lista de metricas

---

## Conceito: Cache de cotacoes

O cache Caffeine e aplicado no `BrapiCotacaoExternaGatewayImpl`, usando o **ticker** como chave:

```java
@Cacheable(value = "cotacoes", key = "#ticker.toUpperCase()")
public Optional<CotacaoResumo> buscarCotacao(String ticker) { ... }
```

**Por que cachear a cotacao e nao a analise?**

- **Cotacao** — o mesmo ticker consultado multiplas vezes num curto periodo retorna o mesmo preco.
  Cache de 15 min evita chamadas redundantes a brapi.dev (API publica com rate limit).
- **Analise** — cada analise pode ter interpretacoes diferentes dependendo do contexto.
  Alem disso, o historico e o valor — guardar cada analise no banco e mais util que cache.

**Configuracao:**
- TTL: 900s (15 min) — `expireAfterWrite=900s`
- Max entries: 200

---

## Conceito: Tratamento de erros

O `GlobalExceptionHandler` retorna sempre o mesmo formato (`ErrorResponse`):

| Excecao | HTTP Status | Quando |
|---|---|---|
| `AnaliseNaoEncontradaException` | 404 | ID inexistente em GET /api/analises/{id} |
| `CotacaoIndisponivelException` | 422 | Ticker invalido ou brapi.dev fora do ar |
| `MethodArgumentNotValidException` | 400 | Body com campos invalidos |
| `RateLimitExceededException` | 429 | Limite de chamadas simultaneas excedido |
| `Exception` (generica) | 500 | Erro inesperado |

---

## Conceito: Testes

```
Tests run: 11, Failures: 0, Errors: 0
```

| Classe de teste | O que valida |
|---|---|
| `AnaliseServiceImplTest` (7) | Analise com sucesso, extracao de COMPRA/VENDA/MANTER/INDEFINIDO, cotacao indisponivel, busca por ID |
| `ComparacaoServiceImplTest` (3) | Comparacao de 2 e 3 acoes, ticker invalido na lista |

Executar:
```
./mvnw test
```

---

## Estrutura do projeto

```
src/main/java/com/rafaelperracini/investidorinteligente/
├── InvestidorInteligenteApplication.java     # Main (@EnableRetry, @EnableCaching)
├── config/
│   ├── DatabaseConfig.java                   # PostgreSQL embarcado
│   ├── OpenApiConfig.java                    # Swagger/OpenAPI info
│   └── RateLimiterConfig.java                # Rate limiter via Semaphore
├── controller/
│   ├── AnaliseController.java               # POST/GET analises + Swagger
│   ├── ComparacaoController.java            # POST comparacoes + Swagger
│   └── MetricasController.java              # GET metricas da IA
├── dto/
│   ├── AnaliseRequest.java                  # Input (record + @Schema + @Valid + regex)
│   ├── AnaliseResponse.java                 # Resultado da analise
│   ├── ComparacaoRequest.java               # Input (lista de tickers + @Size)
│   ├── ComparacaoResponse.java              # Resultado da comparacao
│   ├── CotacaoResumo.java                   # Dados de cotacao (usado em ambos)
│   └── ErrorResponse.java                   # Resposta padronizada de erro
├── entity/
│   ├── AnaliseEntity.java                   # JPA entity (tabela "analises")
│   └── TipoRecomendacao.java               # Enum (COMPRA, VENDA, MANTER, INDEFINIDO)
├── exception/
│   ├── AnaliseNaoEncontradaException.java   # 404
│   ├── CotacaoIndisponivelException.java    # 422
│   └── GlobalExceptionHandler.java          # @RestControllerAdvice
├── gateway/
│   ├── CotacaoExternaGateway.java           # Interface — brapi.dev
│   ├── OllamaGateway.java                   # Interface — LLM
│   └── impl/
│       ├── BrapiCotacaoExternaGatewayImpl.java  # Impl — RestClient + cache
│       └── OllamaGatewayImpl.java           # Impl — ChatClient + retry + metricas
├── repository/
│   └── AnaliseRepository.java               # Spring Data JPA (queries paginadas)
└── service/
    ├── AnaliseService.java                   # Interface — analise e historico
    ├── ComparacaoService.java               # Interface — comparacao entre acoes
    └── impl/
        ├── AnaliseServiceImpl.java           # Busca cotacao + IA + persiste
        └── ComparacaoServiceImpl.java        # Busca N cotacoes + IA compara
```

## Arquitetura (SOLID)

- **Controller** — apenas HTTP, delega ao Service. Anotacoes OpenAPI.
- **Service (AnaliseService)** — orquestra: busca cotacao via gateway, envia para IA, persiste resultado.
- **Service (ComparacaoService)** — busca N cotacoes, monta prompt comparativo, retorna parecer.
- **Gateway (CotacaoExternaGateway)** — encapsula chamadas a brapi.dev com cache.
- **Gateway (OllamaGateway)** — encapsula chamadas ao LLM com retry, metricas e rate limiting.
- **Repository** — Spring Data JPA com queries derivadas e paginacao.
- **Entity** — JPA entity com `TipoRecomendacao` enum.
- **DTOs** — records imutaveis com `@Schema`, `@Valid`, `@Pattern`.
- **Exception** — exceptions de negocio + handler global.

---

## Exemplos de teste

**Analisar PETR4:**
```bash
curl -X POST http://localhost:8084/api/analises \
  -H "Content-Type: application/json" \
  -d '{"ticker": "PETR4"}'
```

**Analisar VALE3:**
```bash
curl -X POST http://localhost:8084/api/analises \
  -H "Content-Type: application/json" \
  -d '{"ticker": "VALE3"}'
```

**Comparar PETR4 vs VALE3 vs ITUB4:**
```bash
curl -X POST http://localhost:8084/api/comparacoes \
  -H "Content-Type: application/json" \
  -d '{"tickers": ["PETR4", "VALE3", "ITUB4"]}'
```

**Historico de analises da PETR4:**
```bash
curl "http://localhost:8084/api/analises?ticker=PETR4"
```

**Buscar analise especifica:**
```bash
curl http://localhost:8084/api/analises/1
```
