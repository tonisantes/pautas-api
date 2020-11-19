# Pautas API

## Arquitetura da Solução

### Componentes da solução:

- `pautas-api` (esse projeto)
- `pautas-worker` - https://github.com/tonisantes/pautas-worker

Decidi criar o componente `pautas-worker` pois achei por bem realizar o processamento dos votos de forma assincrona. O maior motivador dessa decisão foi o fato de em cada voto ser necessário consultar um sistema externo para validação do CPF do associado (como sugerido pelas tarefas bônus do desafio).

Como não tenho controle da estabilidade de um sistema externo, considero sensato manter o processamento assincrono. A ideia é apenas utilizar a API para registrar a "intenção de voto" do associado, e então despachar uma mensagem para que o `pautas-worker` entre em ação na tentativa de processar o voto.

Também utilizo o `pautas-worker` para verificar de tempos em tempos se uma sessão de uma pauta já encerrou, ou se foi concluída, ou se ainda está aguardando o processamento dos votos.

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

### Principais endpoints

- `POST /v1/pautas` - Cria uma pauta
- `PUT /v1/pautas/{id}/abrir-sessao` - Abre uma pauta para receber votações
- `POST /v1/pautas/{id}/votar` - Recebe uma "intenção de voto" (O CPF do associado e seu voto SIM ou NAO vao no payload)
- `GET /v1/pautas/{id}` - Busca uma pauta (aqui é possível ver o resultado da Pauta, que é o objetivo do desafio)

Mais detalhes sobre minhas escolhas nos comentários das classes `PautaController.java` e `VotoController.java`. 

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

## Teste de performance

Fiz um script em Python para criar uma pauta, abrir uma sessão e então disparar vários votos utilizando concorrência.

O fonte desse script está em https://github.com/tonisantes/pautas-test.

Para rodar o script primeiro é necessário inciar o `pautas-api` e o `pautas-worker` e então rodar o seguinte comando:

```bash
docker run --network=host tonisantes/pautas-test:1.0.0 --duracao_sessao=1 --total_votos=10000
```

## Observações

Tentei ao máximo cumprir as tarefas bônus.

Todos os testes que fiz utilizando o serviço https://user-info.herokuapp.com/users/{cpf} retornaram `UNABLE_TO_VOTE`.
