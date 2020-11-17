package desafiosicredi.pautasapi.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import desafiosicredi.pautasapi.model.Pauta;
import desafiosicredi.pautasapi.model.StatusPauta;
import desafiosicredi.pautasapi.model.StatusVoto;
import desafiosicredi.pautasapi.model.TipoVoto;
import desafiosicredi.pautasapi.model.Voto;

public class StatusPautaDTO {
    private Integer id;

    private String nome;

    private StatusPauta status;

    private Map<TipoVoto, Integer> resultado = new HashMap<>();

    private LocalDateTime inicio;

    private LocalDateTime fim;

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

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }

    public Map<TipoVoto, Integer> getResultado() {
        return resultado;
    }

    public void setResultado(Map<TipoVoto, Integer> resultado) {
        this.resultado = resultado;
    }

    public static StatusPautaDTO create(Pauta pauta) {
        StatusPautaDTO dto = new StatusPautaDTO();
        dto.setId(pauta.getId());
        dto.setNome(pauta.getNome());
        dto.setStatus(pauta.getStatus());
        dto.setInicio(pauta.getInicio());
        dto.setFim(pauta.getFim());

        dto.resultado.put(TipoVoto.SIM, 0);
        dto.resultado.put(TipoVoto.NAO, 0);

        for (Voto voto : pauta.getVotos()) {
            if (voto.getStatus() == StatusVoto.CONTABILIZADO) {
                Integer res = dto.resultado.get(voto.getVoto());
                dto.resultado.put(voto.getVoto(), ++res);
            }
        }
        return dto;
    }
}
