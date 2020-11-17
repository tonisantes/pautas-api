package desafiosicredi.pautasapi.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.reactive.TransactionSynchronization;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import desafiosicredi.pautasapi.config.RabbitMQ;
import desafiosicredi.pautasapi.dto.AbrirSessaoDTO;
import desafiosicredi.pautasapi.dto.PautaDTO;
import desafiosicredi.pautasapi.dto.StatusPautaDTO;
import desafiosicredi.pautasapi.dto.StatusVotoDTO;
import desafiosicredi.pautasapi.dto.VotoDTO;
import desafiosicredi.pautasapi.model.Pauta;
import desafiosicredi.pautasapi.model.StatusPauta;
import desafiosicredi.pautasapi.model.Voto;
import desafiosicredi.pautasapi.repositories.PautaRepository;
import desafiosicredi.pautasapi.repositories.VotoRepository;

@RestController
public class PautaController {
    
    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @GetMapping("/pautas/{id}")
    public StatusPautaDTO adicionar(@PathVariable Integer id) {
        Pauta pauta = pautaRepository.findById(id).orElse(null);

        if (pauta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada");
        }

        return StatusPautaDTO.create(pauta);
    }

    @PostMapping("/pautas")
    @ResponseStatus(HttpStatus.CREATED)
    public StatusPautaDTO adicionar(@RequestBody PautaDTO dto) {
        if (dto.getNome() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome da pauta não informado");
        }
        Pauta pauta = dto.transformar();
        pautaRepository.save(pauta); 
        return StatusPautaDTO.create(pauta);
    }

    @PutMapping("/pautas/{id}/abrir")
    public StatusPautaDTO abrirSessao(@PathVariable Integer id, @RequestBody AbrirSessaoDTO dto) {
        Pauta pauta = transactionTemplate.execute(new TransactionCallback<Pauta>(){
            @Override
            public Pauta doInTransaction(TransactionStatus status) {
                Pauta pauta = pautaRepository.findById(id).orElse(null);

                if (pauta == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada");
                }

                pauta.setStatus(StatusPauta.SESSAO_ABERTA);
                pauta.setInicio(LocalDateTime.now());

                Integer duracao = 1;
                if (dto.getDuracao() != null) {
                    duracao = dto.getDuracao();
                }

                pauta.setFim(pauta.getInicio().plusMinutes(duracao));

                pautaRepository.save(pauta);

                return pauta;
            }
        });

        this.rabbitTemplate.convertAndSend(RabbitMQ.FILA_VERIFICAR_STATUS_PAUTA, pauta.getId());
        return StatusPautaDTO.create(pauta);
        
    }
    
    @PostMapping("/pautas/{id}/votar")
    @ResponseStatus(HttpStatus.CREATED)
    public StatusVotoDTO votar(@PathVariable Integer id, @RequestBody VotoDTO dto) {
        Voto voto = transactionTemplate.execute(new TransactionCallback<Voto>(){
            @Override
            public Voto doInTransaction(TransactionStatus status) {
                Pauta pauta = pautaRepository.findById(id).orElse(null);

                if (pauta == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada");
                }

                if (pauta.getStatus() != StatusPauta.SESSAO_ABERTA || LocalDateTime.now().isAfter(pauta.getFim())) {
                    throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Pauta não pode receber votações");
                }

                Voto voto = votoRepository.findByCpfAssociadoAndPauta_id(dto.getCpfAssociado(), pauta.getId());

                if (voto != null) {
                    throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Voto desse associado já foi realizado para essa pauta");
                }

                voto = dto.transformar();
                voto.setPauta(pauta);
                votoRepository.save(voto);
                return voto;
            }
        });

        this.rabbitTemplate.convertAndSend(RabbitMQ.FILA_CONTABILIZAR_VOTO, voto.getId());
        return StatusVotoDTO.create(voto);
    }

}
