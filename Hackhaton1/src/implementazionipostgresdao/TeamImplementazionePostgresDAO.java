package implementazionipostgresdao;

import dao.TeamDAO;
import database.ConnessioneDatabase;
import model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 Implementazione di {@link TeamDAO} su database PostgreSQL.

 La persistenza dei {@link Team} avviene tramite JDBC e supporta due varianti di schema:
 - schema con identificatori quotati e case-sensitive (es. "Team", "Id", "Nome", "RefHackathon");
 - schema con identificatori in minuscolo (es. team, id, nome, refhackathon).

 Per ogni operazione viene tentata prima la query relativa allo schema quotato; in caso di errore SQL
 viene utilizzata la query di fallback per lo schema in minuscolo. Se entrambe falliscono,
 l'errore viene incapsulato in una {@link DaoException}.

 La classe mantiene opzionalmente un riferimento all'hackathon corrente (chiave esterna RefHackathon)
 tramite il campo {@link #refHackathonId}. Se tale valore è impostato, i nuovi team creati vengono
 collegati a quell'hackathon; in caso contrario la colonna FK viene valorizzata a null.
 */
public class TeamImplementazionePostgresDAO implements TeamDAO {

    private static final String INSERT_SQL_QUOTED =
            "INSERT INTO \"Team\"(\"Id\",\"Nome\",\"RefHackathon\") VALUES (?, ?, ?)";
    private static final String SELECT_BY_HACKATHON_QUOTED =
            "SELECT \"Id\" AS id, \"Nome\" AS nome " +
                    "FROM \"Team\" WHERE \"RefHackathon\" = ? ORDER BY \"Id\"";

    private static final String INSERT_SQL_LOWER =
            "INSERT INTO team (id, nome, refhackathon) VALUES (?, ?, ?)";
    private static final String SELECT_BY_HACKATHON_LOWER =
            "SELECT id, nome FROM team WHERE refhackathon = ? ORDER BY id";

    /**
     Identificativo dell'hackathon a cui collegare i nuovi team tramite la FK RefHackathon.

     Se null, la creazione del team imposta la colonna RefHackathon a null.
     */
    private Integer refHackathonId;

    /**
     Costruttore di default.

     Inizializza l'istanza senza associare un hackathon corrente; in tale configurazione,
     i nuovi team creati non vengono collegati ad alcun hackathon (RefHackathon = null).
     */
    public TeamImplementazionePostgresDAO() {
        this(null);
    }

    /**
     Costruttore che consente di impostare l'hackathon corrente.

     @param refHackathonId identificativo dell'hackathon da usare come FK per i nuovi team
     */
    public TeamImplementazionePostgresDAO(Integer refHackathonId) {
        this.refHackathonId = refHackathonId;
        // Niente connessione nel costruttore: la prendiamo quando serve.
    }

    /**
     Aggiorna l'identificativo dell'hackathon corrente.

     Questo metodo è pensato per essere invocato dal Controller dopo la creazione dell'hackathon
     nel database, così da poter collegare correttamente i team successivi tramite la FK RefHackathon.

     @param refHackathonId nuovo identificativo dell'hackathon corrente (può essere null)
     */
    public void setRefHackathonId(Integer refHackathonId) {
        this.refHackathonId = refHackathonId;
    }

    /**
     Restituisce una connessione JDBC gestita da {@link ConnessioneDatabase}.

     La responsabilità di configurare e fornire la connessione verso PostgreSQL è delegata
     alla componente infrastrutturale dedicata.
     */
    private Connection getConnection() {
        return ConnessioneDatabase.getInstance().getConnection();
    }

    /**
     Crea (persiste) un team nel database.

     L'operazione esegue un INSERT parametrico nella tabella Team e, se {@link #refHackathonId}
     è valorizzato, collega il team all'hackathon corrente tramite la colonna RefHackathon.
     In caso di schema quotato/non quotato vengono tentate le rispettive query in fallback.

     @param team team da creare; non può essere null
     @throws IllegalArgumentException se team è null
     @throws DaoException se l'inserimento fallisce su entrambe le varianti di schema
     */
    @Override
    public void creaTeam(Team team) {
        if (team == null) {
            throw new IllegalArgumentException("Team nullo");
        }

        try {
            doInsert(INSERT_SQL_QUOTED, team);
        } catch (SQLException first) {
            try {
                doInsert(INSERT_SQL_LOWER, team);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new DaoException("Errore creazione team (DB)", first);
            }
        }
    }

    /**
     Esegue materialmente l'INSERT del team.

     @param sql query di inserimento da utilizzare
     @param team entità da persistere
     @throws SQLException se si verifica un errore JDBC durante l'operazione
     */
    private void doInsert(String sql, Team team) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, team.getId());
            ps.setString(2, team.getNome());

            if (refHackathonId == null) {
                ps.setObject(3, null);
            } else {
                ps.setInt(3, refHackathonId);
            }

            ps.executeUpdate();
        }
    }

    /**
     Restituisce la lista dei team associati a un determinato hackathon.

     La selezione avviene tramite filtro sulla chiave esterna RefHackathon. Gli oggetti {@link Team}
     vengono ricostruiti a partire dalle colonne selezionate (id e nome). L'ordinamento è demandato
     al database tramite ORDER BY.

     @param hackathonId identificativo dell'hackathon
     @return lista di team partecipanti all'hackathon (eventualmente vuota)
     @throws DaoException se la lettura fallisce su entrambe le varianti di schema
     */
    @Override
    public List<Team> getTeamsDelHackathon(int hackathonId) {
        try {
            return doSelectByHackathon(SELECT_BY_HACKATHON_QUOTED, hackathonId);
        } catch (SQLException first) {
            try {
                return doSelectByHackathon(SELECT_BY_HACKATHON_LOWER, hackathonId);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new DaoException("Errore lettura team dell'hackathon (DB)", first);
            }
        }
    }

    /**
     Esegue una SELECT parametrica per ottenere i team di un hackathon.

     @param sql query di selezione da utilizzare
     @param hackathonId identificativo dell'hackathon usato come filtro
     @return lista di team ricostruiti dal {@link ResultSet}
     @throws SQLException se si verifica un errore JDBC durante la lettura
     */
    private List<Team> doSelectByHackathon(String sql, int hackathonId) throws SQLException {
        List<Team> out = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, hackathonId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Team(
                            rs.getInt("id"),
                            rs.getString("nome")
                    ));
                }
            }
        }

        return out;
    }
}
