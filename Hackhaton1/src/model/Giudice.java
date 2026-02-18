package model;

/**
 Rappresenta il ruolo di Giudice all'interno del dominio dell'hackathon.

 La classe modella un giudice come specializzazione di Utente.
 In questa implementazione non introduce nuovi attributi o comportamenti rispetto alla superclasse,
 ma esiste per motivi di modellazione concettuale:
 - distinguere semanticamente i giudici dagli utenti generici
 - rendere esplicita la tipologia nei metodi e nelle collezioni del dominio (es. lista dei giudici dell'hackathon)
 - supportare eventuali evoluzioni future del modello (attributi o operazioni specifiche dei giudici)

 La validazione dei parametri e l'inizializzazione dello stato sono delegate al costruttore della superclasse Utente.

 */
public class Giudice extends Utente {

    /**
     Costruisce un nuovo Giudice.

     Il costruttore delega alla superclasse Utente l'inizializzazione e la validazione dei campi.

     @param id identificativo univoco del giudice
     @param nome nome del giudice
     @param cognome cognome del giudice
     @param email email del giudice
     @throws IllegalArgumentException se i parametri non rispettano i vincoli definiti nella superclasse Utente
     */
    public Giudice(int id, String nome, String cognome, String email) {
        super(id, nome, cognome, email);
    }
}
