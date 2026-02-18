package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
  Team di partecipanti.
  I vincoli di capienza vengono applicati dall'Hackathon,
  che conosce la dimensione massima del team.
 */
public class Team {

    private final int id;
    private final String nome;
    private final List<Utente> membri = new ArrayList<>();
    private final List<Documento> documenti = new ArrayList<>();

    public Team(int id, String nome) {
        if (id <= 0) throw new IllegalArgumentException("Id team non valido");
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome team non valido");
        this.id = id;
        this.nome = nome;
    }

    /**
      Versione semplice usata nei test: aggiunge il membro senza conoscere il max.
      Il vincolo di capienza viene controllato dall'Hackathon quando registra il team.
     */
    public void aggiungiMembro(Utente u) {
        if (u == null) throw new IllegalArgumentException("Utente nullo");
        if (membri.contains(u)) {
            throw new IllegalStateException("Utente già presente nel team");
        }
        membri.add(u);
    }

    /**
      Aggiunge un membro al team.

      @param u           utente da aggiungere
      @param maxTeamSize dimensione massima consentita (vincolo dell'hackathon)
     @return true se aggiunto, false se team pieno o già presente
     */
    @SuppressWarnings("unused") // Rimuovi il metodo se non viene usato da nessuna parte nel progetto.
    public boolean aggiungiMembro(Utente u, int maxTeamSize) {
        if (u == null) throw new IllegalArgumentException("Utente nullo");
        if (maxTeamSize <= 0) throw new IllegalArgumentException("Max team size non valido");

        if (membri.size() >= maxTeamSize) {
            return false;
        }
        if (membri.contains(u)) {
            return false;
        }
        membri.add(u);
        return true;
    }

    /**
      Carica un documento di avanzamento.
     */
    public void caricaDocumento(Documento d) {
        if (d == null) throw new IllegalArgumentException("Documento nullo");
        documenti.add(d);
    }

    /**
      Rimuove un membro dal team (usato quando un utente viene cancellato).
     */
    public void rimuoviMembro(Utente u) {
        if (u == null) {
            return;
        }
        membri.remove(u);
    }

    /**
      Rollback semplice: rimuove l'ultimo documento caricato (se presente).
      Usato se fallisce la persistenza su DB dopo l'inserimento in memoria.
     */
    public void rimuoviUltimoDocumento() {
        if (documenti.isEmpty()) {
            return;
        }
        documenti.remove(documenti.size() - 1);
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public List<Utente> getMembri() {
        return Collections.unmodifiableList(membri);
    }

    public List<Documento> getDocumenti() {
        return Collections.unmodifiableList(documenti);
    }

    /**
      Dettaglio leggibile del team (utile per la GUI).
     */
    public String getDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("Team: ").append(nome).append("\n");
        sb.append("Id: ").append(id).append("\n\n");

        sb.append("Membri (").append(membri.size()).append("):\n");
        for (Utente u : membri) {
            sb.append("- ").append(u).append("\n");
        }

        sb.append("\nProgressi (").append(documenti.size()).append("):\n");
        for (Documento d : documenti) {
            sb.append("- ").append(d).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return nome;
    }

    /**
      Due team sono considerati uguali se hanno lo stesso id.
     Questo rende coerenti i controlli di duplicato basati su List.contains(...)
     usati nell'entità Hackathon.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
