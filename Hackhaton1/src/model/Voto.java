package model;

/**
  Rappresenta la valutazione finale assegnata da un Giudice a un Team.
  Ogni oggetto Voto modella una relazione uno-a-uno tra:
  - un giudice (chi assegna il voto),
  - un team (chi riceve il voto),
  - un punteggio numerico.
  Il punteggio è vincolato all'intervallo chiuso [0,10],
 coerente con la scala di valutazione definita nel dominio dell'applicazione.
  La classe è immutabile:
 tutti gli attributi sono final e vengono inizializzati esclusivamente
  nel costruttore, garantendo coerenza dello stato dopo la creazione.
 */
public class Voto {

    // Valore numerico assegnato al team (range valido: 0..10)
    private final int punteggio;

    // Giudice che ha espresso la valutazione
    private final Giudice giudice;

    // Team oggetto della valutazione
    private final Team team;

    /**
      Costruisce un voto valido.
      Vengono effettuati controlli di validità per garantire
      che l'oggetto sia sempre in uno stato consistente:
      - il punteggio deve essere compreso tra 0 e 10 inclusi;
      - giudice e team non possono essere null.
      In caso di violazione dei vincoli, viene sollevata un'eccezione
      per impedire la creazione di un oggetto incoerente.
     */
    public Voto(int punteggio, Giudice giudice, Team team) {
        if (punteggio < 0 || punteggio > 10) {
            throw new IllegalArgumentException("Punteggio non valido (0..10)");
        }
        if (giudice == null) throw new IllegalArgumentException("Giudice nullo");
        if (team == null) throw new IllegalArgumentException("Team nullo");
        this.punteggio = punteggio;
        this.giudice = giudice;
        this.team = team;
    }

    /**
      Restituisce il valore numerico del voto.
     */
    public int getPunteggio() {
        return punteggio;
    }

    /**
      Restituisce il giudice che ha espresso la valutazione.
     */
    public Giudice getGiudice() {
        return giudice;
    }

    /**
      Restituisce il team che ha ricevuto la valutazione.
     */
    public Team getTeam() {
        return team;
    }
}
