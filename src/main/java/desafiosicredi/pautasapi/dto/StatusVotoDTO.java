package desafiosicredi.pautasapi.dto;

import desafiosicredi.pautasapi.model.StatusVoto;
import desafiosicredi.pautasapi.model.TipoVoto;
import desafiosicredi.pautasapi.model.Voto;

public class StatusVotoDTO {

    private Integer id;

    private String cpfAssociado;

    private TipoVoto voto;

    private StatusVoto status;

    private Integer pautaId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCpfAssociado() {
        return cpfAssociado;
    }

    public void setCpfAssociado(String cpfAssociado) {
        this.cpfAssociado = cpfAssociado;
    }

    public TipoVoto getVoto() {
        return voto;
    }

    public void setVoto(TipoVoto voto) {
        this.voto = voto;
    }

    public StatusVoto getStatus() {
        return status;
    }

    public void setStatus(StatusVoto status) {
        this.status = status;
    }

    public Integer getPautaId() {
        return pautaId;
    }

    public void setPautaId(Integer pautaId) {
        this.pautaId = pautaId;
    }

    public static StatusVotoDTO create(Voto voto) {
        StatusVotoDTO dto = new StatusVotoDTO();
        dto.setId(voto.getId());
        dto.setCpfAssociado(voto.getCpfAssociado());
        dto.setStatus(voto.getStatus());
        dto.setVoto(voto.getVoto());
        dto.setPautaId(voto.getPauta().getId());
        return dto;
    }

}
