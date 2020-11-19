# Pautas API

## Arquitetura da Solução

### Componentes da solução:

- `pautas-api` (esse projeto)
- `pautas-worker` - https://github.com/tonisantes/pautas-worker

Decidi criar o componente `pautas-worker` pois achei por bem realizar o processamento dos votos de forma assincrona. O maior motivador dessa decisão foi o fato de em cada voto ser necessário consultar um sistema externo para validação do CPF do associado (como sugerido pelas tarefas bônus do desafio).

Como não tenho controle da estabilidade de um sistema externo, consiro sensato manter o processamento assincrono. A ideia é apenas utilizar a API para registrar a "intenção de voto" do associado, e então despachar uma mensagem para que o `pautas-worker` entre em ação na tentativa de processar o voto.

Também utilizo o `pautas-worker` para verificar de tempos em tempos se uma sessão de uma pauta já encerrou, ou se foi concluída, ou se ainda está aguardando o processamento dos votos.

Mais detalhes sobre minhas escolhas nos comentários das classes `PautaController.java` e `VotoController.java`.  

### Breve resumo das entidades escolhidas:

- `Pauta` - Representa uma pauta
- `Voto` - Representa um voto único de algum associado

A entidade `Pauta` possui os seguintes possíveis status:

- `CRIADA`
- `SESSAO_ABERTA`
- `SESSAO_FECHADA`
- `CONCLUIDA`

A entidade `Voto` possui os seguintes possíveis status:

- `PENDENTE`
- `CONTABILIZADO`
- `REJEITADO`
- `ERRO`

## Pré-requisitos para rodar a API

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
