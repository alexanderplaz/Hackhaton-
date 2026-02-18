package implementazionipostgresdao;

import dao.GiudiceDAO;
import database.ConnessioneDatabase;
import model.Giudice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 Implementazione di {@link GiudiceDAO} su database PostgreSQL.

 La classe gestisce la persistenza dell'entità {@link Giudice} tramite JDBC,
 eseguendo operazioni di inserimento, lettura singola (per email), lettura completa
 e cancellazione per identificativo.

 Le query fanno riferimento a uno schema con identificatori quotati
 (es. "Giudice", "Id", "Nome", "Cognome", "Email").
 Eventuali errori JDBC vengono intercettati e incapsulati in una {@link DaoException},
 trasformando le eccezioni checked in unchecked coerentemente con lo strato DAO.
 */
public class GiudiceImplementazionePostgresDAO implements GiudiceDAO {

    private static final String INSERT_GIUDICE =
            "INSERT INTO \"Giudice\"(\"Id\",\"Nome\",\"Cognome\",\"Email\") VALUES (?, ?, ?, ?)";

    private static final String SELECT_BY_EMAIL =
            "SELECT \"Id\", \"Nome\", \"Cognome\", \"Email\" FROM \"Giudice\" WHERE \"Email\" = ?";

    private static final String SELECT_ALL =
            "SELECT \"Id\", \"Nome\", \"Cognome\", \"Email\" FROM \"Giudice\" ORDER BY \"Id\"";

    private static final String DELETE_BY_ID =
            "DELETE FROM \"Giudice\" WHERE \"Id\" = ?";

    /**
     Restituisce una connessione JDBC fornita dal singleton {@link ConnessioneDatabase}.

     La configurazione e gestione della connessione è delegata allo strato infrastrutturale.
     */
    private static Connection getConnection() {
        return ConnessioneDatabase.getInstance().getConnection();
    }

    /**
     Registra un nuovo giudice nel database.

     L'operazione esegue un INSERT parametrico valorizzando id, nome, cognome ed email.
     Viene utilizzato un {@link PreparedStatement} per garantire tipizzazione corretta
     dei parametri e prevenire SQL injection.

     @param giudice entità da registrare; non può essere null
     @throws DaoException se giudice è null o se si verifica un errore JDBC
     */
    @Override
    public void registraGiudice(Giudice giudice) {
        if (giudice == null) {
            throw new DaoException("Giudice nullo");
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_GIUDICE)) {

            ps.setInt(1, giudice.getId());
            ps.setString(2, giudice.getNome());
            ps.setString(3, giudice.getCognome());
            ps.setString(4, giudice.getEmail());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DaoException("Errore durante la registrazione del giudice", e);
        }
    }

    /**
     Recupera un giudice a partire dalla sua email.

     Se l'email è null o vuota (blank), il metodo restituisce null senza interrogare il database.
     In caso contrario, l'email viene normalizzata tramite trim e utilizzata come parametro
     della query di selezione.

     @param email email del giudice da cercare
     @return il giudice corrispondente, oppure null se non presente
     @throws DaoException se si verifica un errore JDBC durante la lettura
     */
    @Override
    public Giudice leggiGiudicePerEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_EMAIL)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                return mapGiudice(rs);
            }

        } catch (SQLException e) {
            throw new DaoException("Errore durante la lettura del giudice per email", e);
        }
    }

    /**
     Restituisce l'elenco completo dei giudici presenti nel database.

     L'ordinamento è demandato al database tramite ORDER BY sull'identificativo.

     @return lista di giudici (eventualmente vuota)
     @throws DaoException se si verifica un errore JDBC durante la lettura
     */
    @Override
    public List<Giudice> leggiTuttiGiudici() {

        List<Giudice> lista = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapGiudice(rs));
            }

            return lista;

        } catch (SQLException e) {
            throw new DaoException("Errore durante la lettura dei giudici", e);
        }
    }

    /**
     Elimina un giudice dal database a partire dal suo identificativo.

     Se l'operazione di DELETE non rimuove alcuna riga, viene sollevata una
     {@link DaoException} per segnalare che il giudice non è presente.

     @param id identificativo del giudice
     @throws DaoException se id non è valido, se il giudice non esiste
     oppure se si verifica un errore JDBC
     */
    @Override
    public void eliminaGiudicePerId(int id) {
        if (id <= 0) {
            throw new DaoException("Id giudice non valido");
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_ID)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DaoException("Giudice non trovato (id=" + id + ")");
            }

        } catch (SQLException e) {
            throw new DaoException("Errore durante la cancellazione del giudice", e);
        }
    }

    /**
     Converte la riga corrente del {@link ResultSet} in un oggetto {@link Giudice}.

     I valori vengono letti tramite i nomi delle colonne definiti nella query
     ("Id", "Nome", "Cognome", "Email") e utilizzati per istanziare l'entità di dominio.

     @param rs result set posizionato su una riga valida
     @return giudice ricostruito a partire dalla riga corrente
     @throws SQLException se si verifica un errore JDBC o una colonna attesa non è presente
     */
    private Giudice mapGiudice(ResultSet rs) throws SQLException {
        int id = rs.getInt("Id");
        String nome = rs.getString("Nome");
        String cognome = rs.getString("Cognome");
        String email = rs.getString("Email");
        return new Giudice(id, nome, cognome, email);
    }
}
