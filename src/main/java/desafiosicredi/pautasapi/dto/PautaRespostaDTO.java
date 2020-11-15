package desafiosicredi.pautasapi.dto;

import desafiosicredi.pautasapi.model.Pauta;
import desafiosicredi.pautasapi.model.StatusPauta;

public class PautaRespostaDTO {
    private Integer id;

    private String nome;

    private StatusPauta status;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public StatusPauta getStatus() {
        return status;
    }

    public void setStatus(StatusPauta status) {
        this.status = status;
    }

    public static PautaRespostaDTO create(Pauta pauta) {
        PautaRespostaDTO dto = new PautaRespostaDTO();
        dto.setId(pauta.getId());
        dto.setNome(pauta.getNome());
        dto.setStatus(pauta.getStatus());
        return dto;
    }
}
