package model;

import java.time.LocalDateTime;

public class Documento {
    private String contenuto;
    private LocalDateTime timestamp;

    public Documento(String contenuto) {
        this.contenuto = contenuto;
        this.timestamp = LocalDateTime.now();
    }

    public String getContenuto() { return contenuto; }
    public LocalDateTime getTimestamp() { return timestamp; }
}