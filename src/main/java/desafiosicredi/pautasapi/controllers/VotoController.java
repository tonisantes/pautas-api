package desafiosicredi.pautasapi.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import desafiosicredi.pautasapi.model.Voto;
import desafiosicredi.pautasapi.repositories.VotoRepository;

@RestController
public class VotoController {
    @Autowired
    private VotoRepository votoRepository;
    
    @GetMapping("/votos/{id}")
    public Voto get(@PathVariable Integer id) {
        Voto voto = votoRepository.findById(id).orElse(null);

        if (voto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voto não encontrado");
        }

        return voto;
    }

}
