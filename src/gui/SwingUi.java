package gui;

import javax.swing.*;
import java.awt.*;

public class SwingUi extends JFrame {

    public SwingUi() {
        setTitle("Hackathon Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


        setLayout(new BorderLayout());

        JLabel titolo = new JLabel("Benvenuto nell'Hackathon Manager!", SwingConstants.CENTER);
        titolo.setFont(new Font("Arial", Font.BOLD, 20));
        add(titolo, BorderLayout.NORTH);

        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setText("Qui appariranno le info sugli hackathon.");
        add(new JScrollPane(infoArea), BorderLayout.CENTER);
    }
}
