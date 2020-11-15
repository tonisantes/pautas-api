package desafiosicredi.pautasapi.model;

import javax.persistence.Column;
import javax.persistence.ForeignKey;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Table(uniqueConstraints={
    @UniqueConstraint(columnNames = {"pauta_id", "cpf_associado"}, name = "uq_voto")
})
public class Voto {
    
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "pauta_id", nullable = false, foreignKey = @ForeignKey(name="fk_voto_pauta_id"))
	private Pauta pauta;

    @NotBlank
    @Column(name = "cpf_associado", nullable = false, length = 11)
    private String cpfAssociado;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)  
    private TipoVoto voto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)  
    private StatusVoto status = StatusVoto.PENDENTE;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public void setPauta(Pauta pauta) {
        this.pauta = pauta;
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
    
}
