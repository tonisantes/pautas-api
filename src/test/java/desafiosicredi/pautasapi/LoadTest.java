package desafiosicredi.pautasapi;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonFormat.Feature;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Timed;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import desafiosicredi.pautasapi.dto.AbrirSessaoDTO;
import desafiosicredi.pautasapi.dto.PautaDTO;
import desafiosicredi.pautasapi.dto.StatusPautaDTO;
import desafiosicredi.pautasapi.dto.StatusVotoDTO;
import desafiosicredi.pautasapi.dto.VotoDTO;
import desafiosicredi.pautasapi.model.Pauta;
import desafiosicredi.pautasapi.model.StatusPauta;
import desafiosicredi.pautasapi.model.StatusVoto;
import desafiosicredi.pautasapi.model.TipoVoto;
import desafiosicredi.pautasapi.repositories.PautaRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class LoadTest {

    private static final Logger log = LoggerFactory.getLogger(LoadTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @Timeout(70)
    public void test() throws Exception {
        PautaDTO pautaDto = new PautaDTO();
        pautaDto.setNome("teste carga");
        ResponseEntity<StatusPautaDTO> pautaResponse =
            this.restTemplate.postForEntity("http://localhost:" + port + "/pautas",
            pautaDto, StatusPautaDTO.class);

        Assertions.assertThat(pautaResponse.getStatusCodeValue()).isEqualTo(201);
        
        AbrirSessaoDTO abrirSessaoDTO = new AbrirSessaoDTO();
        abrirSessaoDTO.setDuracao(2);
        this.restTemplate.put("http://localhost:" + port + "/pautas/" + pautaResponse.getBody().getId() + "/abrir",
            abrirSessaoDTO);

        final Pauta pauta = pautaRepository.findById(pautaResponse.getBody().getId()).orElse(null);
        Assertions.assertThat(pauta).isNotNull();
        Assertions.assertThat(pauta.getStatus()).isEqualTo(StatusPauta.SESSAO_ABERTA);

        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(12);

        final int[] pendentes = new int[]{0};
        final int[] rejeitados = new int[]{0};

        int totalVotos = 10000;

        for (int i = 0; i < totalVotos; i++) {
            futures.add(executor.submit(new Callable<ResponseEntity<String>>(){
                @Override
                public ResponseEntity<String> call() throws Exception {
                    VotoDTO payload = new VotoDTO();
                    try {
                        payload.setCpfAssociado(gerarCPF());
                    } catch (Exception ex) {
                        return null;
                    }
    
                    payload.setVoto(TipoVoto.NAO);
                    ResponseEntity<String> response = restTemplate
                            .postForEntity("http://localhost:" + port + "/pautas/" + pauta.getId() +"/votar", payload, String.class)
                            ;
    
                    Assertions.assertThat(response.getStatusCodeValue()).isIn(List.of(200, 412));

                    return response;
                }
            }));
        }

        for (Future<ResponseEntity<String>> promise : futures) {
            ResponseEntity<String> response = promise.get();
            if (response.getStatusCodeValue() == 200) pendentes[0]++;
            if (response.getStatusCodeValue() == 412) rejeitados[0]++;
        }

        transactionTemplate.execute(new TransactionCallback<Object>(){
            @Override
            public Object doInTransaction(TransactionStatus status) {
                Pauta pautaAtualizada = pautaRepository.findById(pautaResponse.getBody().getId()).orElse(null);

                Assertions.assertThat(pautaAtualizada.getVotos().stream().count()).isEqualTo(pendentes[0]);
                Assertions.assertThat(pautaAtualizada.getVotos().stream().filter(v -> v.getStatus() == StatusVoto.PENDENTE).count()).isEqualTo(pendentes[0]);
                return null;
            }
        });
       
       
    }

    public String gerarCPF() throws Exception {

        int digito1 = 0, digito2 = 0, resto = 0;
        String nDigResult;
        String numerosContatenados;
        String numeroGerado;

        Random numeroAleatorio = new Random();

        // numeros gerados
        int n1 = numeroAleatorio.nextInt(10);
        int n2 = numeroAleatorio.nextInt(10);
        int n3 = numeroAleatorio.nextInt(10);
        int n4 = numeroAleatorio.nextInt(10);
        int n5 = numeroAleatorio.nextInt(10);
        int n6 = numeroAleatorio.nextInt(10);
        int n7 = numeroAleatorio.nextInt(10);
        int n8 = numeroAleatorio.nextInt(10);
        int n9 = numeroAleatorio.nextInt(10);

        int soma = n9 * 2 + n8 * 3 + n7 * 4 + n6 * 5 + n5 * 6 + n4 * 7 + n3 * 8 + n2 * 9 + n1 * 10;

        int valor = (soma / 11) * 11;

        digito1 = soma - valor;

        // Primeiro resto da divisão por 11.
        resto = (digito1 % 11);

        if (digito1 < 2) {
            digito1 = 0;
        } else {
            digito1 = 11 - resto;
        }

        int soma2 = digito1 * 2 + n9 * 3 + n8 * 4 + n7 * 5 + n6 * 6 + n5 * 7 + n4 * 8 + n3 * 9 + n2 * 10 + n1 * 11;

        int valor2 = (soma2 / 11) * 11;

        digito2 = soma2 - valor2;

        // Primeiro resto da divisão por 11.
        resto = (digito2 % 11);

        if (digito2 < 2) {
            digito2 = 0;
        } else {
            digito2 = 11 - resto;
        }

        // Conctenando os numeros
        numerosContatenados = String.valueOf(n1) + String.valueOf(n2) + String.valueOf(n3) + String.valueOf(n4)
                + String.valueOf(n5) + String.valueOf(n6) + String.valueOf(n7) + String.valueOf(n8)
                + String.valueOf(n9);

        // Concatenando o primeiro resto com o segundo.
        nDigResult = String.valueOf(digito1) + String.valueOf(digito2);

        numeroGerado = numerosContatenados + nDigResult;

        return numeroGerado;
    }
}
