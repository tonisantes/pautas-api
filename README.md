# Pautas API

## Pré-requisitos

Tenologias utilizadas:
- `JAVA - openjdk version "11.0.9" 2020-10-20`
- `Apache Maven 3.6.0`
- `RabbitMQ 3.7.15`
- `Postgres`

Comando que utilizei para rodar o RabbitMQ via docker.

```bash
docker run --rm --hostname localhost --name rabbit-test -p 15672:15672 -p 5672:5672 rabbitmq:3.7.15-management
```

Comando que utilizei para rodar o Postgres via docker (já com o usuário, senha e database utilizados na solução).

```bash
docker run --rm --hostname localhost --name postgres-test -p 5432:5432 -e POSTGRES_USER='root' -e POSTGRES_PASSWORD='123456' -e POSTGRES_DB='pautas' postgres
```

## Rodar a API

```bash
mvn spring-boot:run
```

## Documentação da API

http://localhost:8080/swagger-ui.html
