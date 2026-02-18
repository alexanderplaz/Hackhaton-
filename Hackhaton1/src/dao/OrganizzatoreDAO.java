package dao;

import model.Organizzatore;

/**
 DAO per la persistenza degli {@link Organizzatore}.

 Definisce le operazioni minime per:
 - registrare un organizzatore;
 - recuperarlo tramite email.
 */
public interface OrganizzatoreDAO {

    /**
     Salva un organizzatore nel sistema di persistenza.

     @param organizzatore organizzatore da registrare.
     */
    void registraOrganizzatore(Organizzatore organizzatore);

    /**
     Legge un organizzatore a partire dalla sua email.

     @param email email da cercare.
     @return organizzatore trovato, oppure null se non esiste.
     */
    Organizzatore leggiOrganizzatorePerEmail(String email);
}
