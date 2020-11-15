package desafiosicredi.pautasapi.dto;

import desafiosicredi.pautasapi.model.Pauta;

public class PautaDTO {

    private String nome;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Pauta transformar() {
        Pauta pauta = new Pauta();
        pauta.setNome(this.getNome());
        return pauta;
    }
}
