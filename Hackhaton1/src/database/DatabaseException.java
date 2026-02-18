package database;

/**
 * Eccezione specifica per errori di connessione/persistenza DB.
 * Serve a evitare eccezioni generiche (Sonar S112).
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
