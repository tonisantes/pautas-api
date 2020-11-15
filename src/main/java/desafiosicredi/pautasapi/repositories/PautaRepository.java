package desafiosicredi.pautasapi.repositories;

import org.springframework.data.repository.CrudRepository;

import desafiosicredi.pautasapi.model.Pauta;


public interface PautaRepository extends CrudRepository<Pauta, Integer> {
    
}
