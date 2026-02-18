package implementazionipostgresdao;

import dao.VotoDAO;
import database.ConnessioneDatabase;
import model.Giudice;
import model.Team;
import model.Voto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 Implementazione di {@link VotoDAO} su database PostgreSQL.

 Questa classe gestisce la persistenza e la lettura dei {@link Voto} tramite JDBC.
 Il modello {@link Voto} viene considerato immutabile e composto da:
 - un punteggio numerico;
 - un {@link Giudice} che ha espresso il voto;
 - un {@link Team} a cui il voto è associato.

 Poiché lo schema del database può essere presente in due varianti (nomi quotati con maiuscole
 oppure nomi in minuscolo), le operazioni di INSERT e SELECT tentano prima la query "quoted"
 e, in caso di errore SQL, ripiegano sulla versione "lowercase". Se falliscono entrambe le
 query, l'eccezione viene incapsulata in una {@link DaoException}.

 Le query di lettura eseguono JOIN tra le tabelle Voto, Giudice e Team per ricostruire
 completamente gli oggetti di dominio a partire dalle righe restituite.
 */
public class VotoImplementazionePostgresDAO implements VotoDAO {

    private static final String INSERT_SQL_QUOTED =
            "INSERT INTO \"Voto\"(\"Punteggio\",\"RefGiudice\",\"RefTeam\") VALUES (?, ?, ?)";

    private static final String INSERT_SQL_LOWER =
            "INSERT INTO voto (punteggio, refgiudice, refteam) VALUES (?, ?, ?)";

    private static final String SELECT_VOTI_PER_TEAM_QUOTED =
            "SELECT v.\"Punteggio\" AS punteggio, " +
                    "       g.\"Id\" AS g_id, g.\"Nome\" AS g_nome, g.\"Cognome\" AS g_cognome, g.\"Email\" AS g_email, " +
                    "       t.\"Id\" AS t_id, t.\"Nome\" AS t_nome " +
                    "FROM \"Voto\" v " +
                    "JOIN \"Giudice\" g ON g.\"Id\" = v.\"RefGiudice\" " +
                    "JOIN \"Team\" t ON t.\"Id\" = v.\"RefTeam\" " +
                    "WHERE v.\"RefTeam\" = ?";

    private static final String SELECT_VOTI_PER_TEAM_LOWER =
            "SELECT v.punteggio AS punteggio, " +
                    "       g.id AS g_id, g.nome AS g_nome, g.cognome AS g_cognome, g.email AS g_email, " +
                    "       t.id AS t_id, t.nome AS t_nome " +
                    "FROM voto v " +
                    "JOIN giudice g ON g.id = v.refgiudice " +
                    "JOIN team t ON t.id = v.refteam " +
                    "WHERE v.refteam = ?";

    private static final String SELECT_VOTI_DI_GIUDICE_QUOTED =
            "SELECT v.\"Punteggio\" AS punteggio, " +
                    "       g.\"Id\" AS g_id, g.\"Nome\" AS g_nome, g.\"Cognome\" AS g_cognome, g.\"Email\" AS g_email, " +
                    "       t.\"Id\" AS t_id, t.\"Nome\" AS t_nome " +
                    "FROM \"Voto\" v " +
                    "JOIN \"Giudice\" g ON g.\"Id\" = v.\"RefGiudice\" " +
                    "JOIN \"Team\" t ON t.\"Id\" = v.\"RefTeam\" " +
                    "WHERE g.\"Email\" = ?";

    private static final String SELECT_VOTI_DI_GIUDICE_LOWER =
            "SELECT v.punteggio AS punteggio, " +
                    "       g.id AS g_id, g.nome AS g_nome, g.cognome AS g_cognome, g.email AS g_email, " +
                    "       t.id AS t_id, t.nome AS t_nome " +
                    "FROM voto v " +
                    "JOIN giudice g ON g.id = v.refgiudice " +
                    "JOIN team t ON t.id = v.refteam " +
                    "WHERE g.email = ?";

    /**
     Costruttore di default.

     Non apre connessioni in fase di costruzione: la connessione viene ottenuta
     al momento dell'esecuzione delle singole operazioni JDBC.
     */
    public VotoImplementazionePostgresDAO() {
        // Nessuna connessione da aprire qui: la prendiamo quando serve.
    }

    /**
     Restituisce una connessione JDBC dal singleton {@link ConnessioneDatabase}.

     La classe delega a {@link ConnessioneDatabase} la responsabilità di configurazione
     e gestione della connessione verso PostgreSQL.
     */
    private Connection getConnection() {
        return ConnessioneDatabase.getInstance().getConnection();
    }

    /**
     Salva un voto sul database.

     L'operazione esegue un INSERT nella tabella dei voti utilizzando i riferimenti
     (chiavi esterne) verso giudice e team. Viene tentata prima la query con identificatori
     quotati, e in caso di fallimento si tenta la variante in minuscolo.

     @param voto voto da persistere; non può essere null
     @throws IllegalArgumentException se voto è null
     @throws DaoException se il salvataggio fallisce su entrambe le varianti di query
     */
    @Override
    public void salvaVoto(Voto voto) {
        if (voto == null) {
            throw new IllegalArgumentException("Voto nullo");
        }

        try {
            doInsert(INSERT_SQL_QUOTED, voto);
        } catch (SQLException first) {
            try {
                doInsert(INSERT_SQL_LOWER, voto);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new DaoException("Errore salvataggio voto (DB)", first);
            }
        }
    }

    /**
     Esegue materialmente l'INSERT del voto tramite {@link PreparedStatement}.

     @param sql query di INSERT da eseguire
     @param voto voto da inserire
     @throws SQLException se si verifica un errore JDBC durante l'operazione
     */
    private void doInsert(String sql, Voto voto) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, voto.getPunteggio());
            ps.setInt(2, voto.getGiudice().getId());
            ps.setInt(3, voto.getTeam().getId());
            ps.executeUpdate();
        }
    }

    /**
     Restituisce la lista dei voti associati a un team.

     La lettura ricostruisce oggetti completi {@link Voto} eseguendo una JOIN tra
     Voto, Giudice e Team. L'identificativo del team viene passato come parametro
     di query per evitare SQL injection e per consentire al DB di riutilizzare piani.

     @param idTeam identificativo del team
     @return lista dei voti trovati (eventualmente vuota)
     @throws DaoException se la lettura fallisce su entrambe le varianti di query
     */
    @Override
    public List<Voto> getVotiPerTeam(int idTeam) {
        try {
            return doSelectVoti(SELECT_VOTI_PER_TEAM_QUOTED, idTeam);
        } catch (SQLException first) {
            try {
                return doSelectVoti(SELECT_VOTI_PER_TEAM_LOWER, idTeam);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new DaoException("Errore lettura voti per team (DB)", first);
            }
        }
    }

    /**
     Esegue una SELECT parametrica per recuperare i voti di un determinato team.

     @param sql query SELECT da eseguire
     @param idTeam identificativo del team usato come filtro
     @return lista dei voti ricostruiti dal {@link ResultSet}
     @throws SQLException se si verifica un errore JDBC durante la lettura
     */
    private List<Voto> doSelectVoti(String sql, int idTeam) throws SQLException {
        List<Voto> result = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, idTeam);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapVoto(rs));
                }
            }
        }

        return result;
    }

    /**
     Restituisce tutti i voti espressi da un giudice identificato tramite email.

     Se l'email è null o vuota, viene restituita una lista vuota senza interrogare il DB.
     In caso contrario l'email viene normalizzata con trim e utilizzata come parametro
     per la SELECT (prima "quoted", poi "lower").

     @param emailGiudice email del giudice
     @return lista dei voti espressi dal giudice (eventualmente vuota)
     @throws DaoException se la lettura fallisce su entrambe le varianti di query
     */
    @Override
    public List<Voto> getVotiDiGiudice(String emailGiudice) {
        if (emailGiudice == null || emailGiudice.isBlank()) {
            return List.of();
        }

        String mail = emailGiudice.trim();

        try {
            return doSelectVotiByEmail(SELECT_VOTI_DI_GIUDICE_QUOTED, mail);
        } catch (SQLException first) {
            try {
                return doSelectVotiByEmail(SELECT_VOTI_DI_GIUDICE_LOWER, mail);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new DaoException("Errore lettura voti di un giudice (DB)", first);
            }
        }
    }

    /**
     Esegue una SELECT parametrica per recuperare i voti di un giudice filtrando per email.

     @param sql query SELECT da eseguire
     @param email email del giudice usata come filtro
     @return lista dei voti ricostruiti dal {@link ResultSet}
     @throws SQLException se si verifica un errore JDBC durante la lettura
     */
    private List<Voto> doSelectVotiByEmail(String sql, String email) throws SQLException {
        List<Voto> result = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapVoto(rs));
                }
            }
        }

        return result;
    }

    /**
     Converte la riga corrente del {@link ResultSet} in un oggetto {@link Voto}.

     La query seleziona e aliasa esplicitamente le colonne necessarie per costruire:
     - un {@link Giudice} (id, nome, cognome, email);
     - un {@link Team} (id, nome);
     - il relativo {@link Voto} (punteggio, giudice, team).

     @param rs result set posizionato su una riga valida
     @return voto ricostruito a partire dalla riga corrente
     @throws SQLException se una colonna attesa non è presente o si verifica un errore JDBC
     */
    private static Voto mapVoto(ResultSet rs) throws SQLException {
        Giudice g = new Giudice(
                rs.getInt("g_id"),
                rs.getString("g_nome"),
                rs.getString("g_cognome"),
                rs.getString("g_email")
        );

        Team t = new Team(
                rs.getInt("t_id"),
                rs.getString("t_nome")
        );

        return new Voto(rs.getInt("punteggio"), g, t);
    }
}
