package desafiosicredi.pautasapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import desafiosicredi.pautasapi.dto.AbrirSessaoDTO;
import desafiosicredi.pautasapi.dto.PautaDTO;
import desafiosicredi.pautasapi.dto.StatusPautaDTO;
import desafiosicredi.pautasapi.dto.StatusVotoDTO;
import desafiosicredi.pautasapi.model.Pauta;
import desafiosicredi.pautasapi.model.StatusPauta;
import desafiosicredi.pautasapi.model.StatusVoto;
import desafiosicredi.pautasapi.model.TipoVoto;
import desafiosicredi.pautasapi.model.Voto;
import desafiosicredi.pautasapi.repositories.PautaRepository;
import desafiosicredi.pautasapi.repositories.VotoRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PautasApiApplicationTests {

	@LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
	private PautaRepository pautaRepository;

	@MockBean
	private VotoRepository votoRepository;
	
	@MockBean
	private RabbitTemplate rabbitTemplate;
    
    @Test
	public void criarPauta() throws Exception {
        PautaDTO payload = new PautaDTO();
        payload.setNome("teste");
		ResponseEntity<StatusPautaDTO> result = this.restTemplate.postForEntity("http://localhost:" + port + "/v1/pautas", payload, StatusPautaDTO.class);
        assertThat(result.getStatusCodeValue()).isEqualTo(201);
        assertThat(result.getBody().getStatus()).isEqualTo(StatusPauta.CRIADA);
	}

	@Test
	public void criarPautaSemNome() throws Exception {
        PautaDTO payload = new PautaDTO();
		ResponseEntity<String> result = this.restTemplate.postForEntity("http://localhost:" + port + "/v1/pautas", payload, String.class);
        assertThat(result.getStatusCodeValue()).isEqualTo(400);
	}

	@Test
	public void abrirSessao() throws Exception {
		Pauta pauta = new Pauta();
		pauta.setId(1);
		pauta.setNome("teste");
		when(pautaRepository.findById(1)).thenReturn(Optional.of(pauta));
		
		int duracao = 5;

		AbrirSessaoDTO payload = new AbrirSessaoDTO();
		payload.setDuracao(duracao);
		ResponseEntity<StatusPautaDTO> result = this.restTemplate.exchange("http://localhost:" + port + "/v1/pautas/1/abrir-sessao", HttpMethod.PUT, new HttpEntity<>(payload), StatusPautaDTO.class);
        assertThat(result.getStatusCodeValue()).isEqualTo(200);
		assertThat(result.getBody().getStatus()).isEqualTo(StatusPauta.SESSAO_ABERTA);
		assertThat(result.getBody().getInicio().until(result.getBody().getFim(), ChronoUnit.MINUTES)).isEqualTo(duracao);
	}

	@Test
	public void abrirSessaoSemInformarDuracao() throws Exception {
		Pauta pauta = new Pauta();
		pauta.setId(1);
		pauta.setNome("teste");
		when(pautaRepository.findById(1)).thenReturn(Optional.of(pauta));
		
		int duracaoPadrao = 1;

		AbrirSessaoDTO payload = new AbrirSessaoDTO();
		ResponseEntity<StatusPautaDTO> result = this.restTemplate.exchange("http://localhost:" + port + "/v1/pautas/1/abrir-sessao", HttpMethod.PUT, new HttpEntity<>(payload), StatusPautaDTO.class);
        assertThat(result.getStatusCodeValue()).isEqualTo(200);
		assertThat(result.getBody().getStatus()).isEqualTo(StatusPauta.SESSAO_ABERTA);
		assertThat(result.getBody().getInicio().until(result.getBody().getFim(), ChronoUnit.MINUTES)).isEqualTo(duracaoPadrao);
	}

	@Test
	public void abrirSessaoComPautaNaoEncontrada() throws Exception {		
		int duracao = 5;

		AbrirSessaoDTO payload = new AbrirSessaoDTO();
		payload.setDuracao(duracao);
		ResponseEntity<String> result = this.restTemplate.exchange("http://localhost:" + port + "/v1/pautas/1/abrir", HttpMethod.PUT, new HttpEntity<>(payload), String.class);
        assertThat(result.getStatusCodeValue()).isEqualTo(404);
	}

	@Test
	public void votar() throws Exception {		
		Pauta pauta = new Pauta();
		pauta.setId(1);
		pauta.setNome("teste");
		pauta.setStatus(StatusPauta.SESSAO_ABERTA);
		pauta.setInicio(LocalDateTime.now().minusSeconds(60));
		pauta.setFim(pauta.getInicio().plusSeconds(61));
		when(pautaRepository.findById(1)).thenReturn(Optional.of(pauta));

		StatusVotoDTO payload = new StatusVotoDTO();
		payload.setCpfAssociado("07290790901");
		payload.setVoto(TipoVoto.SIM);

		ResponseEntity<StatusVotoDTO> result = this.restTemplate.postForEntity("http://localhost:" + port + "/v1/pautas/1/votar", payload, StatusVotoDTO.class);
		assertThat(result.getStatusCodeValue()).isEqualTo(201);
		assertThat(result.getBody().getStatus()).isEqualTo(StatusVoto.PENDENTE);
	}

	@Test
	public void votarSessaoExpirada() throws Exception {		
		Pauta pauta = new Pauta();
		pauta.setId(1);
		pauta.setNome("teste");
		pauta.setStatus(StatusPauta.SESSAO_ABERTA);
		pauta.setInicio(LocalDateTime.now().minusSeconds(60));
		pauta.setFim(pauta.getInicio().plusSeconds(59));
		when(pautaRepository.findById(1)).thenReturn(Optional.of(pauta));

		StatusVotoDTO payload = new StatusVotoDTO();
		payload.setCpfAssociado("07290790901");
		payload.setVoto(TipoVoto.SIM);

		ResponseEntity<String> result = this.restTemplate.postForEntity("http://localhost:" + port + "/v1/pautas/1/votar", payload, String.class);
		assertThat(result.getStatusCodeValue()).isEqualTo(412);
	}

	@Test
	public void votarSessaoPautaNaoEncontrada() throws Exception {		
		StatusVotoDTO payload = new StatusVotoDTO();
		payload.setCpfAssociado("07290790901");
		payload.setVoto(TipoVoto.SIM);

		ResponseEntity<String> result = this.restTemplate.postForEntity("http://localhost:" + port + "/v1/pautas/1/votar", payload, String.class);
		assertThat(result.getStatusCodeValue()).isEqualTo(404);
	}
	
	@Test
	public void votarSessaoFechada() throws Exception {		
		Pauta pauta = new Pauta();
		pauta.setId(1);
		pauta.setNome("teste");
		pauta.setStatus(StatusPauta.SESSAO_FECHADA);
		pauta.setInicio(LocalDateTime.now().minusSeconds(60));
		pauta.setFim(pauta.getInicio().plusSeconds(90));
		when(pautaRepository.findById(1)).thenReturn(Optional.of(pauta));

		StatusVotoDTO payload = new StatusVotoDTO();
		payload.setCpfAssociado("07290790901");
		payload.setVoto(TipoVoto.SIM);

		ResponseEntity<String> result = this.restTemplate.postForEntity("http://localhost:" + port + "/v1/pautas/1/votar", payload, String.class);
		assertThat(result.getStatusCodeValue()).isEqualTo(412);
	}

	@Test
	public void votarNovamenteMesmaPauta() throws Exception {		
		Pauta pauta = new Pauta();
		pauta.setId(1);
		pauta.setNome("teste");
		pauta.setStatus(StatusPauta.SESSAO_ABERTA);
		pauta.setInicio(LocalDateTime.now().minusSeconds(60));
		pauta.setFim(pauta.getInicio().plusSeconds(90));
		when(pautaRepository.findById(1)).thenReturn(Optional.of(pauta));

		Voto voto = new Voto();
		voto.setId(1);
		voto.setCpfAssociado("07290790901");
		voto.setVoto(TipoVoto.NAO);
		voto.setPauta(pauta);
		when(votoRepository.findByCpfAssociadoAndPauta_id(voto.getCpfAssociado(), pauta.getId())).thenReturn(voto);

		StatusVotoDTO payload = new StatusVotoDTO();
		payload.setCpfAssociado(voto.getCpfAssociado());
		payload.setVoto(TipoVoto.SIM);

		ResponseEntity<String> result = this.restTemplate.postForEntity("http://localhost:" + port + "/v1/pautas/1/votar", payload, String.class);
		assertThat(result.getStatusCodeValue()).isEqualTo(412);
	}

}
