package model;

import controller.Controller;
import model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import gui.SwingUi;


public class Main {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            gui.SwingUi ui = new gui.SwingUi();
            ui.setVisible(true);
        });

        Controller controller = new Controller();


        Organizzatore organizzatore = new Organizzatore(1, "Alice", "alice@email.com");
        controller.creaHackathon("Hack4Future", "Milano",
                LocalDate.of(2025, 5, 15),
                LocalDate.of(2025, 5, 17),
                10, 3, organizzatore);


        Giudice g1 = new Giudice(2, "Luca", "luca@giudici.com");
        Giudice g2 = new Giudice(3, "Marta", "marta@giudici.com");
        controller.aggiungiGiudice(g1);
        controller.aggiungiGiudice(g2);


        Utente u1 = new Utente(4, "Giulia", "giulia@user.com");
        Utente u2 = new Utente(5, "Marco", "marco@user.com");
        Utente u3 = new Utente(6, "Elisa", "elisa@user.com");
        controller.registraUtente(u1);
        controller.registraUtente(u2);
        controller.registraUtente(u3);


        Team team1 = new Team(1, "TeamRocket");
        team1.aggiungiMembro(u1);
        team1.aggiungiMembro(u2);
        team1.aggiungiMembro(u3);
        controller.aggiungiTeam(team1);


        controller.caricaDocumento(team1, new Documento("Prima versione progetto"));
        controller.caricaDocumento(team1, new Documento("Versione finale progetto"));


        controller.assegnaVoto(new Voto(8, g1, team1));
        controller.assegnaVoto(new Voto(9, g2, team1));


        System.out.println("📊 Classifica finale:");
        Map<Team, Integer> classifica = controller.calcolaClassifica();
        classifica.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(entry -> {
                    System.out.println(entry.getKey().getNome() + " → " + entry.getValue() + " punti");
                });
    }
}
