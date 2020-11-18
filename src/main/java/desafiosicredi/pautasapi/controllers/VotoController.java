package desafiosicredi.pautasapi.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import desafiosicredi.pautasapi.dto.StatusVotoDTO;
import desafiosicredi.pautasapi.model.Voto;
import desafiosicredi.pautasapi.repositories.VotoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/v1/votos")
@Tag(name = "votos", description = "API de votos")
public class VotoController {
    @Autowired
    private VotoRepository votoRepository;
    
    @Operation(summary = "Retorna um voto pelo seu id", tags = { "votos" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Voto retornado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Voto não encontrado", content = { @Content(schema = @Schema(hidden = true)) })
    })
    @GetMapping("/{id}")
    public StatusVotoDTO get(@PathVariable Integer id) {
        /**
         * Busca um voto para verificação do seu status (se foi contabilizado/rejeitado, ou se ainda está pendente)
         */
        Voto voto = votoRepository.findById(id).orElse(null);

        if (voto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voto não encontrado");
        }

        return StatusVotoDTO.create(voto);
    }

}
