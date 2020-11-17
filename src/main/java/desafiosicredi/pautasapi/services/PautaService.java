package desafiosicredi.pautasapi.services;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import desafiosicredi.pautasapi.config.RabbitMQ;
import desafiosicredi.pautasapi.dto.StatusVotoDTO;
import desafiosicredi.pautasapi.dto.VotoDTO;
import desafiosicredi.pautasapi.model.Pauta;
import desafiosicredi.pautasapi.model.StatusPauta;
import desafiosicredi.pautasapi.model.Voto;
import desafiosicredi.pautasapi.repositories.PautaRepository;
import desafiosicredi.pautasapi.repositories.VotoRepository;

@Service
public class PautaService {

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;
    
    @Async
    public CompletableFuture<StatusVotoDTO> votarMesmo(Integer id, VotoDTO dto) {
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
        return CompletableFuture.completedFuture(StatusVotoDTO.create(voto));
    }

}
