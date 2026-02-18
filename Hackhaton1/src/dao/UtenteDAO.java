package dao;

import model.Utente;
import java.util.List;

public interface UtenteDAO {
    // Metodo per registrare un nuovo utente
    void registraUtente(Utente utente);

    // Metodo per trovare un utente tramite email (utile per il login)
    Utente leggiUtentePerEmail(String email);

    // Metodo per ottenere tutti gli utenti
    List<Utente> leggiTuttiUtenti();

    // Elimina un utente tramite id (dopo la cancellazione id/email sono riutilizzabili)
    void eliminaUtentePerId(int id);
}
