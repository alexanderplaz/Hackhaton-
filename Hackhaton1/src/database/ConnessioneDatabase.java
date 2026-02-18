package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 Classe responsabile della gestione delle connessioni al database PostgreSQL.

 La classe implementa un pattern Singleton con responsabilità di factory:
 - Singleton: garantisce un’unica istanza configurata dell’oggetto ConnessioneDatabase.
 - Factory: fornisce nuove istanze di {@link java.sql.Connection} ad ogni chiamata del metodo {@link #getConnection()}.

 Principi progettuali adottati:
 - Fail-fast: al momento della creazione dell’istanza viene effettuato un tentativo di connessione per validare
 immediatamente la configurazione (URL, utente, password). In caso di errore viene sollevata una
 {@link database.DatabaseException}.
 - Nessun riuso di Connection: ogni invocazione di getConnection() restituisce una nuova connessione.
 La responsabilità di chiusura è demandata ai DAO tramite try-with-resources.

 Configurazione:
 - Le credenziali vengono lette prioritariamente da variabili d’ambiente (DB_URL, DB_USER, DB_PASSWORD).
 - In assenza di variabili d’ambiente, vengono utilizzati valori di default coerenti con una configurazione locale.

 Nota di sicurezza: l’uso di password in chiaro nei default è accettabile solo in contesti di sviluppo.
 In ambienti di produzione è obbligatorio l’uso di variabili d’ambiente o sistemi di secret management.
 */
public final class ConnessioneDatabase {

    /**
     Nome della variabile d’ambiente contenente l’URL del database.
     */
    private static final String ENV_DB_URL = "DB_URL";

    /**
     Nome della variabile d’ambiente contenente lo username del database.
     */
    private static final String ENV_DB_USER = "DB_USER";

    /**
     Nome della variabile d’ambiente contenente la password del database.
     */
    private static final String ENV_DB_PASSWORD = "DB_PASSWORD";

    /**
     URL di default per la connessione PostgreSQL in ambiente locale.
     */
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/Hackathon";

    /**
     Username di default per la connessione PostgreSQL.
     */
    private static final String DEFAULT_USER = "postgres";

    /**
     Password di default per la connessione PostgreSQL.
     Nota: sostituire con la propria password reale in ambiente di sviluppo.
     */
    private static final String DEFAULT_PASSWORD = "1234"; // <-- metti qui la tua password reale

    /**
     Istanza Singleton della classe.
     */
    private static ConnessioneDatabase instance;

    /**
     URL effettivo utilizzato per la connessione, determinato da variabile d’ambiente o default.
     */
    private final String url;

    /**
     Username effettivo utilizzato per la connessione.
     */
    private final String user;

    /**
     Password effettiva utilizzata per la connessione.
     */
    private final String password;

    /**
     Costruttore privato per implementare il pattern Singleton.

     Durante la costruzione:
     - vengono lette le configurazioni da variabili d’ambiente o default,
     - viene effettuato un test immediato di connessione (fail-fast),
     così da intercettare errori di configurazione in fase di avvio dell’applicazione.

     @throws DatabaseException se la connessione iniziale di test fallisce.
     */
    private ConnessioneDatabase() {
        this.url = readEnvOrDefault(ENV_DB_URL, DEFAULT_URL);
        this.user = readEnvOrDefault(ENV_DB_USER, DEFAULT_USER);
        this.password = readEnvOrDefault(ENV_DB_PASSWORD, DEFAULT_PASSWORD);

        // Test fail-fast: se la config è sbagliata, esplode qui con messaggio chiaro.
        try (Connection ignored = DriverManager.getConnection(url, user, password)) {
            // connessione valida
        } catch (SQLException e) {
            throw new DatabaseException("Connessione DB non riuscita. URL=" + url + " USER=" + user, e);
        }
    }

    /**
     Legge una variabile d’ambiente e, se assente o vuota, restituisce un valore di default.

     @param key nome della variabile d’ambiente.
     @param def valore di default da usare se la variabile non è definita o è blank.
     @return valore della variabile d’ambiente (trimmed) oppure il default.
     */
    private static String readEnvOrDefault(String key, String def) {
        String val = System.getenv(key);
        return (val == null || val.isBlank()) ? def : val.trim();
    }

    /**
     Restituisce l’istanza Singleton di ConnessioneDatabase.

     Se l’istanza non è ancora stata creata, viene inizializzata al primo accesso
     (lazy initialization).

     @return istanza unica di ConnessioneDatabase.
     */
    public static ConnessioneDatabase getInstance() {
        if (instance == null) {
            instance = new ConnessioneDatabase();
        }
        return instance;
    }

    /**
     Restituisce una nuova connessione al database PostgreSQL.

     Regola fondamentale:
     - Ogni chiamata produce una nuova istanza di {@link java.sql.Connection}.
     - La chiusura della connessione è responsabilità del chiamante (tipicamente i DAO),
     che devono utilizzare il costrutto try-with-resources.

     @return nuova connessione JDBC verso il database configurato.
     @throws DatabaseException se l’apertura della connessione fallisce.
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException("Errore apertura connessione DB. URL=" + url + " USER=" + user, e);
        }
    }
}
