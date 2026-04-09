# Investidor Inteligente

[![CI](https://github.com/perracini/investidor-inteligente/actions/workflows/ci.yml/badge.svg)](https://github.com/perracini/investidor-inteligente/actions/workflows/ci.yml)

> **Projeto de portfolio** — arquitetura profissional com SOLID, testes, observabilidade,
> resiliencia e documentacao OpenAPI. Desenvolvido para demonstrar dominio em integracao
> Java + IA + APIs externas em cenario realista de investimentos.

API de analise fundamentalista de acoes brasileiras com IA. Busca dados em tempo real
na brapi.dev, envia para IA local (Ollama/Llama 3.2) analisar e emite pareceres de
investimento com recomendacao (COMPRA / VENDA / MANTER).

Tambem permite **comparar** multiplas acoes lado a lado com ranking de preferencia.

**Sem Docker por padrao.** PostgreSQL sobe automaticamente com a aplicacao
(embarcado via `io.zonky.test:embedded-postgres`). Se preferir rodar containerizado,
existe `Dockerfile` + `docker-compose.yml` — veja a secao [Docker (opcional)](#docker-opcional)
mais abaixo.

---

## Demo

### Swagger UI

![Swagger UI](docs/screenshots/swagger.png)

> _Para gerar: suba a aplicacao com `./mvnw spring-boot:run`, abra
> `http://localhost:8084/swagger-ui.html` e capture a tela. Salve em
> `docs/screenshots/swagger.png`._

### Fluxo completo via curl

```bash
# 1) Cria analise da PETR4 (busca cotacao real + chama IA + persiste)
curl -X POST http://localhost:8084/api/analises \
  -H "Content-Type: application/json" \
  -d '{"ticker": "PETR4"}'

# Resposta:
# {
#   "id": 1,
#   "ticker": "PETR4",
#   "empresa": "Petroleo Brasileiro SA Pfd",
#   "preco": 46.46,
#   "variacao": -4.23,
#   "dividendYield": 0.0,
#   "pl": 5.44,
#   "recomendacao": "COMPRA",
#   "parecer": "O preco atual da PETR4 esta abaixo do P/L historico...",
#   "criadoEm": "2026-04-08T16:29:25"
# }

# 2) Busca a analise persistida por ID
curl http://localhost:8084/api/analises/1

# 3) Lista historico filtrando por ticker (paginado)
curl "http://localhost:8084/api/analises?ticker=PETR4&page=0&size=10"
```

![Demo GIF](docs/screenshots/demo.gif)

> _Para gerar o GIF: use [ScreenToGif](https://www.screentogif.com/) (Windows) ou
> similar, grave o ciclo POST -> GET acima e salve em `docs/screenshots/demo.gif`._

---

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

## Conceito: Contextos de transacao

O metodo `analisar()` do `AnaliseServiceImpl` **nao e `@Transactional` no escopo do
metodo inteiro**. A chamada ao Ollama leva 10-20s e manter uma transacao aberta
durante I/O externo segura connection do pool e mata a escalabilidade sob carga
(10 analises paralelas esgotariam o HikariCP default).

A solucao: como o unico acesso ao banco e o `repository.save()` ao final,
deixamos o `JpaRepository.save()` abrir sua propria transacao curta (o Spring Data
ja anota `SimpleJpaRepository` como `@Transactional`). Resultado: a TX dura
milissegundos em vez de dezenas de segundos, e a IA roda fora de qualquer contexto
transacional.

```
[busca externa brapi.dev]   sem TX
[chamada Ollama 10-20s]     sem TX — NAO segura connection do pool
[repository.save()]         TX curta do Spring Data (commit implicito)
```

**Se o metodo precisar de mais de uma operacao de banco** (ex.: salvar historico +
atualizar agregados), a forma correta e usar `TransactionTemplate` para agrupar
*apenas* os acessos ao banco em uma TX curta, mantendo a chamada a IA fora da
transacao. O padrao esta documentado no `suporte-inteligente` (que tem o cenario
mais complexo de dois contextos transacionais separados por I/O).

**Metodos de leitura** (`buscarPorId`, `listarPorTicker`, `listarTodas`) sao
marcados como `@Transactional(readOnly = true)` para otimizar sessoes de leitura
do Hibernate.

---

## Conceito: Testes unitarios + integracao

O projeto tem **duas camadas de teste** com propositos distintos e complementares:

```
Tests run: 15, Failures: 0, Errors: 0
```

### Testes unitarios (11 testes — `service/impl/*Test.java`)

Isolam cada classe de seus colaboradores via Mockito. Rapidos (milissegundos),
nao sobem Spring, nao tocam Postgres.

| Classe de teste | O que valida |
|---|---|
| `AnaliseServiceImplTest` (7) | Analise com sucesso, extracao de COMPRA/VENDA/MANTER/INDEFINIDO, cotacao indisponivel, busca por ID |
| `ComparacaoServiceImplTest` (3) | Comparacao de 2 e 3 acoes, ticker invalido na lista |

### Teste de integracao (4 testes — `integration/AnaliseFluxoCompletoIntegrationTest`)

Sobe o contexto Spring completo (`@SpringBootTest` + `MockMvc`) com Postgres
embutido. Mocka apenas a `CotacaoExternaGateway` (brapi.dev) e o `OllamaGateway`
via `@MockitoBean` — nao depende de rede nem do Ollama local no CI. Valida o
fluxo real ponta a ponta:

```
POST /api/analises -> HTTP 200
   -> AnaliseService busca cotacao (mock)
   -> chama Ollama (mock)
   -> persiste no Postgres embutido
   -> retorna parecer
GET /api/analises/{id} -> HTTP 200 com a analise persistida
GET /api/analises?ticker=PETR4 -> lista paginada com a analise recem-criada
```

| Cenario | O que valida |
|---|---|
| `deveAnalisarEConsultarAcaoComSucesso` | Fluxo completo POST -> persistencia -> GET por id -> GET paginado com filtro |
| `deveRetornar422QuandoCotacaoIndisponivel` | Excecao de negocio mapeada para 422 pelo `GlobalExceptionHandler` |
| `deveRetornar400QuandoTickerInvalido` | Bean Validation do regex `^[A-Za-z]{4}\d{1,2}$` |
| `deveRetornar404QuandoAnaliseInexistente` | `AnaliseNaoEncontradaException` -> 404 |

### Unitarios vs. integracao — comparativo

| Criterio | Unitario | Integracao |
|---|---|---|
| **Velocidade** | ~50ms por teste | ~7s por teste (setup do contexto) |
| **Sobe Spring?** | Nao | Sim (`@SpringBootTest`) |
| **Toca Postgres?** | Nao (repository mockado) | Sim (embedded) |
| **Mocka IA / brapi.dev?** | Sim (gateways Mockito) | Sim (`@MockitoBean`) |
| **O que valida bem** | Logica de extracao, branching, exceptions, edge cases | Serializacao JSON, Bean Validation, handlers globais, paginacao real, queries JPA |
| **O que *nao* valida** | Integracao entre camadas, wiring Spring, queries JPA | Casos extremos unitarios (custaria caro rodar um contexto por caso) |
| **Quando quebra** | Mudou logica da classe | Mudou contrato entre camadas ou configuracao Spring |

As duas camadas se complementam: unitarios cobrem logica interna com velocidade,
integracao cobre o wiring e os contratos entre componentes reais. **Nenhuma das duas
sozinha e suficiente** — unitarios podem passar enquanto o sistema nao sobe (ex.:
bean nao encontrado, query JPA invalida), e integracao e caro demais para cobrir
todos os ramos de um metodo.

Executar:
```
./mvnw test                                                # tudo
./mvnw test -Dtest='*ServiceImplTest'                      # so unitarios
./mvnw test -Dtest='AnaliseFluxoCompletoIntegrationTest'   # so integracao
```

---

## CI/CD (GitHub Actions)

O projeto tem um pipeline minimalista em [.github/workflows/ci.yml](.github/workflows/ci.yml)
que roda a cada push e pull request contra `master`:

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      - run: ./mvnw --batch-mode --no-transfer-progress test
```

**O que esse pipeline garante:**

1. **Build reprodutivel** — JDK 17 Temurin (mesma versao que o `pom.xml` exige).
2. **Todos os testes verdes** — unitarios + integracao (incluindo o de Postgres embutido).
3. **Cache de dependencias** — `~/.m2/repository` reaproveitado entre runs para builds rapidos.
4. **Artefatos em caso de falha** — `target/surefire-reports/` publicados como artifact por 7 dias para debug.

O badge no topo do README reflete o estado da ultima run na `master`. PR que
quebrar o pipeline fica bloqueado ate corrigir.

**Por que so `test` e nao deploy?**
Este e um projeto de portfolio — nao ha ambiente de staging/producao para
publicar. O pipeline demonstra o raciocinio correto (fail-fast em PR, build
reprodutivel, artefatos em caso de falha), que e o que um revisor tecnico
espera ver. Para adicionar deploy, bastaria um job `deploy` dependente de
`test` com `needs: [test]`.

---

## Docker (opcional)

Embora o projeto rode sem Docker por padrao, existem um `Dockerfile` e um
`docker-compose.yml` opcionais para quem preferir isolamento via container:

```bash
# Build e run via compose (app + Postgres embutido no mesmo container)
docker compose up app

# Ou direto via docker
docker build -t investidor-inteligente .
docker run -p 8084:8084 investidor-inteligente
```

**Detalhes de implementacao:**

- **Multi-stage build** — stage 1 (`maven:3.9-eclipse-temurin-17`) constroi o jar,
  stage 2 (`eclipse-temurin:17-jre-alpine`) contem apenas o runtime. Imagem final
  fica em ~200MB em vez de ~700MB.
- **Cache de dependencias** — `pom.xml` copiado e `dependency:go-offline` rodado
  antes do codigo fonte, para que mudancas em `src/` nao invalidem o cache do Maven.
- **Usuario nao-root** — runtime roda como user `app` (nao-root) por seguranca.
- **Portas expostas** — 8084 (HTTP/API/Swagger).

**Por que embarcado e nao servicos separados por default?**
A escolha foi **deliberada**: o projeto e auto-contido para fins de portfolio e
estudo. Quem avalia nao precisa configurar Postgres nem infraestrutura — um
`./mvnw spring-boot:run` e suficiente. O `docker-compose.yml` tem a "Opcao B"
comentada mostrando como desacoplar em servicos separados (Postgres em container
proprio) para producao real. A intencao e deixar claro que a escolha foi
consciente e que a aplicacao pode ser containerizada "de verdade" com mudancas
minimas.

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
