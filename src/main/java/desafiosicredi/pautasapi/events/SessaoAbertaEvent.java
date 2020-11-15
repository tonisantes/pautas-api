package desafiosicredi.pautasapi.events;

import desafiosicredi.pautasapi.model.Pauta;

public class SessaoAbertaEvent {
    private Pauta pauta;

    public SessaoAbertaEvent(Pauta pauta) {
        this.pauta = pauta;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public void setPauta(Pauta pauta) {
        this.pauta = pauta;
    }
}
