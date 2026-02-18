package model;

import java.util.Objects;

/**
 Rappresenta l'entità Organizzatore nel dominio applicativo dell'hackathon.

 L'organizzatore è il soggetto responsabile della gestione dell'evento.
 Dal punto di vista del modello, è un'entità identificata univocamente da un id
 e caratterizzata da nome, cognome e password.

 Vincoli di validazione:
 - id deve essere strettamente positivo
 - nome, cognome e password devono essere non null, non vuoti e di lunghezza minima pari a 3 caratteri

 La classe è immutabile: tutti gli attributi sono final e inizializzati nel costruttore.

 Nota di sicurezza:
 La password viene memorizzata in chiaro esclusivamente per finalità didattiche.
 In un sistema reale si utilizzerebbe un meccanismo di hashing sicuro con salt.
 */
public class Organizzatore {

    private final int id;
    private final String nome;
    private final String cognome;
    private final String password;

    /**
     Costruisce un nuovo oggetto Organizzatore.

     Precondizioni:
     - id > 0
     - nome, cognome e password devono avere almeno 3 caratteri significativi

     @param id identificativo univoco dell'organizzatore
     @param nome nome dell'organizzatore
     @param cognome cognome dell'organizzatore
     @param password password necessaria per accedere alla modalità organizzatore
     @throws IllegalArgumentException se uno dei parametri viola i vincoli definiti
     */
    public Organizzatore(int id, String nome, String cognome, String password) {
        if (id <= 0) throw new IllegalArgumentException("Id organizzatore non valido");
        this.nome = validateMin3(nome, "Nome organizzatore non valido");
        this.cognome = validateMin3(cognome, "Cognome organizzatore non valido");
        this.password = validateMin3(password, "Password non valida");
        this.id = id;
    }

    /**
     Metodo di validazione interna che verifica che una stringa:
     - non sia null
     - non sia composta solo da spazi
     - abbia lunghezza minima pari a 3 caratteri

     @param s stringa da validare
     @param msg messaggio di errore da utilizzare in caso di violazione
     @return la stringa ripulita da spazi iniziali e finali
     @throws IllegalArgumentException se la stringa non rispetta i vincoli
     */
    private static String validateMin3(String s, String msg) {
        if (s == null) throw new IllegalArgumentException(msg);
        String t = s.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(msg);
        if (t.length() < 3) throw new IllegalArgumentException(msg + " (min 3 caratteri)");
        return t;
    }

    /**
     Restituisce l'identificativo univoco dell'organizzatore.

     @return id dell'organizzatore
     */
    public int getId() {
        return id;
    }

    /**
     Restituisce il nome dell'organizzatore.

     @return nome
     */
    public String getNome() {
        return nome;
    }

    /**
     Restituisce il cognome dell'organizzatore.

     @return cognome
     */
    public String getCognome() {
        return cognome;
    }

    /**
     Restituisce il nome completo dell'organizzatore,
     ottenuto concatenando nome e cognome.

     @return nome completo nel formato "Nome Cognome"
     */
    public String getNomeCompleto() {
        return nome + " " + cognome;
    }

    /**
     Verifica se la password fornita corrisponde a quella memorizzata.

     Il confronto è effettuato tramite equals tra stringhe.
     Se il parametro è null, il metodo restituisce false.

     @param attempt password tentata
     @return true se la password coincide, false altrimenti
     */
    public boolean checkPassword(String attempt) {
        if (attempt == null) return false;
        return password.equals(attempt);
    }

    /**
     Restituisce la password memorizzata.

     Questo metodo è previsto esclusivamente per esigenze di persistenza
     nello strato DAO.

     Nota: in un sistema reale non si esporrebbe mai la password in chiaro.

     @return password dell'organizzatore
     */
    public String getPassword() {
        return password;
    }

    /**
     Restituisce una rappresentazione testuale dell'organizzatore.

     La rappresentazione coincide con il nome completo.

     @return stringa rappresentante l'organizzatore
     */
    @Override
    public String toString() {
        return getNomeCompleto();
    }

    /**
     Determina l'uguaglianza logica tra due organizzatori.

     Due oggetti Organizzatore sono considerati uguali se
     possiedono lo stesso identificativo id.

     @param o oggetto da confrontare
     @return true se rappresentano lo stesso organizzatore
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Organizzatore)) return false;
        Organizzatore that = (Organizzatore) o;
        return id == that.id;
    }

    /**
     Restituisce l'hash code coerente con la definizione di equals.

     L'hash è calcolato esclusivamente sull'id,
     in quanto identificatore univoco dell'entità.

     @return valore hash dell'organizzatore
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
