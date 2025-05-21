package controller;

import model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Controller {


    private Hackathon hackathon;
    private List<Voto> voti = new ArrayList<>();

    public void creaHackathon(String titolo, String sede, LocalDate inizio, LocalDate fine,
                              int maxPartecipanti, int maxTeamSize, Organizzatore organizzatore) {
        this.hackathon = new Hackathon(titolo, sede, inizio, fine, maxPartecipanti, maxTeamSize, organizzatore);
    }

    public void aggiungiGiudice(Giudice g) {
        hackathon.aggiungiGiudice(g);
    }

    public void registraUtente(Utente u) {
        hackathon.registraUtente(u);
    }

    public void aggiungiTeam(Team t) {
        hackathon.aggiungiTeam(t);
    }

    public void caricaDocumento(Team team, Documento d) {
        team.caricaDocumento(d);
    }

    public void assegnaVoto(Voto v) {
        voti.add(v);
    }

    public Map<Team, Integer> calcolaClassifica() {
        Map<Team, Integer> classifica = new HashMap<>();
        for (Voto v : voti) {
            Team t = v.getTeam();
            classifica.put(t, classifica.getOrDefault(t, 0) + v.getPunteggio());
        }
        return classifica;
    }

    public Hackathon getHackathon() {
        return hackathon;
    }

    public List<Voto> getVoti() {
        return voti;
    }
}

