package desafiosicredi.pautasapi.controllers;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import desafiosicredi.pautasapi.config.RabbitMQConfig;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/v1/pautas")
@Tag(name = "pautas", description = "API de pautas")
public class PautaController {

    private static final Logger log = LoggerFactory.getLogger(PautaController.class);
    
    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Operation(summary = "Retorna uma pauta pelo seu id", tags = { "pautas" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Pauta retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Pauta não encontrada", content = { @Content(schema = @Schema(hidden = true)) })
    })
    @GetMapping("/{id}")
    public StatusPautaDTO get(@PathVariable @Parameter(description = "Id da pauta") Integer id) {
        /**
         * Retorna uma pauta.
         * 
         * Criei esse endpoint informar o status e o resultado de uma pauta.
         * 
         * Uma pauta pode estar nos seguites status:
         * 
         *  `CRIADA` - a pauta foi apenas criada mas não teve sessão aberta ainda.
         *  `SESSAO_ABERTA` - a pauta foi aberta para votação
         *  `SESSAO_FECHADA` - a sessão de votação já foi encerrada,
         *                     no entanto os votos ainda podem estar sendo contabilizados pelos consumidores
         *                     (mais detalhes sobre o porque dos consumidores nos próximos comentários) 
         *  `CONCLUIDA` - a sessão foi concluída (todos os votos foram processados e seu tempo já expirou)
         */

        Pauta pauta = pautaRepository.findById(id).orElse(null);

        if (pauta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada");
        }

        return StatusPautaDTO.create(pauta);
    }

    @Operation(summary = "Cria uma pauta", tags = { "pautas" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "201", description = "Pauta criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Nome não informado", content = { @Content(schema = @Schema(hidden = true)) })
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StatusPautaDTO adicionar(@RequestBody PautaDTO dto) {
        /**
         * Cria a pauta.
         * O status inicial da pauta será `CRIADA`, indicando
         * que a pauta foi apenas criada mas para receber votos será
         * necessário abriar uma sessão (próximo método).
         */
        if (dto.getNome() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome da pauta não informado");
        }
        Pauta pauta = dto.transformar();
        pautaRepository.save(pauta); 

        log.info("Pauta " + pauta.getId() + "Criada");
        return StatusPautaDTO.create(pauta);
    }

    @Operation(summary = "Abre a sessão da pauta para votação", description = "A duração da sessão deve ser informada em minutos", tags = { "pautas" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Pauta retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Pauta não encontrada", content = { @Content(schema = @Schema(hidden = true)) })
    })
    @PutMapping("/{id}/abrir-sessao")
    public StatusPautaDTO abrirSessao(
            @PathVariable @Parameter(description = "Id da pauta") Integer id,
            @RequestBody @Parameter(description="Duração da sessão de votação em minutos") AbrirSessaoDTO dto) {

        /*
            Esse endpoint é responsável por abrir a pauta para votação.
            Na prática apenas registro a duração da sessão e altero seu status para sinalizar que
            é possível receber votos nessa pauta.

            Também decidi enviar a pauta recem aberta para o rabbit,
            assim algum consumidor irá iniciar uma verificação de tempos em tempos para identificar se sessão já terminou
            e assim notificar o resultado para a plataforma (ver o outro componente `pautas-worker`).

            Também tomei a decisão de utilizar o `transactionTemplate.execute` pois no termino do método
            é feito o envio de uma mensagem para o rabbit e eu quis garantir que a transação foi 
            comitada antes de fazer esse envio, fiz isso porque fiquei com receio do consumidor pegar a
            mensagem antes mesmo do registro ter sido inserido no banco.
        */
        Pauta pauta = transactionTemplate.execute(new TransactionCallback<Pauta>(){
            @Override
            public Pauta doInTransaction(TransactionStatus status) {
                Pauta pauta = pautaRepository.findById(id).orElse(null);

                if (pauta == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada");
                }

                pauta.setStatus(StatusPauta.SESSAO_ABERTA);
                pauta.setInicio(LocalDateTime.now());
                
                // 1 minuto por default
                Integer duracao = 1;
                if (dto.getDuracao() != null) {
                    duracao = dto.getDuracao();
                }

                pauta.setFim(pauta.getInicio().plusMinutes(duracao));

                pautaRepository.save(pauta);

                log.info("Pauta " + pauta.getId() + "aberta para votação");
                return pauta;
            }
        });

        // Enviando a pauta para verificação assincrona.
        this.rabbitTemplate.convertAndSend(RabbitMQConfig.FILA_VERIFICAR_STATUS_PAUTA, pauta.getId());

        log.info("Pauta " + pauta.getId() + "aberta para votação");
        return StatusPautaDTO.create(pauta);
    }
    
    @Operation(summary = "Envia um voto para a pauta especificada", description = "O voto deve ser SIM ou NAO", tags = { "pautas" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "201", description = "Voto enviado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Pauta não encontrada", content = { @Content(schema = @Schema(hidden = true)) }),
        @ApiResponse(responseCode = "412", description = "Pauta não pode receber votações ou a votação já foi realizada pelo associado", content = { @Content(schema = @Schema(hidden = true)) })
    })
    @PostMapping("/{id}/votar")
    @ResponseStatus(HttpStatus.CREATED)
    public StatusVotoDTO votar(@PathVariable @Parameter(description = "Id da pauta") Integer id, @RequestBody VotoDTO dto) {
        /*
        Esse endpoint é responsável por regisrar uma intenção de voto.
        Na prática apenas registro o voto no banco e depois envio o voto
        para processamento asincrono.

        Tomei a decisão de deixar o processamento do voto asincrono devido a 
        integração com o serviço externo que valida o CPF para votação (https://user-info.herokuapp.com/users/{cpf}).
        Como não tenho controle sobre a estabilidade desse sistema externo,
        acho que é melhor deixar o processamento asincrono pra já liberar a API
        e além disso o consumidor pode fazer retentativas.

        Caso o associado queira verificar se o voto foi contabilizado ou rejeitado
        é possível chamar o endpoint `/votos/{id}` e verificar o `status` do voto.

        A identificação do associado que está realizando o voto é apenas o `cpf` enviado
        no request. O status incial do voto é `PENDENTE` pois o voto será enviado para processamento
        assincrono.

        Também tomei a decisão de utilizar o `transactionTemplate.execute` pois no termino do método
        é feito o envio de uma mensagem para o rabbit e eu quis garantir que a transação foi 
        comitada antes de fazer esse envio, fiz isso porque fiquei com receio do consumidor pegar a
        mensagem antes mesmo do registro ter sido inserido no banco.
        */
        Voto voto = transactionTemplate.execute(new TransactionCallback<Voto>(){
            @Override
            public Voto doInTransaction(TransactionStatus status) {
                Pauta pauta = pautaRepository.findById(id).orElse(null);

                if (pauta == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada");
                }

                /**
                 * Verifico se a pauta está apta a receber votos,
                 * para isso a mesma deve estar com o status `SESSAO_ABERTA`
                 * e não pode estar expirada.
                 */
                if (pauta.getStatus() != StatusPauta.SESSAO_ABERTA || LocalDateTime.now().isAfter(pauta.getFim())) {
                    throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Pauta não pode receber votações");
                }

                /**
                 * Verico se o associado já não votou nessa pauta.
                 */
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

        // Enviando o voto para o rabbit.
        this.rabbitTemplate.convertAndSend(RabbitMQConfig.FILA_CONTABILIZAR_VOTO, voto.getId());

        log.info("Voto " + voto.getId() + ", associado " + voto.getCpfAssociado());
        return StatusVotoDTO.create(voto);
    }

}
