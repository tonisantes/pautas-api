package desafiosicredi.pautasapi.dto;

import desafiosicredi.pautasapi.model.TipoVoto;
import desafiosicredi.pautasapi.model.Voto;

public class VotoDTO {
    private String cpfAssociado;

    private TipoVoto voto;

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

    public Voto transformar() {
        Voto voto = new Voto();
        voto.setCpfAssociado(this.getCpfAssociado());
        voto.setVoto(this.getVoto());
        return voto;
    }

}
