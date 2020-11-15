package desafiosicredi.pautasapi.repositories;

import org.springframework.data.repository.CrudRepository;

import desafiosicredi.pautasapi.model.Voto;


public interface VotoRepository extends CrudRepository<Voto, Integer> {
    Voto findByCpfAssociadoAndPauta_id(String cpfAssociado, Integer pautaId);
}
