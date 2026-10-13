# CinePass — Base para ensino de Saga Pattern

Projeto didático para evoluir de uma arquitetura síncrona baseada em DDD para Saga Pattern.

A versão inicial **não implementa Saga, Kafka nem Outbox de propósito**. O objetivo é criar primeiro uma inconsistência distribuída observável e, nas aulas seguintes, evoluir a solução.

## Stack

- Java 21
- Spring Boot 4.1.1
- Spring Cloud 2025.1.3
- Spring Cloud Netflix Eureka
- Spring Cloud Gateway Server WebFlux
- Spring Cloud LoadBalancer
- Spring Data JPA
- PostgreSQL 17
- Docker Compose

> O release train Spring Cloud 2025.1.x suporta Spring Boot 4.1.x e o release 2025.1.3 é compatível com Spring Boot 4.1.x.

---

## Arquitetura inicial

```text
                           CLIENTE
                              |
                              v
                     +----------------+
                     |   API GATEWAY  |
                     |     :8080      |
                     +-------+--------+
                             |
                   Service Discovery
                             |
                      +------v------+
                      |   EUREKA    |
                      |    :8761    |
                      +-------------+

          +------------------+-------------------+
          |                  |                   |
          v                  v                   v
 +----------------+  +----------------+  +----------------+
 | RESERVA        |  | PAGAMENTO      |  | INGRESSO       |
 | SERVICE :8081  |  | SERVICE :8082  |  | SERVICE :8083  |
 +----------------+  +----------------+  +----------------+
 | Filme          |  | Pagamento      |  | Ingresso       |
 | Sessao         |  | Cobranca       |  | Emissao        |
 | Assento        |  | Estorno        |  | Cancelamento   |
 | Reserva        |  |                |  |                |
 +-------+--------+  +-------+--------+  +-------+--------+
         |                   |                   |
         v                   v                   v
   cinepass_db        pagamento_db        ingresso_db
```

O Gateway é a porta de entrada externa. O `reserva-service` usa Eureka + Spring Cloud LoadBalancer para chamar os serviços internos diretamente pelo nome lógico:

```text
http://PAGAMENTO-SERVICE
http://INGRESSO-SERVICE
```

Isso evita transformar o API Gateway em um intermediário obrigatório para chamadas internas.

---

## Estrutura

```text
cinepass-saga-base/
|
|-- discovery-server/
|-- api-gateway/
|-- reserva-service/
|-- pagamento-service/
|-- ingresso-service/
|-- infra/postgres/init.sql
|-- requests/cinepass.http
|-- docker-compose.yml
`-- pom.xml
```

O `pom.xml` da raiz é apenas um **agregador de build**. Os serviços não compartilham classes de domínio nem dependências entre si.

---

# DDD no projeto

## reserva-service

É o monólito de domínio inicial. Ele concentra:

- Filme
- Sessão
- Assentos
- Reserva

O Aggregate principal é `Reserva`.

```text
Reserva
 |
 |-- ReservaId
 |-- ClienteId
 |-- SessaoId
 |-- AssentoId
 |-- Dinheiro
 |-- StatusReserva
 |-- pagamentoId
 `-- ingressoId
```

Estados:

```text
CRIADA
  |
  v
AGUARDANDO_PAGAMENTO
  |
  v
PAGAMENTO_APROVADO
  |
  v
EMITINDO_INGRESSO
  |
  v
CONFIRMADA
```

O modelo de domínio não depende de JPA. As entidades JPA ficam em `infrastructure.persistence`.

## pagamento-service

Aggregate:

```text
Pagamento
 |
 |-- PagamentoId
 |-- ReservaId
 |-- Dinheiro
 `-- StatusPagamento
```

Estados:

```text
PENDENTE -> APROVADO -> ESTORNADO
        \
         -> RECUSADO
```

O método `estornar()` já existe porque será usado depois como **ação compensatória da Saga**.

## ingresso-service

Aggregate:

```text
Ingresso
 |
 |-- IngressoId
 |-- ReservaId
 |-- SessaoId
 |-- CodigoIngresso
 |-- Assentos
 `-- StatusIngresso
```

Também existe `cancelar()`, que poderá participar de futuras compensações.

---

# O problema didático proposital

O caso de uso `ReservaApplicationService.realizar()` usa uma transação local:

```java
@Transactional
public ReservaDetalhe realizar(...) {
    // reserva assentos no banco local

    // cria reserva no banco local

    pagamentoGateway.cobrar(...); // outro processo + outro banco

    ingressoGateway.emitir(...);  // outro processo + outro banco

    // confirma reserva local
}
```

A anotação `@Transactional` protege apenas `cinepass_db`.

Ela **não** controla as transações dos outros serviços.

## Cenário de falha

```text
1. Reserva assento                 OK
2. Cria Reserva                    OK
3. pagamento-service cobra         OK + COMMIT remoto
4. ingresso-service falha          ERRO
5. reserva-service lança exception
6. cinepass_db faz ROLLBACK
```

Resultado:

```text
cinepass_db
  Reserva: inexistente
  Assento: disponível novamente

pagamento_db
  Pagamento: APROVADO

ingresso_db
  Ingresso: inexistente
```

Esse é o problema que será resolvido posteriormente com Saga.

---

# Como executar

## Opção 1 — tudo com Docker Compose

Na raiz:

```bash
docker compose up --build
```

Serviços:

| Componente | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Eureka | http://localhost:8761 |
| Reserva Service | http://localhost:8081 |
| Pagamento Service | http://localhost:8082 |
| Ingresso Service | http://localhost:8083 |
| PostgreSQL | localhost:5432 |

Na primeira execução o PostgreSQL cria:

```text
cinepass_db
pagamento_db
ingresso_db
```

Para zerar completamente os bancos:

```bash
docker compose down -v
```

Depois:

```bash
docker compose up --build
```

## Opção 2 — infraestrutura em Docker e aplicações na IDE

Suba somente o Postgres:

```bash
docker compose up postgres
```

Depois execute, nesta ordem:

```text
DiscoveryServerApplication
PagamentoServiceApplication
IngressoServiceApplication
ReservaServiceApplication
ApiGatewayApplication
```

---

# Dados iniciais

O `reserva-service` cria automaticamente um filme e uma sessão.

Filme fixo:

```text
11111111-1111-1111-1111-111111111111
```

Sessão fixa:

```text
22222222-2222-2222-2222-222222222222
```

Assentos iniciais:

```text
A1 A2 A3 A4 A5
B1 B2 B3 B4 B5
```

Preço por assento:

```text
R$ 39,90
```

---

# Endpoints pelo Gateway

## Filmes

```http
GET http://localhost:8080/api/filmes
```

## Sessões

```http
GET http://localhost:8080/api/sessoes
```

## Criar reserva

```http
POST http://localhost:8080/api/reservas
Content-Type: application/json

{
  "clienteId": "33333333-3333-3333-3333-333333333333",
  "sessaoId": "22222222-2222-2222-2222-222222222222",
  "assentos": ["A1", "A2"],
  "simularRecusaPagamento": false,
  "simularFalhaIngresso": false
}
```

## Consultar reserva

```http
GET http://localhost:8080/api/reservas/{reservaId}
```

## Consultar pagamento

```http
GET http://localhost:8080/api/pagamentos/{pagamentoId}
```

## Consultar pagamento pela reserva

```http
GET http://localhost:8080/api/pagamentos/reserva/{reservaId}
```

## Estornar pagamento

```http
POST http://localhost:8080/api/pagamentos/{pagamentoId}/estorno
```

## Consultar ingresso pela reserva

```http
GET http://localhost:8080/api/ingressos/reserva/{reservaId}
```

---

# Demonstração 1 — caminho feliz

```http
POST http://localhost:8080/api/reservas
Content-Type: application/json

{
  "clienteId": "33333333-3333-3333-3333-333333333333",
  "sessaoId": "22222222-2222-2222-2222-222222222222",
  "assentos": ["A1"],
  "simularRecusaPagamento": false,
  "simularFalhaIngresso": false
}
```

Resultado esperado:

```text
Reserva    CONFIRMADA
Pagamento APROVADO
Ingresso   EMITIDO
```

---

# Demonstração 2 — pagamento recusado

Use outro assento:

```http
POST http://localhost:8080/api/reservas
Content-Type: application/json

{
  "clienteId": "33333333-3333-3333-3333-333333333333",
  "sessaoId": "22222222-2222-2222-2222-222222222222",
  "assentos": ["A2"],
  "simularRecusaPagamento": true,
  "simularFalhaIngresso": false
}
```

O pagamento será registrado como `RECUSADO` no serviço de pagamento e a transação local do `reserva-service` será revertida.

---

# Demonstração 3 — a falha que introduz Saga

Use outro assento:

```http
POST http://localhost:8080/api/reservas
Content-Type: application/json

{
  "clienteId": "33333333-3333-3333-3333-333333333333",
  "sessaoId": "22222222-2222-2222-2222-222222222222",
  "assentos": ["A3"],
  "simularRecusaPagamento": false,
  "simularFalhaIngresso": true
}
```

A resposta será um erro semelhante a:

```json
{
  "title": "Inconsistência distribuída proposital",
  "status": 500,
  "detail": "O pagamento foi aprovado, mas a emissão do ingresso falhou...",
  "reservaId": "...",
  "pagamentoId": "...",
  "proximoPassoDaAula": "Implementar compensação com Saga Pattern"
}
```

Copie o `pagamentoId` e consulte:

```http
GET http://localhost:8080/api/pagamentos/{pagamentoId}
```

Você verá:

```json
{
  "status": "APROVADO"
}
```

Agora consulte a Reserva usando o `reservaId` retornado no erro:

```http
GET http://localhost:8080/api/reservas/{reservaId}
```

Resultado:

```text
404 Not Found
```

Isso demonstra que:

```text
@Transactional != transação distribuída
```

---

# Evolução planejada para as próximas aulas

## Etapa 1 — compensação síncrona manual

```text
Cobrar
  |
  v
Emitir ingresso
  X
  |
  v
Estornar pagamento
```

## Etapa 2 — Kafka

Trocar parte da comunicação síncrona por eventos.

## Etapa 3 — Transactional Outbox

Resolver:

```text
Banco + Kafka
```

## Etapa 4 — Saga Choreography

```text
ReservaCriada
      |
      v
Pagamento
      |
      v
PagamentoAprovado
      |
      v
Ingresso
```

## Etapa 5 — Saga Orchestration

```text
             ReservaSaga
                 |
       +---------+---------+
       |                   |
       v                   v
   Pagamento            Ingresso
```

Depois podem ser adicionados:

- Saga ID
- Commands e Events
- compensações
- idempotência
- retries
- timeouts
- DLT
- observabilidade
- Outbox Pattern

---

# Observação arquitetural importante

Nesta primeira versão, a transação local permanece aberta durante chamadas HTTP externas. Isso é **intencionalmente inadequado para produção** e existe para tornar o problema pedagógico muito claro.

A evolução para Saga irá remover a necessidade de fingir que uma transação ACID local consegue controlar todo o processo distribuído.
