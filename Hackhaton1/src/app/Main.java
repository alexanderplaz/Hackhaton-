package app;

import controller.Controller;
import gui.SwingUi;


import javax.swing.SwingUtilities;



public class Main {

    public static void main(String[] args) {

        // Il controller viene creato vuoto: l'hackathon e l'organizzatore
        // verranno inizializzati dalla GUI nella schermata iniziale.
        Controller controller = new Controller();

        SwingUtilities.invokeLater(() -> {
            SwingUi ui = new SwingUi(controller);
            ui.setVisible(true);
        });
    }
}
