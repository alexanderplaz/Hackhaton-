package dao;

import model.Voto;
import java.util.List;

public interface VotoDAO {
    // Inserisce un nuovo voto nel database
    void salvaVoto(Voto voto);

    // Recupera tutti i voti assegnati a uno specifico Team (utile per calcolare la media)
    List<Voto> getVotiPerTeam(int idTeam);

    // (Opzionale) Recupera i voti dati da un giudice specifico
    List<Voto> getVotiDiGiudice(String emailGiudice);
}
