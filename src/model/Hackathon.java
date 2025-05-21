package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Hackathon {
    private String titolo;
    private String sede;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private int maxPartecipanti;
    private int maxTeamSize;
    private Organizzatore organizzatore;
    private List<Giudice> giudici = new ArrayList<>();
    private List<Registrazione> registrazioni = new ArrayList<>();
    private List<Team> team = new ArrayList<>();

    public Hackathon(String titolo, String sede, LocalDate dataInizio, LocalDate dataFine,
                     int maxPartecipanti, int maxTeamSize, Organizzatore organizzatore) {
        this.titolo = titolo;
        this.sede = sede;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.maxPartecipanti = maxPartecipanti;
        this.maxTeamSize = maxTeamSize;
        this.organizzatore = organizzatore;
    }

    public void aggiungiGiudice(Giudice g) {
        giudici.add(g);
    }

    public void registraUtente(Utente u) {
        registrazioni.add(new Registrazione(u, this));
    }

    public void aggiungiTeam(Team t) {
        team.add(t);
    }

    // Getters (se servono)
}