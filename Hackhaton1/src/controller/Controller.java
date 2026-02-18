package controller;

import dao.DocumentoDAO;
import dao.GiudiceDAO;
import dao.OrganizzatoreDAO;
import dao.UtenteDAO;
import dao.VotoDAO;
import database.ConnessioneDatabase;
import implementazionipostgresdao.DocumentoImplementazionePostgresDAO;
import implementazionipostgresdao.GiudiceImplementazionePostgresDAO;
import implementazionipostgresdao.OrganizzatoreImplementazionePostgresDAO;
import implementazionipostgresdao.TeamImplementazionePostgresDAO;
import implementazionipostgresdao.UtenteImplementazionePostgresDAO;
import implementazionipostgresdao.VotoImplementazionePostgresDAO;
import model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 Controller applicativo dell’applicazione (strato Control del pattern BCE - Boundary/Control/Entity).

 Responsabilità principali:
 - Coordinare l’interazione tra Boundary (GUI) ed Entity (modello di dominio), fornendo metodi di alto livello
 corrispondenti alle funzionalità del sistema.
 - Applicare vincoli e regole applicative trasversali (es. abilitazioni “organizzatore”, requisiti minimi per gara).
 - Gestire la persistenza su database delegando ai DAO (Data Access Object) le operazioni CRUD.

 Strategia di consistenza (rollback applicativo):
 - Per molte operazioni si applica una sequenza “aggiorno stato in memoria -> persisto su DB”.
 - Se l’operazione di persistenza fallisce, il controller ripristina lo stato del modello (annullando l’azione)
 per evitare divergenze tra memoria e database.

 Nota: i controlli “di dominio” (capienza, vincoli temporali interni all’hackathon, ecc.) sono in larga parte
 delegati alle classi del package model; il controller aggiunge vincoli di orchestrazione e di flusso applicativo.
 */
public class Controller {

    /**
     DAO responsabile delle operazioni di persistenza sugli utenti.
     */
    private final UtenteDAO utenteDAO;

    /**
     DAO responsabile delle operazioni di persistenza sui giudici.
     */
    private final GiudiceDAO giudiceDAO;

    /**
     DAO dei team (implementazione concreta PostgreSQL).

     È mantenuto come tipo concreto perché espone una funzionalità aggiuntiva non presente nell’interfaccia:
     {@code setRefHackathonId}, necessaria per gestire il riferimento dell’hackathon nel database.
     */
    private final TeamImplementazionePostgresDAO teamDAO;

    /**
     DAO responsabile delle operazioni di persistenza sui voti finali.
     */
    private final VotoDAO votoDAO;

    /**
     DAO responsabile delle operazioni di persistenza sugli organizzatori.
     */
    private final OrganizzatoreDAO organizzatoreDAO;

    /**
     DAO responsabile delle operazioni di persistenza sui documenti.
     */
    private final DocumentoDAO documentoDAO;

    /**
     Hackathon corrente gestito dal controller.

     L’oggetto viene inizializzato tramite {@link #creaHackathon(String, CapitaleEuropa, LocalDate, LocalDate, int, int, Organizzatore)}.
     La maggior parte dei metodi richiede che questo campo sia valorizzato.
     */
    private Hackathon hackathon;

    /**
     Vincolo applicativo: numero massimo di giudici registrabili.
     */
    private static final int MAX_GIUDICI = 3;

    /**
     Vincolo applicativo: numero minimo di team richiesto per abilitare le funzionalità di gara.
     */
    private static final int MIN_TEAM_PER_INIZIARE_GARA = 3;

    /**
     Flag applicativo che rappresenta l’apertura delle registrazioni da parte dell’organizzatore.

     Nota: il flag viene sincronizzato rispetto alla data corrente tramite {@link #syncRegistrazioniConData(LocalDate)}
     per evitare stati incoerenti.
     */
    private boolean registrazioniAperteDaOrganizzatore = false;

    /**
     Flag applicativo che rappresenta l’abilitazione dell’invio documenti da parte dell’organizzatore.

     Nota: il flag viene sincronizzato rispetto alla data corrente tramite {@link #syncInvioDocumentiConData(LocalDate)}
     per evitare abilitazioni fuori periodo.
     */
    private boolean invioDocumentiAbilitatoDaOrganizzatore = false;

    /**
     Collezione dei voti finali registrati (uno per coppia giudice-team).

     Viene utilizzata per generare report e calcolare la classifica finale.
     */
    private final List<Voto> votiFinali = new ArrayList<>();

    /**
     Mappa che associa a ciascun team la lista dei punteggi ottenuti dai documenti caricati.

     La struttura supporta:
     - conteggio documenti consegnati,
     - somma e media dei progressi,
     - calcolo dello score finale (combinazione progressi + voti finali).
     */
    private final Map<Team, List<Integer>> punteggiDocumenti = new HashMap<>();

    /**
     Generatore casuale utilizzato per simulare voti e valutazioni.
     */
    private final Random random = new Random();

    /**
     Costruisce il controller inizializzando le implementazioni DAO PostgreSQL.

     L’hackathon non viene creato automaticamente: la creazione del dominio è demandata al metodo
     {@link #creaHackathon(String, CapitaleEuropa, LocalDate, LocalDate, int, int, Organizzatore)}.
     */
    public Controller() {
        this.utenteDAO = new UtenteImplementazionePostgresDAO();
        this.giudiceDAO = new GiudiceImplementazionePostgresDAO();
        this.teamDAO = new TeamImplementazionePostgresDAO();
        this.votoDAO = new VotoImplementazionePostgresDAO();
        this.organizzatoreDAO = new OrganizzatoreImplementazionePostgresDAO();
        this.documentoDAO = new DocumentoImplementazionePostgresDAO();
    }

    /**
     Eccezione applicativa specifica del controller.

     Scopo:
     - fornire messaggi più chiari e contestualizzati rispetto a eccezioni generiche,
     - incapsulare errori infrastrutturali (in particolare JDBC/DB) quando vengono gestiti a livello di Controller.
     */
    public static class ControllerException extends RuntimeException {
        public ControllerException(String message, Throwable cause) {
            super(message, cause);
        }

        public ControllerException(String message) {
            super(message);
        }
    }

    /**
     Inizializza l’hackathon gestito dal controller e tenta una persistenza “best-effort” sul database.

     Operazioni eseguite:
     1) Creazione dell’istanza {@link model.Hackathon} in memoria.
     2) Tentativo di registrazione dell’organizzatore su DB. Eventuali errori non bloccano l’avvio del sistema
     (ad esempio duplicati già presenti).
     3) Tentativo di garantire l’esistenza di una riga Hackathon su DB tramite {@link #ensureHackathonRow(String)} e,
     se disponibile, impostazione del riferimento dell’hackathon sul DAO dei team.

     @param titolo titolo dell’hackathon.
     @param sede sede dell’evento (capitale europea).
     @param inizio data di inizio dell’hackathon.
     @param fine data di fine dell’hackathon.
     @param maxPartecipanti numero massimo di partecipanti registrabili.
     @param maxTeamSize dimensione massima di un team.
     @param organizzatore organizzatore dell’evento.
     */
    public void creaHackathon(String titolo, CapitaleEuropa sede, LocalDate inizio, LocalDate fine,
                              int maxPartecipanti, int maxTeamSize, Organizzatore organizzatore) {
        this.hackathon = new Hackathon(titolo, sede, inizio, fine, maxPartecipanti, maxTeamSize, organizzatore);

        try {
            organizzatoreDAO.registraOrganizzatore(organizzatore);
        } catch (RuntimeException ex) {
            // Best-effort: duplicati o errori non bloccanti non impediscono l'avvio dell'applicazione.
        }

        try {
            Integer hackathonDbId = ensureHackathonRow(titolo);
            teamDAO.setRefHackathonId(hackathonDbId);
        } catch (RuntimeException ex) {
            teamDAO.setRefHackathonId(null);
        }
    }

    /**
     Restituisce l’hackathon corrente gestito dal controller.

     @return hackathon corrente.
     @throws IllegalStateException se l’hackathon non è stato ancora inizializzato.
     */
    public Hackathon getHackathon() {
        requireHackathon();
        return hackathon;
    }

    /**
     Garantisce (se possibile) l’esistenza nel database di una riga Hackathon associata al titolo fornito.

     Il metodo restituisce l’identificativo della riga trovata o appena inserita.
     Per aumentare la compatibilità con script SQL differenti, supporta due varianti di schema:
     - schema “quotato” (identificatori tra doppi apici e case-sensitive),
     - schema “lowercase” (identificatori non quotati, comportamento SQL standard).

     @param titolo titolo dell’hackathon.
     @return id della riga hackathon nel DB, oppure null se il titolo è nullo/blank o se non determinabile.
     @throws ControllerException se si verifica un errore JDBC durante accesso al DB.
     */
    private Integer ensureHackathonRow(String titolo) {
        if (titolo == null || titolo.isBlank()) return null;
        String t = titolo.trim();

        try {
            Connection c = ConnessioneDatabase.getInstance().getConnection();
            return tryEnsureHackathonRow(c, t);
        } catch (SQLException e) {
            throw new ControllerException("Errore creazione/lettura hackathon nel DB", e);
        }
    }

    /**
     Tenta l’operazione di find/insert della riga Hackathon applicando due strategie in sequenza:
     1) schema quotato,
     2) schema lowercase.

     Se entrambe le strategie falliscono, propaga l’eccezione SQL arricchita con suppressed.

     @param c connessione JDBC già aperta.
     @param titolo titolo dell’hackathon.
     @return id della riga hackathon trovata o inserita.
     @throws SQLException se entrambe le strategie falliscono.
     */
    private Integer tryEnsureHackathonRow(Connection c, String titolo) throws SQLException {
        SQLException first = null;

        try {
            return ensureHackathonRowQuoted(c, titolo);
        } catch (SQLException e) {
            first = e;
        }

        try {
            return ensureHackathonRowLower(c, titolo);
        } catch (SQLException e) {
            e.addSuppressed(first);
            throw e;
        }
    }

    /**
     Strategia di find/insert per schema quotato.

     Implementazione:
     - SELECT per titolo; se esiste restituisce l’id.
     - In assenza, INSERT con RETURNING id.

     @param c connessione JDBC già aperta.
     @param titolo titolo dell’hackathon.
     @return id della riga hackathon trovata o inserita.
     @throws SQLException se l’operazione JDBC fallisce.
     */
    private Integer ensureHackathonRowQuoted(Connection c, String titolo) throws SQLException {
        String sel = "SELECT \"Id\" FROM \"Hackathon\" WHERE \"Titolo\" = ? ORDER BY \"Id\" DESC LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sel)) {
            ps.setString(1, titolo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        String ins = "INSERT INTO \"Hackathon\"(\"Titolo\") VALUES (?) RETURNING \"Id\"";
        try (PreparedStatement ps = c.prepareStatement(ins)) {
            ps.setString(1, titolo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return null;
    }

    /**
     Strategia di find/insert per schema lowercase.

     Implementazione:
     - SELECT per titolo; se esiste restituisce l’id.
     - In assenza, INSERT con RETURNING id.

     @param c connessione JDBC già aperta.
     @param titolo titolo dell’hackathon.
     @return id della riga hackathon trovata o inserita.
     @throws SQLException se l’operazione JDBC fallisce.
     */
    private Integer ensureHackathonRowLower(Connection c, String titolo) throws SQLException {
        String sel = "SELECT id FROM hackathon WHERE titolo = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sel)) {
            ps.setString(1, titolo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        String ins = "INSERT INTO hackathon(titolo) VALUES (?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(ins)) {
            ps.setString(1, titolo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return null;
    }

    /**
     Apre le registrazioni lato organizzatore.

     Vincoli applicati:
     - devono essere presenti esattamente (o almeno) {@link #MAX_GIUDICI} giudici registrati;
     - la data deve rientrare nella finestra di registrazione definita dal modello;
     - prima di aprire, lo stato viene sincronizzato tramite {@link #syncRegistrazioniConData(LocalDate)}.

     @param oggi data corrente utilizzata per verificare la finestra temporale.
     @throws IllegalStateException se i vincoli non sono rispettati.
     */
    public void apriRegistrazioni(LocalDate oggi) {
        requireHackathon();
        syncRegistrazioniConData(oggi);

        int nGiudici = hackathon.getGiudici().size();
        if (nGiudici < MAX_GIUDICI) {
            throw new IllegalStateException(
                    "Non puoi aprire le registrazioni finche' non registri " + MAX_GIUDICI +
                            " giudici (attuali: " + nGiudici + ")");
        }

        if (!hackathon.isRegistrazioneConsentita(oggi)) {
            throw new IllegalStateException(
                    "Puoi aprire le registrazioni solo nel periodo: "
                            + hackathon.getDataInizioRegistrazioni() + " -> " + hackathon.getDataChiusuraRegistrazioni());
        }
        registrazioniAperteDaOrganizzatore = true;
    }

    /**
     Chiude esplicitamente le registrazioni lato organizzatore, disabilitando il relativo flag applicativo.
     */
    public void chiudiRegistrazioni() {
        registrazioniAperteDaOrganizzatore = false;
    }

    /**
     Indica se le registrazioni risultano aperte lato organizzatore.

     @return true se il flag di apertura registrazioni è attivo, false altrimenti.
     */
    public boolean isRegistrazioniAperteDaOrganizzatore() {
        return registrazioniAperteDaOrganizzatore;
    }

    /**
     Sincronizza lo stato dell’apertura registrazioni rispetto alla data corrente.

     Se la data non è all’interno della finestra di registrazione, il flag viene forzato a false.

     @param oggi data corrente.
     @throws IllegalArgumentException se oggi è null.
     */
    public void syncRegistrazioniConData(LocalDate oggi) {
        requireHackathon();
        if (oggi == null) throw new IllegalArgumentException("Data nulla");
        if (!hackathon.isRegistrazioneConsentita(oggi)) {
            registrazioniAperteDaOrganizzatore = false;
        }
    }

    /**
     Verifica se è possibile registrare un ulteriore giudice senza violare il vincolo massimo.

     @return true se il numero di giudici attuali è minore di {@link #MAX_GIUDICI}, false altrimenti.
     */
    public boolean canAddJudge() {
        requireHackathon();
        return hackathon.getGiudici().size() < MAX_GIUDICI;
    }

    /**
     Sincronizza lo stato dell’abilitazione invio documenti rispetto alla data corrente.

     Se la data non è durante l’hackathon, il flag viene forzato a false.

     @param oggi data corrente.
     @throws IllegalArgumentException se oggi è null.
     */
    public void syncInvioDocumentiConData(LocalDate oggi) {
        requireHackathon();
        if (oggi == null) throw new IllegalArgumentException("Data nulla");
        if (!hackathon.isDuranteHackathon(oggi)) {
            invioDocumentiAbilitatoDaOrganizzatore = false;
        }
    }

    /**
     Abilita l’invio dei documenti lato organizzatore.

     Vincoli applicati:
     - la data deve essere durante l’hackathon;
     - devono essere presenti almeno {@link #MAX_GIUDICI} giudici;
     - devono essere presenti almeno {@link #MIN_TEAM_PER_INIZIARE_GARA} team;
     - prima di abilitare, lo stato viene sincronizzato tramite {@link #syncInvioDocumentiConData(LocalDate)}.

     @param oggi data corrente.
     @throws IllegalStateException se i vincoli non sono rispettati.
     */
    public void abilitaInvioDocumenti(LocalDate oggi) {
        requireHackathon();
        syncInvioDocumentiConData(oggi);

        if (!hackathon.isDuranteHackathon(oggi)) {
            throw new IllegalStateException(
                    "Puoi abilitare l'invio documenti solo durante l'hackathon (" +
                            hackathon.getDataInizio() + " -> " + hackathon.getDataFine() + ")");
        }

        requireMinGiudici();
        requireMinTeam();

        invioDocumentiAbilitatoDaOrganizzatore = true;
    }



    /**
     Disabilita esplicitamente l’invio documenti lato organizzatore, disattivando il relativo flag applicativo.
     */
    public void disabilitaInvioDocumenti() {
        invioDocumentiAbilitatoDaOrganizzatore = false;
    }

    /**
     Indica se l’invio documenti risulta abilitato lato organizzatore.

     @return true se il flag è attivo, false altrimenti.
     */
    public boolean isInvioDocumentiAbilitatoDaOrganizzatore() {
        return invioDocumentiAbilitatoDaOrganizzatore;
    }

    /**
     Aggiunge un giudice all’hackathon, rispettando il vincolo massimo e persistendo l’operazione su DB.

     Strategia:
     - aggiunge il giudice al modello,
     - tenta la registrazione sul DB tramite DAO,
     - in caso di errore, rimuove il giudice dal modello (rollback applicativo).

     @param g giudice da aggiungere.
     @throws IllegalStateException se è già stato raggiunto il massimo numero di giudici.
     @throws RuntimeException se la persistenza fallisce (propagata dopo rollback).
     */
    public void aggiungiGiudice(Giudice g) {
        requireHackathon();
        if (hackathon.getGiudici().size() >= MAX_GIUDICI) {
            throw new IllegalStateException("Numero massimo di giudici raggiunto (" + MAX_GIUDICI + ")");
        }
        hackathon.aggiungiGiudice(g);

        try {
            giudiceDAO.registraGiudice(g);
        } catch (RuntimeException ex) {
            hackathon.rimuoviGiudice(g);
            throw ex;
        }
    }

    /**
     Restituisce la lista dei giudici associati all’hackathon corrente.

     Nota: la lista restituita è quella del modello; eventuali modifiche esterne inciderebbero sullo stato interno.

     @return lista dei giudici registrati.
     @throws IllegalStateException se l’hackathon non è inizializzato.
     */
    public List<Giudice> getGiudici() {
        requireHackathon();
        return hackathon.getGiudici();
    }

    /**
     Elimina un giudice identificato dal suo id, aggiornando sia database sia stato in memoria.

     Operazioni eseguite:
     1) validazione dell’id,
     2) ricerca del giudice nel modello,
     3) eliminazione su DB tramite DAO,
     4) rimozione dal modello,
     5) rimozione di eventuali voti finali associati a tale giudice (pulizia dello stato applicativo).

     @param id identificativo del giudice.
     @throws IllegalArgumentException se id <= 0.
     @throws IllegalStateException se il giudice non esiste nel modello.
     */
    public void eliminaGiudicePerId(int id) {
        requireHackathon();

        if (id <= 0) {
            throw new IllegalArgumentException("Id giudice non valido");
        }

        Giudice target = null;
        for (Giudice g : hackathon.getGiudici()) {
            if (g.getId() == id) {
                target = g;
                break;
            }
        }

        if (target == null) {
            throw new IllegalStateException("Giudice non trovato (id=" + id + ")");
        }

        giudiceDAO.eliminaGiudicePerId(id);
        hackathon.rimuoviGiudice(target);

        final int idFinal = id;
        votiFinali.removeIf(v -> v.getGiudice() != null && v.getGiudice().getId() == idFinal);
    }

    /**
     Registra un utente all’hackathon in base alla data corrente e allo stato delle registrazioni.

     Vincoli applicati:
     - le registrazioni devono essere aperte dall’organizzatore;
     - i vincoli temporali e di capienza sono delegati al modello.

     Strategia:
     - registra l’utente nel modello,
     - persiste l’utente su DB tramite DAO,
     - in caso di errore su DB, annulla la registrazione nel modello (rollback).

     @param u utente da registrare.
     @param oggi data corrente.
     @throws IllegalStateException se le registrazioni non sono aperte.
     @throws RuntimeException se la persistenza fallisce (propagata dopo rollback).
     */
    public void registraUtente(Utente u, LocalDate oggi) {
        requireHackathon();
        if (!registrazioniAperteDaOrganizzatore) {
            throw new IllegalStateException("Registrazioni non aperte dall'organizzatore");
        }
        hackathon.registraUtente(u, oggi);

        try {
            utenteDAO.registraUtente(u);
        } catch (RuntimeException ex) {
            hackathon.rimuoviRegistrazione(u);
            throw ex;
        }
    }

    /**
     Elimina un utente identificato dal suo id, aggiornando database e stato del modello.

     Operazioni eseguite:
     - eliminazione utente su DB,
     - rimozione dell’utente dalla registrazione dell’hackathon,
     - rimozione dai team in cui è presente,
     - eliminazione dei team rimasti senza membri.

     @param id identificativo dell’utente.
     @throws IllegalArgumentException se id <= 0.
     @throws IllegalStateException se l’utente non è presente tra i registrati.
     */
    public void eliminaUtentePerId(int id) {
        requireHackathon();
        if (id <= 0) {
            throw new IllegalArgumentException("Id utente non valido");
        }

        Utente target = null;
        for (Registrazione r : hackathon.getRegistrazioni()) {
            if (r.getUtente().getId() == id) {
                target = r.getUtente();
                break;
            }
        }
        if (target == null) {
            throw new IllegalStateException("Utente non trovato (id=" + id + ")");
        }

        utenteDAO.eliminaUtentePerId(id);

        List<Team> snapshotTeams = new ArrayList<>(hackathon.getTeam());
        for (Team t : snapshotTeams) {
            if (t.getMembri().contains(target)) {
                t.rimuoviMembro(target);
                if (t.getMembri().isEmpty()) {
                    hackathon.rimuoviTeam(t);
                }
            }
        }

        hackathon.rimuoviRegistrazione(target);
    }

    /**
     Aggiunge un team all’hackathon garantendo un id non in collisione con DB e memoria.

     Vincoli applicati:
     - le registrazioni devono essere aperte dall’organizzatore.

     Strategia per l’id:
     - calcola un id sicuro come max(maxIdInMemoria, maxIdNelDB) + 1.

     Strategia di consistenza:
     - costruisce un nuovo oggetto Team con id sicuro e stessi dati,
     - aggiunge il team al modello,
     - persiste su DB,
     - in caso di errore su DB, rimuove il team dal modello (rollback).

     @param t team da aggiungere (usato come “template” per nome e membri).
     @param oggi data corrente.
     @throws IllegalStateException se le registrazioni non sono aperte.
     @throws IllegalArgumentException se t è null.
     */
    public void aggiungiTeam(Team t, LocalDate oggi) {
        requireHackathon();
        if (!registrazioniAperteDaOrganizzatore) {
            throw new IllegalStateException("Registrazioni non aperte dall'organizzatore");
        }
        if (t == null) throw new IllegalArgumentException("Team nullo");

        int idSicuro = nextTeamIdSafe();
        Team teamDbSafe = new Team(idSicuro, t.getNome());
        for (Utente u : t.getMembri()) {
            teamDbSafe.aggiungiMembro(u);
        }

        hackathon.aggiungiTeam(teamDbSafe, oggi);

        try {
            teamDAO.creaTeam(teamDbSafe);
        } catch (RuntimeException ex) {
            hackathon.rimuoviTeam(teamDbSafe);
            throw ex;
        }
    }

    /**
     Calcola un id di team non in collisione con gli id presenti in memoria e, se possibile, con quelli presenti su DB.

     @return nuovo id sicuro.
     */
    private int nextTeamIdSafe() {
        int maxMem = 0;
        for (Team t : hackathon.getTeam()) {
            if (t.getId() > maxMem) maxMem = t.getId();
        }

        int maxDb = 0;
        try {
            maxDb = readMaxTeamIdFromDb();
        } catch (RuntimeException ignored) {
            // Best-effort: se non si riesce a leggere dal DB, si usa almeno il massimo in memoria.
        }

        return Math.max(maxMem, maxDb) + 1;
    }

    /**
     Legge dal database il valore massimo dell’id dei team.

     @return massimo id presente, oppure 0 se la tabella è vuota.
     @throws ControllerException se si verifica un errore JDBC durante la lettura.
     */
    private int readMaxTeamIdFromDb() {
        try (Connection c = ConnessioneDatabase.getInstance().getConnection()) {
            return readMaxTeamIdAuto(c);
        } catch (SQLException e) {
            throw new ControllerException("Errore connessione DB durante lettura MAX(Id) team", e);
        }
    }

    /**
     Esegue la lettura del massimo id tentando prima schema quotato e poi schema lowercase.

     @param c connessione JDBC già aperta.
     @return massimo id presente.
     @throws ControllerException se entrambe le strategie falliscono.
     */
    private int readMaxTeamIdAuto(Connection c) {
        try {
            return readMaxTeamIdQuoted(c);
        } catch (SQLException first) {
            try {
                return readMaxTeamIdLower(c);
            } catch (SQLException second) {
                first.addSuppressed(second);
                throw new ControllerException("Impossibile leggere MAX(Id) dei team dal DB", first);
            }
        }
    }

    /**
     Lettura del massimo id sullo schema quotato.

     @param c connessione JDBC già aperta.
     @return massimo id presente.
     @throws SQLException se l’operazione JDBC fallisce.
     */
    private int readMaxTeamIdQuoted(Connection c) throws SQLException {
        String sql = "SELECT COALESCE(MAX(\"Id\"), 0) FROM \"Team\"";
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    /**
     Lettura del massimo id sullo schema lowercase.

     @param c connessione JDBC già aperta.
     @return massimo id presente.
     @throws SQLException se l’operazione JDBC fallisce.
     */
    private int readMaxTeamIdLower(Connection c) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM team";
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }



    /**
     Carica un documento e simula la valutazione dei giudici, aggiornando anche la struttura dei progressi.

     Flusso logico:
     1) verifica che l’invio documenti sia abilitato e che i requisiti minimi (giudici e team) siano soddisfatti;
     2) carica il documento nel modello e lo salva nel DB;
     3) genera un voto casuale 1..10 per ogni giudice;
     4) calcola la media dei voti e la converte in punti documento (arrotondamento);
     5) registra i punti nella mappa {@link #punteggiDocumenti}.

     @param team team destinatario del documento.
     @param d documento da caricare.
     @param oggi data corrente.
     @return stringa di riepilogo formattata (voti, media, punti e conteggio documenti).
     */
    public String caricaDocumentoEValuta(Team team, Documento d, LocalDate oggi) {
        requireHackathon();
        if (team == null) throw new IllegalArgumentException("Team nullo");
        if (d == null) throw new IllegalArgumentException("Documento nullo");

        syncInvioDocumentiConData(oggi);

        if (!invioDocumentiAbilitatoDaOrganizzatore) {
            throw new IllegalStateException("Invio documenti non abilitato dall'organizzatore");
        }

        requireMinGiudici();
        requireMinTeam();

        hackathon.caricaDocumento(team, d, oggi);

        try {
            documentoDAO.salvaDocumento(team.getId(), d);
        } catch (RuntimeException ex) {
            team.rimuoviUltimoDocumento();
            throw ex;
        }

        int somma = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("Voti giudici (1..10):\n");

        for (Giudice g : hackathon.getGiudici()) {
            int voto = random.nextInt(10) + 1;
            somma += voto;
            sb.append("- ").append(g.getNomeCompleto()).append(": ").append(voto).append("\n");
        }

        double media = somma / (double) hackathon.getGiudici().size();
        int puntiAggiunti = (int) Math.round(media);

        punteggiDocumenti.computeIfAbsent(team, k -> new ArrayList<>()).add(puntiAggiunti);

        sb.append("\nMedia: ").append(String.format(Locale.ITALY, "%.2f", media))
                .append("  -> Punti documento: ").append(puntiAggiunti)
                .append("\nDocumenti consegnati finora: ").append(getNumeroDocumentiConsegnati(team))
                .append("/").append(getSlotTotaliDocumenti());

        return sb.toString();
    }

    /**
     Pubblica la descrizione del problema dell’hackathon delegando al modello la memorizzazione e i controlli temporali.

     Prima di pubblicare impone vincoli applicativi minimi:
     - presenza di {@link #MAX_GIUDICI} giudici,
     - presenza di almeno {@link #MIN_TEAM_PER_INIZIARE_GARA} team.

     @param descrizione descrizione del problema.
     @param oggi data corrente.
     */
    public void pubblicaProblema(String descrizione, LocalDate oggi) {
        requireHackathon();
        requireMinGiudici();
        requireMinTeam();
        hackathon.pubblicaProblema(descrizione, oggi);
    }

    /**
     Registra un voto finale per una coppia giudice-team.

     Vincoli applicati:
     - il giudice deve essere tra quelli dell’hackathon;
     - il team deve appartenere all’hackathon;
     - la votazione deve essere consentita (hackathon terminato secondo modello);
     - non sono ammessi duplicati per la stessa coppia giudice-team.

     Strategia:
     - aggiunge il voto alla lista {@link #votiFinali},
     - salva il voto su DB tramite DAO,
     - in caso di errore su DB, rimuove il voto dalla lista (rollback).

     @param v voto finale da registrare.
     @param oggi data corrente.
     @throws IllegalArgumentException se v è null o se giudice/team non appartengono all’hackathon.
     @throws IllegalStateException se la votazione non è consentita o se esiste già un voto per la stessa coppia.
     */
    public void assegnaVotoFinale(Voto v, LocalDate oggi) {
        requireHackathon();
        if (v == null) throw new IllegalArgumentException("Voto nullo");

        if (!hackathon.getGiudici().contains(v.getGiudice())) {
            throw new IllegalArgumentException("Giudice non associato all'hackathon");
        }
        if (!hackathon.getTeam().contains(v.getTeam())) {
            throw new IllegalArgumentException("Team non registrato all'hackathon");
        }
        if (!hackathon.isVotazioneConsentita(oggi)) {
            throw new IllegalStateException("Non e ancora possibile votare (hackathon non terminato)");
        }

        boolean duplicato = votiFinali.stream().anyMatch(existing ->
                existing.getGiudice().equals(v.getGiudice()) && existing.getTeam().equals(v.getTeam()));
        if (duplicato) {
            throw new IllegalStateException("Voto gia presente per questo giudice e questo team");
        }

        votiFinali.add(v);

        try {
            votoDAO.salvaVoto(v);
        } catch (RuntimeException ex) {
            votiFinali.remove(v);
            throw ex;
        }
    }


    /**
     Calcola il numero totale di slot documenti disponibili per un team:
     durataHackathonInGiorni * maxDocumentiAlGiornoPerTeam.

     @return numero totale di slot documento.
     */
    public int getSlotTotaliDocumenti() {
        requireHackathon();
        return hackathon.getDurataHackathonGiorni() * hackathon.getMaxDocumentiAlGiornoPerTeam();
    }

    /**
     Restituisce il numero di documenti consegnati da un team, basandosi sui punteggi registrati.

     @param team team di interesse.
     @return numero di documenti consegnati, 0 se team è null o non presente nella mappa.
     */
    public int getNumeroDocumentiConsegnati(Team team) {
        if (team == null) return 0;
        return punteggiDocumenti.getOrDefault(team, Collections.emptyList()).size();
    }

    /**
     Calcola la somma dei punti ottenuti dai documenti caricati da un team.

     @param team team di interesse.
     @return somma punti, 0 se team è null o non presente nella mappa.
     */
    public int getSommaPuntiDocumenti(Team team) {
        if (team == null) return 0;
        int sum = 0;
        for (int p : punteggiDocumenti.getOrDefault(team, Collections.emptyList())) sum += p;
        return sum;
    }

    /**
     Calcola quanti documenti mancano al completamento di tutti gli slot disponibili.

     @param team team di interesse.
     @return numero di documenti mancanti (mai negativo).
     */
    public int getDocumentiMancanti(Team team) {
        int slots = getSlotTotaliDocumenti();
        int consegnati = getNumeroDocumentiConsegnati(team);
        return Math.max(0, slots - consegnati);
    }

    /**
     Calcola la media dei progressi del team su scala 0..10 considerando anche i documenti mancanti come 0.

     Formalmente:
     media = (somma punti documenti consegnati) / (slot totali)

     @param team team di interesse.
     @return media progressi in [0,10] (dipende da come vengono generati i punteggi), oppure 0 se slot <= 0.
     */
    public double calcolaMediaProgressiConZeri(Team team) {
        int slots = getSlotTotaliDocumenti();
        if (slots <= 0) return 0.0;
        return getSommaPuntiDocumenti(team) / (double) slots;
    }

    /**
     Simula eventuali voti finali mancanti e produce un report testuale dei risultati.

     Vincoli:
     - richiede presenza minima di giudici e team,
     - richiede che la votazione sia consentita (hackathon terminato secondo il modello).

     Effetti collaterali:
     - può aggiungere voti finali mancanti nella lista {@link #votiFinali} e persistenti su DB
     tramite chiamate interne a {@link #assegnaVotoFinale(Voto, LocalDate)}.

     @param oggi data corrente.
     @return report testuale con voti finali e score dei team.
     */
    public String simulaVotiFinaliEReport(LocalDate oggi) {
        requireHackathon();
        if (oggi == null) throw new IllegalArgumentException("Data nulla");

        requireMinGiudici();
        requireMinTeam();

        if (!hackathon.isVotazioneConsentita(oggi)) {
            throw new IllegalStateException("Non e ancora possibile votare (hackathon non terminato)");
        }

        simulaVotiMancanti(oggi);
        return creaReportVotiFinali(oggi);
    }

    /**
     Per ogni coppia giudice-team, se manca il voto finale, lo genera casualmente in range 0..10 e lo registra.

     @param oggi data corrente.
     */
    private void simulaVotiMancanti(LocalDate oggi) {
        for (Giudice g : hackathon.getGiudici()) {
            for (Team t : hackathon.getTeam()) {
                if (esisteVotoPer(g, t)) continue;
                int voto = random.nextInt(11); // 0..10
                assegnaVotoFinale(new Voto(voto, g, t), oggi);
            }
        }
    }

    /**
     Crea un report testuale contenente:
     - voti finali aggregati per team,
     - media voti finali con penalizzazione implicita a zero,
     - media progressi con penalizzazione a zero,
     - score totale calcolato come combinazione pesata.

     @param oggi data corrente usata solo a fini di intestazione del report.
     @return report testuale.
     */
    private String creaReportVotiFinali(LocalDate oggi) {
        StringBuilder sb = new StringBuilder();
        sb.append("VOTI FINALI\n");
        sb.append("Data: ").append(oggi).append("\n\n");

        for (Team t : hackathon.getTeam()) {
            sb.append("TEAM: ").append(t.getNome()).append("\n");

            List<Integer> votiTeam = raccogliVotiTeam(t);

            sb.append("- Voti: ").append(votiTeam).append("\n");
            sb.append("- Media voti finali (con 0 se manca qualcuno): ")
                    .append(String.format(Locale.ITALY, "%.2f", mediaVotiTeamConZeri(t))).append("\n");
            sb.append("- Media progressi (con 0): ")
                    .append(String.format(Locale.ITALY, "%.2f", calcolaMediaProgressiConZeri(t))).append("\n");
            sb.append("- SCORE (0..10) = 70% voti + 30% progressi: ")
                    .append(String.format(Locale.ITALY, "%.2f", scoreTotaleTeam(t))).append("\n\n");
        }

        return sb.toString();
    }

    /**
     Estrae tutti i punteggi dei voti finali associati a un team.

     @param team team di interesse.
     @return lista dei punteggi dei voti finali del team.
     */
    private List<Integer> raccogliVotiTeam(Team team) {
        List<Integer> voti = new ArrayList<>();
        for (Voto v : votiFinali) {
            if (v.getTeam().equals(team)) voti.add(v.getPunteggio());
        }
        return voti;
    }

    /**
     Verifica se esiste già un voto finale registrato per una specifica coppia giudice-team.

     @param giudice giudice di interesse.
     @param team team di interesse.
     @return true se esiste già un voto, false altrimenti.
     */
    private boolean esisteVotoPer(Giudice giudice, Team team) {
        return votiFinali.stream()
                .anyMatch(v -> v.getGiudice().equals(giudice) && v.getTeam().equals(team));
    }

    /**
     Genera la schermata testuale della classifica finale completa ordinando i team per score decrescente.

     Vincoli:
     - la classifica è disponibile solo se la votazione è consentita (hackathon terminato secondo modello),
     - richiede presenza minima di giudici e team.

     @param oggi data corrente.
     @return schermata testuale con classifica e dettagli.
     */
    public String generaSchermataClassificaCompleta(LocalDate oggi) {
        requireHackathon();
        if (oggi == null) throw new IllegalArgumentException("Data nulla");

        if (!hackathon.isVotazioneConsentita(oggi)) {
            throw new IllegalStateException("Classifica non disponibile: hackathon non terminato");
        }

        requireMinGiudici();
        requireMinTeam();

        List<Team> teams = new ArrayList<>(hackathon.getTeam());
        teams.sort((a, b) -> Double.compare(scoreTotaleTeam(b), scoreTotaleTeam(a)));

        StringBuilder sb = new StringBuilder();
        sb.append("CLASSIFICA FINALE\n");
        sb.append("Hackathon: ").append(hackathon.getTitolo())
                .append(" (").append(hackathon.getSedeAsString()).append(")\n");
        sb.append("Periodo: ").append(hackathon.getDataInizio()).append(" -> ").append(hackathon.getDataFine()).append("\n");
        sb.append("Min team per iniziare: ").append(MIN_TEAM_PER_INIZIARE_GARA)
                .append(" | Team attuali: ").append(hackathon.getTeam().size()).append("\n\n");

        sb.append("Legenda:\n");
        sb.append("- Media voti finali: media dei voti 0..10 (se un giudice non vota, vale 0)\n");
        sb.append("- Media progressi: media dei punteggi-documento 0..10 (documenti mancanti valgono 0)\n");
        sb.append("- SCORE totale (0..10) = 70% Media voti + 30% Media progressi\n\n");

        int pos = 1;
        for (Team t : teams) {
            double mediaVoti = mediaVotiTeamConZeri(t);
            double mediaProg = calcolaMediaProgressiConZeri(t);
            int consegnati = getNumeroDocumentiConsegnati(t);
            int slots = getSlotTotaliDocumenti();
            int mancanti = getDocumentiMancanti(t);
            double score = scoreTotaleTeam(t);

            sb.append(pos).append(") ").append(t.getNome()).append("\n");
            sb.append("   - SCORE totale: ").append(String.format(Locale.ITALY, "%.2f", score)).append("\n");
            sb.append("   - Media voti finali: ").append(String.format(Locale.ITALY, "%.2f", mediaVoti)).append("\n");
            sb.append("   - Media progressi (con 0): ").append(String.format(Locale.ITALY, "%.2f", mediaProg)).append("\n");
            sb.append("   - Documenti: ").append(consegnati).append("/").append(slots)
                    .append(" (mancanti: ").append(mancanti).append(")\n\n");

            pos++;
        }

        return sb.toString();
    }

    /**
     Calcola la media dei voti finali di un team dividendo la somma dei voti per il numero totale di giudici.

     Scelta progettuale:
     - la divisione avviene sul numero totale di giudici, non sul numero di voti presenti;
     in tal modo la mancanza di un voto equivale implicitamente a un contributo pari a 0.

     @param team team di interesse.
     @return media dei voti finali, oppure 0 se non ci sono giudici.
     */
    private double mediaVotiTeamConZeri(Team team) {
        int sum = 0;

        for (Voto v : votiFinali) {
            if (team.equals(v.getTeam())) {
                sum += v.getPunteggio();
            }
        }

        if (hackathon.getGiudici().isEmpty()) return 0.0;
        int giudici = hackathon.getGiudici().size();
        return sum / (double) giudici;
    }

    /**
     Calcola lo score complessivo del team come combinazione pesata:
     0.70 * media voti finali + 0.30 * media progressi.

     @param team team di interesse.
     @return score totale del team.
     */
    private double scoreTotaleTeam(Team team) {
        double voti = mediaVotiTeamConZeri(team);
        double prog = calcolaMediaProgressiConZeri(team);
        return (0.70 * voti) + (0.30 * prog);
    }



    /**
     Controllo interno: verifica che il vincolo sul numero minimo di giudici sia rispettato.

     @throws IllegalStateException se i giudici registrati sono meno di {@link #MAX_GIUDICI}.
     */
    private void requireMinGiudici() {
        int n = hackathon.getGiudici().size();
        if (n < MAX_GIUDICI) {
            throw new IllegalStateException(
                    "Servono almeno " + MAX_GIUDICI + " giudici registrati per iniziare la gara (attuali: " + n + ")");
        }
    }

    /**
     Controllo interno: verifica che il vincolo sul numero minimo di team sia rispettato.

     @throws IllegalStateException se i team registrati sono meno di {@link #MIN_TEAM_PER_INIZIARE_GARA}.
     */
    private void requireMinTeam() {
        int n = hackathon.getTeam().size();
        if (n < MIN_TEAM_PER_INIZIARE_GARA) {
            throw new IllegalStateException(
                    "Servono almeno " + MIN_TEAM_PER_INIZIARE_GARA + " team per iniziare la gara (attuali: " + n + ")");
        }
    }

    /**
     Controllo interno: verifica che l’hackathon sia stato inizializzato.

     @throws IllegalStateException se l’hackathon è null.
     */
    private void requireHackathon() {
        if (hackathon == null) throw new IllegalStateException("Hackathon non inizializzato");
    }
}
