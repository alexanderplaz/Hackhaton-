import java.util.ArrayList;
import java.util.List;

public class Team {
    private int id;
    private String nome;
    private List<Utente> membri = new ArrayList<>();
    private List<Documento> documenti = new ArrayList<>();

    public Team(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public void aggiungiMembro(Utente u) {
        membri.add(u);
    }

    public void caricaDocumento(Documento d) {
        documenti.add(d);
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public List<Utente> getMembri() { return membri; }
}