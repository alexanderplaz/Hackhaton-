package implementazionipostgresdao;

/**
 * Eccezione custom per errori DAO (persistenza).
 * Serve per evitare RuntimeException generiche e dare messaggi chiari.
 */
public class DaoException extends RuntimeException {
    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }

    public DaoException(String message) {
        super(message);
    }
}