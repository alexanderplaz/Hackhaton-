package implementazionipostgresdao;

import dao.OrganizzatoreDAO;
import database.ConnessioneDatabase;
import model.Organizzatore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 Implementazione di {@link OrganizzatoreDAO} su database PostgreSQL.

 La classe si occupa esclusivamente della persistenza dell'entità {@link Organizzatore}.
 Nel modello di dominio l'organizzatore non possiede un attributo email; di conseguenza,
 l'operazione di ricerca per email prevista dall'interfaccia DAO non è applicabile
 e viene implementata restituendo null.

 Come per le altre implementazioni DAO, sono supportate due varianti di schema:
 - identificatori quotati e case-sensitive (es. "Organizzatore", "Id", "Nome");
 - identificatori in minuscolo (es. organizzatore, id, nome).

 In caso di errore SQL sulla query principale (schema quotato), viene tentata la variante
 in minuscolo. Se entrambe falliscono, l'eccezione viene incapsulata in una {@link DaoException}.
 */
public class OrganizzatoreImplementazionePostgresDAO implements OrganizzatoreDAO {

    private static final String INSERT_SQL_QUOTED =
            "INSERT INTO \"Organizzatore\"(\"Id\",\"Nome\",\"Cognome\",\"Password\") VALUES (?, ?, ?, ?)";

    private static final String INSERT_SQL_LOWER =
            "INSERT INTO organizzatore (id, nome, cognome, password) VALUES (?, ?, ?, ?)";

    /**
     Costruttore di default.

     Non viene aperta alcuna connessione in fase di costruzione. La connessione JDBC
     viene richiesta al momento dell'esecuzione delle operazioni di persistenza.
     */
    public OrganizzatoreImplementazionePostgresDAO() {
        // Niente try/catch SQLException: getConnection() non lancia SQLException checked.
    }

    /**
     Restituisce una connessione JDBC fornita dal singleton {@link ConnessioneDatabase}.

     La gestione della configurazione e del ciclo di vita della connessione è delegata
     alla componente infrastrutturale.
     */
    private Connection getConnection() {
        return ConnessioneDatabase.getInstance().getConnection();
    }

    /**
     Registra un nuovo organizzatore nel database.

     L'operazione esegue un INSERT parametrico nella tabella degli organizzatori,
     valorizzando id, nome, cognome e password. In caso di schema quotato/non quotato
     vengono tentate le rispettive query in fallback.

     @param organizzatore entità da registrare; non può essere null
     @throws IllegalArgumentException se organizzatore è null
     @throws DaoException se l'inserimento fallisce su entrambe le varianti di schema
     */
    @Override
    public void registraOrganizzatore(Organizzatore organizzatore) {
        if (organizzatore == null) {
            throw new IllegalArgumentException("Organizzatore nullo");
        }

        try {
            doInsert(INSERT_SQL_QUOTED, organizzatore);
        } catch (SQLException first) {
            try {
                doInsert(INSERT_SQL_LOWER, organizzatore);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new DaoException("Errore registrazione organizzatore (DB)", first);
            }
        }
    }

    /**
     Esegue materialmente l'INSERT dell'organizzatore.

     @param sql query di inserimento da utilizzare
     @param o organizzatore da persistere
     @throws SQLException se si verifica un errore JDBC durante l'operazione
     */
    private void doInsert(String sql, Organizzatore o) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, o.getId());
            ps.setString(2, o.getNome());
            ps.setString(3, o.getCognome());
            ps.setString(4, o.getPassword());
            ps.executeUpdate();
        }
    }

    /**
     Metodo non applicabile.

     L'entità {@link Organizzatore} non prevede un attributo email nel modello
     né nello schema di persistenza; pertanto la ricerca per email non è definita
     e il metodo restituisce sempre null.

     @param email parametro non utilizzato
     @return sempre null
     */
    @Override
    public Organizzatore leggiOrganizzatorePerEmail(String email) {
        return null;
    }
}
