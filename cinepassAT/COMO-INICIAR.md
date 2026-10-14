# Como iniciar o ambiente do Cinepass

## Componentes

| Componente | Porta | Como sobe |
|---|---|---|
| Eureka Server (discovery-server) | 8761 | IntelliJ |
| API Gateway | 8080 | IntelliJ |
| reserva-service | 8081 | IntelliJ |
| pagamento-service | 8082 | IntelliJ |
| ingresso-service | 8083 | IntelliJ |
| notificacao-service | 8084 | IntelliJ |
| reputacao-service | 8085 | IntelliJ |
| auditoria-service | 8086 | IntelliJ |
| Apache Kafka | 9092 | Docker Compose |
| Kafka UI | 8090 | Docker Compose |
| PostgreSQL | 25432 | Docker Compose |
| Temporal (servidor e UI) | 7233 e 28081 | Docker Compose |
| Loki (logs) | 3100 | Docker Compose |
| Grafana (consulta de logs) | 3000 | Docker Compose |
| Zipkin (rastreamento) | 9411 | Docker Compose |

## Bancos de dados (um por microsserviço)

| Serviço | Banco |
|---|---|
| reserva-service | cinepass_db |
| pagamento-service | pagamento_db |
| ingresso-service | ingresso_db |
| auditoria-service | auditoria_db |
| notificacao-service | notificacao_db |
| reputacao-service | reputacao_db |

Os bancos são criados pelo arquivo `infra/postgres/init.sql` na primeira vez que o volume do PostgreSQL é criado. Se o volume já existia, crie os que faltam:

```
docker exec cinepass-postgres psql -U cinepass -d postgres -c "CREATE DATABASE auditoria_db;"
docker exec cinepass-postgres psql -U cinepass -d postgres -c "CREATE DATABASE notificacao_db;"
docker exec cinepass-postgres psql -U cinepass -d postgres -c "CREATE DATABASE reputacao_db;"
```

## Passo a passo

Todos os comandos são executados na raiz do projeto.

1. PostgreSQL e Temporal:

```
docker compose -f docker-compose-temporal.yaml up -d
```

2. Kafka e Kafka UI:

```
docker compose up -d zookeeper kafka kafka-ui
```

3. Observabilidade (Loki, Promtail, Grafana e Zipkin):

```
docker compose -f docker-compose-logs.yaml up -d
```

4. Conferir que os containers estão ativos:

```
docker ps
```

5. Subir os microsserviços no IntelliJ (aba Services), esperando "Started" em cada um, nesta ordem:
    1. discovery-server
    2. pagamento-service
    3. ingresso-service
    4. reserva-service
    5. auditoria-service, notificacao-service e reputacao-service
    6. api-gateway

## Endereços úteis

| O que | Endereço |
|---|---|
| Criar reserva (pelo gateway) | POST http://localhost:8080/api/reservas |
| Eureka | http://localhost:8761 |
| Kafka UI | http://localhost:8090 |
| Temporal UI | http://localhost:28081 |
| Grafana (logs centralizados) | http://localhost:3000 |
| Zipkin (rastreamento) | http://localhost:9411 |

## Parar o ambiente

Pare os serviços no IntelliJ e rode:

```
docker compose -f docker-compose-logs.yaml down
docker compose down
docker compose -f docker-compose-temporal.yaml down
```