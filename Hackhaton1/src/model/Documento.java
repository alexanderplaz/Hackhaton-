package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Documento di avanzamento ("progress update") caricato da un team.

 * Nota importante per questo progetto:
 * la GUI usa una "data simulata" (LocalDate). Per evitare incoerenze
 * tra data simulata e data reale del PC, il timestamp del documento
 * viene costruito a partire dalla data simulata passata al costruttore.
 */
public class Documento {

    private final String contenuto;
    private final LocalDateTime timestamp;

    /**
     * Crea un documento usando la data simulata.
     *
     * @param contenuto     testo del documento (non nullo e non vuoto)
     * @param dataSimulata  data (simulata) in cui il documento viene caricato
     */
    public Documento(String contenuto, LocalDate dataSimulata) {
        if (contenuto == null || contenuto.isBlank()) {
            throw new IllegalArgumentException("Contenuto documento non valido");
        }
        if (dataSimulata == null) {
            throw new IllegalArgumentException("Data simulata nulla");
        }
        this.contenuto = contenuto;

        // Manteniamo anche un orario "realistico" (ora del PC) ma fissiamo la data a quella simulata.
        this.timestamp = LocalDateTime.of(dataSimulata, LocalTime.now());
    }

    /**
     * Costruttore di comodo (usa la data reale del sistema).

     * Consigliato solo per test locali; nella GUI usiamo sempre la data simulata.
     */
    public Documento(String contenuto) {
        this(contenuto, LocalDate.now());
    }

    public String getContenuto() {
        return contenuto;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return timestamp + " - " + contenuto;
    }
}
