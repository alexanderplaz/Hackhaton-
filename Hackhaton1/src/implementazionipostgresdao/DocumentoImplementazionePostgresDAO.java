package implementazionipostgresdao;

import dao.DocumentoDAO;
import database.ConnessioneDatabase;
import model.Documento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 Implementazione di {@link DocumentoDAO} su database PostgreSQL.

 La classe gestisce la persistenza dei {@link Documento} associati a un team.
 Ogni documento viene memorizzato con:
 - riferimento al team (chiave esterna RefTeam);
 - istante temporale di inserimento (Timestamp);
 - contenuto testuale (Contenuto).

 Le operazioni esposte consentono di:
 - salvare un documento per un determinato team;
 - leggere tutti i documenti di un team, ordinati per timestamp crescente.

 Gli errori JDBC vengono intercettati e incapsulati in una {@link DaoException},
 coerentemente con lo stile dello strato DAO.
 */
public class DocumentoImplementazionePostgresDAO implements DocumentoDAO {

    private static final String INSERT_DOC =
            "INSERT INTO \"Documento\"(\"RefTeam\",\"Timestamp\",\"Contenuto\") VALUES (?, ?, ?)";

    private static final String SELECT_BY_TEAM =
            "SELECT \"Timestamp\", \"Contenuto\" FROM \"Documento\" WHERE \"RefTeam\" = ? ORDER BY \"Timestamp\"";

    /**
     Restituisce una connessione JDBC fornita dal singleton {@link ConnessioneDatabase}.

     La configurazione e la gestione della connessione verso PostgreSQL sono delegate
     alla componente infrastrutturale.
     */
    private static Connection getConnection() {
        return ConnessioneDatabase.getInstance().getConnection();
    }

    /**
     Salva un documento associandolo a uno specifico team.

     L'operazione esegue un INSERT parametrico nella tabella Documento.
     Il timestamp del documento viene convertito in {@link Timestamp} per la persistenza.

     @param teamId identificativo del team a cui associare il documento
     @param documento documento da salvare
     @throws DaoException se teamId non è valido, se documento è null oppure se si verifica un errore JDBC
     */
    @Override
    public void salvaDocumento(int teamId, Documento documento) {
        if (teamId <= 0) {
            throw new DaoException("Id team non valido");
        }
        if (documento == null) {
            throw new DaoException("Documento nullo");
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_DOC)) {

            ps.setInt(1, teamId);
            ps.setTimestamp(2, Timestamp.valueOf(documento.getTimestamp()));
            ps.setString(3, documento.getContenuto());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DaoException("Errore durante il salvataggio del documento", e);
        }
    }

    /**
     Legge tutti i documenti associati a un team.

     Se l'identificativo del team non è valido (<= 0), viene restituita una lista vuota
     senza effettuare accesso al database.

     I documenti vengono recuperati ordinati per timestamp crescente. Per ciascuna riga
     si ricostruisce un oggetto {@link Documento} a partire da contenuto e data derivata
     dal timestamp persistito. Questo perché l'entità Documento, nel modello adottato,
     non espone necessariamente un costruttore che consenta di impostare direttamente
     un {@link LocalDateTime}.

     @param teamId identificativo del team
     @return lista dei documenti del team (eventualmente vuota)
     @throws DaoException se si verifica un errore JDBC durante la lettura
     */
    @Override
    public List<Documento> leggiDocumentiPerTeam(int teamId) {
        if (teamId <= 0) {
            return List.of();
        }

        List<Documento> docs = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_TEAM)) {

            ps.setInt(1, teamId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime ts = rs.getTimestamp("Timestamp").toLocalDateTime();
                    String contenuto = rs.getString("Contenuto");
                    Documento d = new Documento(contenuto, ts.toLocalDate());
                    docs.add(d);
                }
            }

            return docs;

        } catch (SQLException e) {
            throw new DaoException("Errore durante la lettura documenti del team", e);
        }
    }
}
