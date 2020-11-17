package desafiosicredi.pautasapi;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import desafiosicredi.pautasapi.dto.PautaDTO;
import desafiosicredi.pautasapi.dto.StatusPautaDTO;
import desafiosicredi.pautasapi.model.StatusPauta;
import desafiosicredi.pautasapi.repositories.PautaRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class HttpRequestTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PautaRepository pautaRepository;
    
    @Test
	public void criarPauta() throws Exception {
        PautaDTO payload = new PautaDTO();
        payload.setNome("teste");
		ResponseEntity<StatusPautaDTO> result = this.restTemplate.postForEntity("http://localhost:" + port + "/pautas", payload, StatusPautaDTO.class);
        Assertions.assertThat(result.getStatusCodeValue()).isEqualTo(201);
        Assertions.assertThat(result.getBody().getId()).isNotNull();
        Assertions.assertThat(result.getBody().getStatus()).isEqualTo(StatusPauta.CRIADA);
        Assertions.assertThat(pautaRepository.findById(result.getBody().getId())).isNotNull();
	}
}
