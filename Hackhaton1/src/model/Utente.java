package model;

import java.util.Objects;

/**
 Entità di dominio che rappresenta un utente della piattaforma.

 L'oggetto Utente modella una persona registrata al sistema,
 identificata univocamente da un id intero positivo.

 Vincoli di dominio applicati:
 - id deve essere maggiore di zero;
 - nome e cognome devono contenere almeno 3 caratteri dopo trim;
 - email non può essere null né vuota.

 La classe è immutabile: tutti gli attributi sono final
 e vengono inizializzati esclusivamente nel costruttore.
 */
public class Utente {

    // Identificatore univoco dell'utente
    private final int id;

    // Nome dell'utente (minimo 3 caratteri dopo trim)
    private final String nome;

    // Cognome dell'utente (minimo 3 caratteri dopo trim)
    private final String cognome;

    // Email dell'utente (non null e non vuota)
    private final String email;

    /**
     Costruisce un nuovo utente valido.

     Il costruttore applica controlli per garantire la coerenza
     dello stato dell'oggetto al momento della creazione.
     Se uno dei vincoli non è rispettato viene sollevata
     un'eccezione IllegalArgumentException.
     */
    public Utente(int id, String nome, String cognome, String email) {
        if (id <= 0) {
            throw new IllegalArgumentException("Id utente non valido");
        }

        this.nome = validateNomeCognome(nome, "Nome non valido");
        this.cognome = validateNomeCognome(cognome, "Cognome non valido");

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email non valida");
        }

        this.email = email.trim();
        this.id = id;
    }

    /**
     Metodo di supporto per validare nome e cognome.

     Applica le seguenti regole:
     - la stringa non può essere null;
     - dopo trim non può essere vuota;
     - deve avere almeno 3 caratteri.

     Restituisce la stringa normalizzata (trim).
     */
    private static String validateNomeCognome(String s, String msg) {

        String t = s.trim();

        if (t.isEmpty()) {
            throw new IllegalArgumentException(msg);
        }

        if (t.length() < 3) {
            throw new IllegalArgumentException(msg + " (min 3 caratteri)");
        }

        return t;
    }

    /**
     Restituisce l'identificatore univoco dell'utente.
     */
    public int getId() {
        return id;
    }

    /**
     Restituisce il nome dell'utente.
     */
    public String getNome() {
        return nome;
    }

    /**
     Restituisce il cognome dell'utente.
     */
    public String getCognome() {
        return cognome;
    }

    /**
     Restituisce il nome completo nel formato "Nome Cognome".
     */
    public String getNomeCompleto() {
        return nome + " " + cognome;
    }

    /**
     Restituisce l'indirizzo email associato all'utente.
     */
    public String getEmail() {
        return email;
    }

    /**
     Rappresentazione testuale dell'utente.
     Mostra nome completo ed email.
     */
    @Override
    public String toString() {
        return getNomeCompleto() + " (" + email + ")";
    }

    /**
     Due utenti sono considerati uguali se hanno lo stesso id.

     Questo significa che l'identità logica dell'utente
     nel sistema è determinata esclusivamente dall'id.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Utente other)) return false;
        return id == other.id;
    }

    /**
     Calcola l'hash code coerentemente con equals.
     Poiché equals dipende solo dall'id,
     anche hashCode utilizza esclusivamente l'id.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
