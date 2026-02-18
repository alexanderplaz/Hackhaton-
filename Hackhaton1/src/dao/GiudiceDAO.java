package dao;

import model.Giudice;
import java.util.List;

public interface GiudiceDAO {
    // Registra un nuovo giudice nel database
    void registraGiudice(Giudice giudice);

    // Trova un giudice tramite email (utile per il login)
    Giudice leggiGiudicePerEmail(String email);

    // Restituisce la lista di tutti i giudici
    List<Giudice> leggiTuttiGiudici();

    // Elimina un giudice tramite id (dopo la cancellazione id/email sono riutilizzabili)
    void eliminaGiudicePerId(int id);
}