import eventlet
eventlet.monkey_patch()

import random
import requests


def gerar_cpf():
    def calcula_digito(digs):
       s = 0
       qtd = len(digs)
       for i in range(qtd):
          s += n[i] * (1+qtd-i)
       res = 11 - s % 11
       if res >= 10: return 0
       return res                                                                              
    n = [random.randrange(10) for i in range(9)]
    n.append(calcula_digito(n))
    n.append(calcula_digito(n))
    return "%d%d%d%d%d%d%d%d%d%d%d" % tuple(n)


class PaustasTesteIntegrado():
    def __init__(self, api_url="http://localhost:8080/v1", duracao_sessao=1, total_votos=10000):
        self._api_url = api_url
        self._duracao_sessao = duracao_sessao
        self._total_votos = total_votos
        self._pool = eventlet.GreenPool(size=50)

    def _vote(self, pauta_id):
        cpf = gerar_cpf()
        voto = random.choice(["SIM", "NAO"])
        url = "{}/pautas/{}/votar".format(self._api_url, pauta_id)
        print("votando..")
        response = requests.post(url, json={"cpfAssociado": cpf, "voto": voto})
        response.raise_for_status()

        print("%s -> %s" % (cpf, voto))

    def execute(self):
        url = "{}/pautas".format(self._api_url)
        response = requests.post(url, json={"nome": "Teste de Carga"})
        response.raise_for_status()
        pauta = response.json()

        url = "{}/pautas/{}/abrir-sessao".format(self._api_url, pauta["id"])
        response = requests.put(url, json={"duracao": self._duracao_sessao})
        response.raise_for_status()

        for _ in range(self._total_votos):
            self._pool.spawn_n(self._vote, pauta["id"])

        self._pool.waitall()

if __name__ == "__main__":
    PaustasTesteIntegrado().execute()
