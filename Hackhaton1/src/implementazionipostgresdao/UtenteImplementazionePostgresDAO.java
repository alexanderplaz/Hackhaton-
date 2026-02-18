package implementazionipostgresdao;

import dao.UtenteDAO;
import database.ConnessioneDatabase;
import model.Utente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 Implementazione di {@link UtenteDAO} su database PostgreSQL.

 La tabella di persistenza degli utenti memorizza i campi fondamentali dell'entità {@link Utente}:
 Id, Nome, Cognome, Email.

 La classe supporta due varianti di schema:
 - schema con identificatori quotati e case-sensitive (es. "Utente", "Id", "Nome");
 - schema con identificatori in minuscolo (es. utente, id, nome).

 Per ogni operazione viene tentata prima la query relativa allo schema quotato e, in caso di errore SQL,
 viene tentata la corrispondente query per lo schema in minuscolo. Se entrambe falliscono, l'errore viene
 incapsulato in una {@link DaoException}.
 */
public class UtenteImplementazionePostgresDAO implements UtenteDAO {

    private static final String INSERT_SQL_QUOTED =
            "INSERT INTO \"Utente\"(\"Id\",\"Nome\",\"Cognome\",\"Email\") VALUES (?, ?, ?, ?)";
    private static final String SELECT_BY_EMAIL_QUOTED =
            "SELECT \"Id\" AS id, \"Nome\" AS nome, \"Cognome\" AS cognome, \"Email\" AS email " +
                    "FROM \"Utente\" WHERE \"Email\" = ?";
    private static final String SELECT_ALL_QUOTED =
            "SELECT \"Id\" AS id, \"Nome\" AS nome, \"Cognome\" AS cognome, \"Email\" AS email " +
                    "FROM \"Utente\" ORDER BY \"Id\"";

    private static final String DELETE_BY_ID_QUOTED =
            "DELETE FROM \"Utente\" WHERE \"Id\" = ?";

    private static final String INSERT_SQL_LOWER =
            "INSERT INTO utente(id, nome, cognome, email) VALUES (?, ?, ?, ?)";
    private static final String SELECT_BY_EMAIL_LOWER =
            "SELECT id, nome, cognome, email FROM utente WHERE email = ?";
    private static final String SELECT_ALL_LOWER =
            "SELECT id, nome, cognome, email FROM utente ORDER BY id";

    private static final String DELETE_BY_ID_LOWER =
            "DELETE FROM utente WHERE id = ?";

    /**
     Costruttore di default.

     Non viene instaurata alcuna connessione in fase di costruzione: la connessione JDBC
     viene richiesta al singleton {@link ConnessioneDatabase} quando un metodo DAO viene invocato.
     */
    public UtenteImplementazionePostgresDAO() {
        // Niente fail-fast con SQLException: getConnection() non lancia checked SQLException.
        // Se manca DB, verrà lanciata DatabaseException (unchecked) quando serve.
    }

    /**
     Restituisce una connessione JDBC gestita da {@link ConnessioneDatabase}.

     La gestione del ciclo di vita della connessione (configurazione, apertura, eventuale riuso)
     è delegata alla classe dedicata all'infrastruttura.
     */
    private Connection getConnection() {
        return ConnessioneDatabase.getInstance().getConnection();
    }

    /**
     Registra un nuovo utente nel database.

     L'operazione inserisce una nuova riga nella tabella degli utenti utilizzando una query parametrica
     (PreparedStatement) per evitare SQL injection e garantire corretta gestione dei tipi.

     @param utente utente da registrare; non può essere null
     @throws IllegalArgumentException se utente è null
     @throws DaoException se l'inserimento fallisce su entrambe le varianti di schema
     */
    @Override
    public void registraUtente(Utente utente) {
        if (utente == null) {
            throw new IllegalArgumentException("Utente nullo");
        }

        try {
            doInsert(INSERT_SQL_QUOTED, utente);
        } catch (SQLException first) {
            try {
                doInsert(INSERT_SQL_LOWER, utente);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new DaoException("Errore registrazione utente (DB)", first);
            }
        }
    }

    /**
     Esegue materialmente l'INSERT dell'utente.

     @param sql query di inserimento da utilizzare
     @param utente entità da persistere
     @throws SQLException se si verifica un errore JDBC durante l'operazione
     */
    private void doInsert(String sql, Utente utente) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, utente.getId());
            ps.setString(2, utente.getNome());
            ps.setString(3, utente.getCognome());
            ps.setString(4, utente.getEmail());
            ps.executeUpdate();
        }
    }

    /**
     Legge un utente a partire dalla sua email.

     Se l'email è null o composta solo da spazi, viene restituito null senza effettuare accesso al DB.
     In caso contrario l'email viene normalizzata con trim e utilizzata come parametro della query.

     @param email email dell'utente da cercare
     @return l'utente corrispondente, oppure null se non presente
     @throws DaoException se la lettura fallisce su entrambe le varianti di schema
     */
    @Override
    public Utente leggiUtentePerEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        String mail = email.trim();

        try {
            return doSelectByEmail(SELECT_BY_EMAIL_QUOTED, mail);
        } catch (SQLException first) {
            try {
                return doSelectByEmail(SELECT_BY_EMAIL_LOWER, mail);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new DaoException("Errore lettura utente per email (DB)", first);
            }
        }
    }

    /**
     Esegue una SELECT parametrica per ottenere un utente tramite email.

     @param sql query di selezione da utilizzare
     @param email email usata come filtro (già normalizzata)
     @return utente trovato oppure null se non esiste alcuna riga corrispondente
     @throws SQLException se si verifica un errore JDBC durante la lettura
     */
    private Utente doSelectByEmail(String sql, String email) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUtente(rs);
            }
        }
    }

    /**
     Restituisce l'elenco completo degli utenti presenti in archivio.

     L'ordinamento è demandato al database tramite ORDER BY sull'identificativo.

     @return lista di utenti (eventualmente vuota)
     @throws DaoException se la lettura fallisce su entrambe le varianti di schema
     */
    @Override
    public List<Utente> leggiTuttiUtenti() {
        try {
            return doSelectAll(SELECT_ALL_QUOTED);
        } catch (SQLException first) {
            try {
                return doSelectAll(SELECT_ALL_LOWER);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new DaoException("Errore lettura lista utenti (DB)", first);
            }
        }
    }

    /**
     Elimina un utente a partire dal suo identificativo.

     Se l'operazione di delete non rimuove alcuna riga, viene sollevata una {@link DaoException}
     per segnalare che non esiste un utente con l'id indicato.

     @param id identificativo dell'utente
     @throws IllegalArgumentException se id non è positivo
     @throws DaoException se l'eliminazione fallisce (errore DB) oppure se l'utente non è presente
     */
    @Override
    public void eliminaUtentePerId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Id utente non valido");
        }

        try {
            doDelete(DELETE_BY_ID_QUOTED, id);
        } catch (SQLException first) {
            try {
                doDelete(DELETE_BY_ID_LOWER, id);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new DaoException("Errore cancellazione utente (DB)", first);
            }
        }
    }

    /**
     Esegue materialmente la DELETE dell'utente.

     @param sql query di cancellazione da utilizzare
     @param id identificativo dell'utente da eliminare
     @throws SQLException se si verifica un errore JDBC durante l'operazione
     @throws DaoException se nessuna riga viene eliminata (utente inesistente)
     */
    private void doDelete(String sql, int id) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DaoException("Utente non trovato (id=" + id + ")");
            }
        }
    }

    /**
     Esegue una SELECT per ottenere tutti gli utenti.

     @param sql query di selezione da utilizzare
     @return lista di utenti ricostruiti dal {@link ResultSet}
     @throws SQLException se si verifica un errore JDBC durante la lettura
     */
    private List<Utente> doSelectAll(String sql) throws SQLException {
        List<Utente> out = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(mapUtente(rs));
            }
        }

        return out;
    }

    /**
     Converte la riga corrente del {@link ResultSet} in un oggetto {@link Utente}.

     Le colonne vengono lette tramite gli alias "id", "nome", "cognome", "email" in modo da
     rendere uniforme la mappatura tra schema quotato e schema in minuscolo.

     @param rs result set posizionato su una riga valida
     @return utente ricostruito a partire dalla riga corrente
     @throws SQLException se si verifica un errore JDBC o una colonna attesa non è presente
     */
    private static Utente mapUtente(ResultSet rs) throws SQLException {
        return new Utente(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("cognome"),
                rs.getString("email")
        );
    }
}
