package model;

import java.util.Objects;

/**
 Rappresenta l'associazione tra un oggetto di tipo Utente e un oggetto di tipo Hackathon.

 La classe modella il concetto di registrazione di un utente a uno specifico hackathon.
 Dal punto di vista concettuale, costituisce una classe di associazione tra le entità
 Utente e Hackathon nel modello di dominio.

 La registrazione è immutabile: una volta creata, l'associazione tra utente e hackathon
 non può essere modificata. Entrambi i riferimenti sono obbligatori e non null.

 Invarianti:
 - utente != null
 - hackathon != null

 L'uguaglianza tra due oggetti Registrazione è definita in base alla coppia (utente, hackathon).
 Due registrazioni sono considerate uguali se fanno riferimento allo stesso utente e allo stesso hackathon.
 */
public class Registrazione {

    private final Utente utente;
    private final Hackathon hackathon;

    /**
     Costruisce una nuova registrazione che associa un utente a un hackathon.

     Precondizioni:
     - utente non deve essere null
     - hackathon non deve essere null

     @param utente l'utente che effettua la registrazione
     @param hackathon l'hackathon a cui l'utente si registra
     @throws IllegalArgumentException se utente o hackathon sono null
     */
    public Registrazione(Utente utente, Hackathon hackathon) {
        if (utente == null) throw new IllegalArgumentException("Utente nullo");
        if (hackathon == null) throw new IllegalArgumentException("Hackathon nullo");
        this.utente = utente;
        this.hackathon = hackathon;
    }

    /**
     Restituisce l'utente associato a questa registrazione.

     @return l'utente registrato all'hackathon
     */
    public Utente getUtente() {
        return utente;
    }

    /**
     Restituisce l'hackathon associato a questa registrazione.

     @return l'hackathon a cui l'utente è registrato
     */
    public Hackathon getHackathon() {
        return hackathon;
    }

    /**
     Restituisce una rappresentazione testuale della registrazione.

     La stringa è costruita concatenando la rappresentazione dell'utente
     con il titolo dell'hackathon.

     @return rappresentazione testuale della registrazione
     */
    @Override
    public String toString() {
        return utente + " -> " + hackathon.getTitolo();
    }

    /**
     Determina se questa registrazione è uguale a un altro oggetto.

     Due oggetti Registrazione sono considerati uguali se:
     - fanno riferimento allo stesso utente (in termini di equals)
     - fanno riferimento allo stesso hackathon (in termini di equals)

     @param o oggetto da confrontare
     @return true se le due registrazioni rappresentano la stessa associazione logica
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Registrazione)) return false;
        Registrazione other = (Registrazione) o;
        return Objects.equals(utente, other.utente) && Objects.equals(hackathon, other.hackathon);
    }

    /**
     Restituisce l'hash code coerente con la definizione di equals.

     L'hash è calcolato sulla coppia (utente, hackathon) per garantire
     la corretta gestione della registrazione in strutture dati hash-based
     come HashSet o HashMap.

     @return valore hash della registrazione
     */
    @Override
    public int hashCode() {
        return Objects.hash(utente, hackathon);
    }
}
