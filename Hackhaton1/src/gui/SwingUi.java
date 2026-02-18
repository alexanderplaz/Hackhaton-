package gui;

import controller.Controller;
import model.*;
import java.io.Serial;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/**
 GUI Swing principale dell'applicazione "Hackathon Manager", realizzata con Swing e organizzata secondo il pattern BCE
 (Boundary-Control-Entity).

 La classe rappresenta il Boundary dell'applicazione: raccoglie l'input dell'utente tramite componenti grafici, invoca
 le operazioni del {@link controller.Controller} (Control) e visualizza le informazioni delle entità di dominio
 ({@link model.Hackathon}, {@link model.Team}, {@link model.Utente}, {@link model.Giudice}, ecc.).

 Flusso generale:
 - All'avvio mostra un pannello di configurazione iniziale (setup) in cui vengono inseriti i dati dell'organizzatore e
 la sede dell'evento.
 - Dopo la conferma, inizializza il dominio creando un hackathon tramite il controller e passa alla schermata principale.
 - La schermata principale supporta due modalità operative:
 - Modalità UTENTE: operazioni disponibili ai partecipanti (registrazione, creazione team, invio documenti).
 - Modalità ORGANIZZATORE: operazioni di gestione (data corrente, registrazioni, invio documenti, giudici, fine gara).
 - L'accesso alla modalità organizzatore è protetto da password.

 Vincoli gestiti a livello di interfaccia:
 - In modalità utente l'invio di un documento richiede che sia selezionato un team.
 - In modalità organizzatore alcune operazioni sono abilitate solo se rispettano vincoli temporali e di consistenza
 (es. minimo numero di giudici, finestra di registrazione, periodo hackathon, data non bloccata).

 Nota: questa classe NON implementa le regole di business in modo definitivo. I vincoli reali vengono verificati dal
 Controller e dalle entità di dominio; la GUI applica soltanto abilitazioni/disabilitazioni e validazioni minime.
 */
public class SwingUi extends JFrame {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     Identificatore della card di setup nel {@link CardLayout} principale.
     */
    private static final String CARD_SETUP = "SETUP";
    /**
     Identificatore della card applicativa nel {@link CardLayout} principale.
     */
    private static final String CARD_APP = "APP";

    /**
     Identificatore del footer in modalità utente.
     */
    private static final String FOOTER_USER = "USER";
    /**
     Identificatore del footer in modalità organizzatore.
     */
    private static final String FOOTER_ORG = "ORG";

    /**
     Titolo tab della lista utenti.
     */
    private static final String TAB_USERS = "Utenti";
    /**
     Titolo tab della lista team.
     */
    private static final String TAB_TEAMS = "Team";
    /**
     Titolo tab della gestione giudici (visibile solo in modalità organizzatore).
     */
    private static final String TAB_JUDGES = "Giudici";
    /**
     Titolo tab classifica (visibile solo dopo fine hackathon o a gara conclusa).
     */
    private static final String TAB_RANKING = "Classifica";

    /**
     Titolo di default per i dialoghi di errore.
     */
    private static final String TITLE_ERROR = "Errore";
    /**
     Titolo di default per i dialoghi informativi.
     */
    private static final String TITLE_INFO = "Info";
    /**
     Titolo di default per i dialoghi di conferma.
     */
    private static final String TITLE_OK = "OK";

    /**
     Nome del font base usato nella UI.
     */
    private static final String FONT_ARIAL = "Arial";
    /**
     Font per il titolo principale.
     */
    private static final Font FONT_TITLE = new Font(FONT_ARIAL, Font.BOLD, 20);
    /**
     Font per sezioni e intestazioni.
     */
    private static final Font FONT_SECTION = new Font(FONT_ARIAL, Font.BOLD, 14);
    /**
     Font per testo normale.
     */
    private static final Font FONT_TEXT = new Font(FONT_ARIAL, Font.PLAIN, 12);
    /**
     Font monospaziato per aree di dettaglio/report.
     */
    private static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 12);

    /**
     Controller applicativo (strato Control del BCE) a cui vengono delegate le operazioni di dominio.
     Marcato transient perché la UI non gestisce persistenza dell'istanza.
     */
    private final transient Controller controller;

    /**
     CardLayout globale della finestra: permette il passaggio tra setup iniziale e schermata applicativa.
     */
    private final CardLayout rootCards = new CardLayout();
    /**
     Pannello radice controllato da {@link #rootCards}.
     */
    private final JPanel root = new JPanel(rootCards);

    /**
     CardLayout del footer: commuta tra azioni utente e azioni organizzatore.
     */
    private final CardLayout footerCards = new CardLayout();
    /**
     Pannello footer controllato da {@link #footerCards}.
     */
    private final JPanel footer = new JPanel(footerCards);

    /**
     Indica se la configurazione iniziale è stata completata e l'hackathon è stato creato.
     */
    private boolean initialized = false;
    /**
     Organizzatore configurato nella fase di setup. Marcato transient perché non gestisce persistenza.
     */
    private transient Organizzatore organizzatore;
    /**
     Data corrente "simulata" dall'applicazione. Può essere modificata dall'organizzatore per testare le fasi del flusso.
     */
    private LocalDate dataCorrente = LocalDate.now();
    /**
     Se true, la data corrente non è più modificabile (gara conclusa) e la UI mostra la classifica.
     */
    private boolean dataBloccata = false;
    /**
     Se true, la UI è in modalità organizzatore e abilita le operazioni amministrative.
     */
    private boolean organizerMode = false;

    /**
     Bottone mostrato in header con il nome organizzatore; consente l'ingresso/uscita dalla modalità organizzatore.
     */
    private JButton organizerButton;
    /**
     Bottone in header per commutare modalità (ridondante rispetto a {@link #organizerButton}).
     */
    private JButton switchModeButton;
    /**
     Etichetta di stato della modalità corrente.
     */
    private final JLabel modeLabel = new JLabel("Modalita': UTENTE");
    /**
     Etichetta della data corrente e del suo stato (bloccata/non bloccata).
     */
    private final JLabel dateLabel = new JLabel();
    /**
     Etichetta che riassume la fase corrente dell'hackathon.
     */
    private final JLabel statusLabel = new JLabel();
    /**
     Etichetta con informazioni sulla presenza dei giudici (minimo 3).
     */
    private final JLabel judgesInfoLabel = new JLabel();
    /**
     Etichetta con informazioni sulla finestra di registrazione e stato aperto/chiuso.
     */
    private final JLabel registrationInfoLabel = new JLabel();
    /**
     Etichetta con date, durata e sede dell'hackathon.
     */
    private final JLabel hackathonInfoLabel = new JLabel();
    /**
     Etichetta con limiti numerici (team, partecipanti, max membri/team).
     */
    private final JLabel limitsInfoLabel = new JLabel();
    /**
     Etichetta con limite e stato dell'invio documenti.
     */
    private final JLabel uploadLimitInfoLabel = new JLabel();
    /**
     Etichetta che mostra un contatore (giorno di registrazioni o giorno di hackathon) o informazioni di attesa.
     */
    private final JLabel dayCounterLabel = new JLabel();

    /**
     Modello della lista utenti.
     */
    private final DefaultListModel<Utente> userModel = new DefaultListModel<>();
    /**
     Lista utenti visualizzata nella UI.
     */
    private final JList<Utente> userList = new JList<>(userModel);

    /**
     Modello della lista team.
     */
    private final DefaultListModel<Team> teamModel = new DefaultListModel<>();
    /**
     Lista team visualizzata nella UI.
     */
    private final JList<Team> teamList = new JList<>(teamModel);

    /**
     Modello della lista giudici.
     */
    private final DefaultListModel<Giudice> judgeModel = new DefaultListModel<>();
    /**
     Lista giudici visualizzata nella UI.
     */
    private final JList<Giudice> judgeList = new JList<>(judgeModel);

    /**
     Etichetta che riporta lo stato del problema (pubblicato/non pubblicato) nella tab giudici.
     */
    private final JLabel problemCurrentLabel = new JLabel();
    /**
     Area testo con la descrizione del problema. In modalità organizzatore può essere utilizzata per inserire/pubblicare.
     */
    private final JTextArea problemArea = new JTextArea(4, 20);
    /**
     Bottone per pubblicare il problema dalla tab giudici. Può risultare null prima della costruzione della tab.
     */
    private JButton btnPublishProblemInTab;

    /**
     Area che visualizza dettagli dell'elemento selezionato o report generati dal controller.
     */
    private final JTextArea details = new JTextArea();

    /**
     Bottone organizzatore per impostare la data corrente.
     */
    private JButton btnSetDate;
    /**
     Bottone organizzatore per concludere la gara (blocco data e abilitazione classifica).
     */
    private JButton btnConcludeRace;
    /**
     Bottone organizzatore per aprire le registrazioni (solo durante finestra di registrazione).
     */
    private JButton btnOpenReg;
    /**
     Bottone organizzatore per chiudere le registrazioni.
     */
    private JButton btnCloseReg;
    /**
     Bottone organizzatore per abilitare l'invio dei documenti (solo durante hackathon e con vincoli soddisfatti).
     */
    private JButton btnEnableDocs;
    /**
     Bottone organizzatore per disabilitare l'invio dei documenti.
     */
    private JButton btnDisableDocs;
    /**
     Bottone organizzatore per registrare un giudice.
     */
    private JButton btnRegisterJudge;
    /**
     Bottone organizzatore per eliminare un giudice.
     */
    private JButton btnDeleteJudge;
    /**
     Bottone organizzatore per mostrare la classifica nei dettagli.
     */
    private JButton btnShowRanking;

    /**
     Bottone utente per eliminare un utente.
     */
    private JButton btnDeleteUser;

    /**
     Pannello a tab della sezione centrale della UI.
     */
    private JTabbedPane tabs;
    /**
     Componente associato alla tab giudici, costruito una volta e riutilizzato.
     */
    private Component judgeTabComponent;
    /**
     Componente associato alla tab classifica, costruito una volta e riutilizzato.
     */
    private Component rankingTabComponent;
    /**
     Area di testo dedicata alla visualizzazione della classifica completa.
     */
    private final JTextArea rankingArea = new JTextArea();

    /**
     Costruisce la finestra principale e inizializza l'interfaccia in modalità setup.
     Il dominio viene creato solo dopo la conferma del pannello di configurazione iniziale.

     @param controller controller applicativo utilizzato per invocare operazioni di dominio.
     @throws IllegalArgumentException se controller è null.
     */
    public SwingUi(Controller controller) {
        if (controller == null) throw new IllegalArgumentException("Controller nullo");
        this.controller = controller;

        setTitle("Hackathon Manager");
        setSize(1020, 600);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        root.add(buildSetupPanel(), CARD_SETUP);
        root.add(buildAppPanel(), CARD_APP);

        setContentPane(root);
        rootCards.show(root, CARD_SETUP);
    }

    // =====================================================================================
    // SETUP
    // =====================================================================================

    /**
     Costruisce il pannello di configurazione iniziale.

     Il pannello permette l'inserimento dei dati dell'organizzatore (nome, cognome, password) e della sede dell'evento
     tramite enumerazione {@link model.CapitaleEuropa}. Alla conferma viene invocata l'inizializzazione del dominio e,
     in caso di successo, la UI passa alla schermata applicativa.

     @return pannello Swing per la configurazione iniziale.
     */
    private JPanel buildSetupPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Configurazione iniziale", SwingConstants.CENTER);
        title.setFont(new Font(FONT_ARIAL, Font.BOLD, 22));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JTextField nomeField = new JTextField();
        JTextField cognomeField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JComboBox<CapitaleEuropa> sedeCombo = new JComboBox<>(CapitaleEuropa.values());
        sedeCombo.setSelectedItem(CapitaleEuropa.ROMA);

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Nome organizzatore:"), c);
        c.gridx = 1; c.gridy = 0; form.add(nomeField, c);

        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Cognome organizzatore:"), c);
        c.gridx = 1; c.gridy = 1; form.add(cognomeField, c);

        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Password:"), c);
        c.gridx = 1; c.gridy = 2; form.add(passwordField, c);

        c.gridx = 0; c.gridy = 3; form.add(new JLabel("Sede (capitale europea):"), c);
        c.gridx = 1; c.gridy = 3; form.add(sedeCombo, c);

        panel.add(form, BorderLayout.CENTER);

        JButton btnConfirm = new JButton("Conferma");
        btnConfirm.addActionListener(e -> {
            String nome = nomeField.getText().trim();
            String cognome = cognomeField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            CapitaleEuropa sede = (CapitaleEuropa) sedeCombo.getSelectedItem();

            try {
                initDomain(nome, cognome, password, sede);
                rootCards.show(root, CARD_APP);
            } catch (Exception ex) {
                showError(ex);
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnConfirm);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    /**
     Inizializza lo stato di dominio e la UI dopo la conferma del setup.

     Operazioni eseguite:
     - Creazione dell'organizzatore e dell'evento hackathon tramite {@link controller.Controller}.
     - Inizializzazione delle variabili di stato della UI (data corrente, modalità, flag di blocco data).
     - Impostazione di un default coerente con il flusso: registrazioni chiuse e invio documenti disabilitato.
     - Aggiornamento dei componenti principali e ricaricamento dei dati visualizzati.

     @param nome nome dell'organizzatore.
     @param cognome cognome dell'organizzatore.
     @param password password dell'organizzatore.
     @param sede sede dell'hackathon (capitale europea).
     @throws IllegalArgumentException se la sede è null.
     */
    private void initDomain(String nome, String cognome, String password, CapitaleEuropa sede) {
        if (sede == null) throw new IllegalArgumentException("Sede non valida");

        this.organizzatore = new Organizzatore(1, nome, cognome, password);

        LocalDate oggi = LocalDate.now();
        LocalDate inizio = oggi.plusDays(10);
        LocalDate fine = inizio.plusDays(4);

        controller.creaHackathon(
                "Hack4Future",
                sede,
                inizio,
                fine,
                40,
                2,
                organizzatore
        );

        this.dataCorrente = LocalDate.now();
        this.dataBloccata = false;
        this.organizerMode = false;
        this.initialized = true;

        controller.chiudiRegistrazioni();
        controller.disabilitaInvioDocumenti();

        organizerButton.setText("Organizzatore: " + organizzatore.getNomeCompleto());
        organizerButton.setToolTipText("Clicca per entrare/uscire dalla modalita' organizzatore");

        reloadAll();
        showUserMode();
    }

    // =====================================================================================
    // APP
    // =====================================================================================

    /**
     Costruisce il pannello principale dell'applicazione, composto da:
     - header (titolo, modalità e controllo accesso organizzatore),
     - pannello stato (informazioni sintetiche sull'evento),
     - pannello centrale (tab e dettagli),
     - footer (azioni contestuali alla modalità).

     @return pannello Swing della schermata applicativa.
     */
    private JPanel buildAppPanel() {
        JPanel app = new JPanel(new BorderLayout(10, 10));
        app.setBorder(new EmptyBorder(10, 10, 10, 10));

        app.add(buildHeader(), BorderLayout.NORTH);
        app.add(buildStatusPanel(), BorderLayout.WEST);
        app.add(buildCenterPanel(), BorderLayout.CENTER);
        app.add(buildFooterPanel(), BorderLayout.SOUTH);

        return app;
    }

    /**
     Costruisce l'header della schermata principale.

     L'header contiene:
     - bottone con nome organizzatore che consente il toggle della modalità,
     - titolo centrale dell'applicazione,
     - label di modalità e bottone di switch.

     @return pannello Swing dell'header.
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());

        organizerButton = new JButton("Organizzatore: (non configurato)");
        organizerButton.setBorderPainted(false);
        organizerButton.setContentAreaFilled(false);
        organizerButton.setFocusPainted(false);
        organizerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        organizerButton.setFont(FONT_SECTION);
        organizerButton.addActionListener(e -> {
            if (!initialized) return;
            toggleOrganizerMode();
        });
        header.add(organizerButton, BorderLayout.WEST);

        JLabel title = new JLabel("Hackathon Manager", SwingConstants.CENTER);
        title.setFont(FONT_TITLE);
        header.add(title, BorderLayout.CENTER);

        modeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        east.setOpaque(false);
        east.add(modeLabel);

        switchModeButton = new JButton("Passa a ORGANIZZATORE");
        switchModeButton.addActionListener(e -> {
            if (!initialized) return;
            toggleOrganizerMode();
        });
        east.add(switchModeButton);

        header.add(east, BorderLayout.EAST);
        return header;
    }

    /**
     Costruisce il pannello laterale che visualizza lo stato dell'hackathon e della simulazione temporale.

     @return pannello Swing contenente le etichette di stato.
     */
    private JPanel buildStatusPanel() {
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Stato"));

        statusPanel.add(dateLabel);
        statusPanel.add(Box.createVerticalStrut(6));
        statusPanel.add(statusLabel);
        statusPanel.add(Box.createVerticalStrut(6));
        statusPanel.add(judgesInfoLabel);
        statusPanel.add(Box.createVerticalStrut(10));
        statusPanel.add(registrationInfoLabel);
        statusPanel.add(Box.createVerticalStrut(6));
        statusPanel.add(hackathonInfoLabel);
        statusPanel.add(Box.createVerticalStrut(6));
        statusPanel.add(limitsInfoLabel);
        statusPanel.add(Box.createVerticalStrut(6));
        statusPanel.add(uploadLimitInfoLabel);
        statusPanel.add(Box.createVerticalStrut(6));
        statusPanel.add(dayCounterLabel);

        return statusPanel;
    }

    /**
     Costruisce la sezione centrale della UI.

     Elementi principali:
     - {@link JTabbedPane} con tab utenti/team e tab condizionali (giudici, classifica),
     - area di dettaglio che mostra informazioni sull'elemento selezionato o report.

     La tab classifica, se selezionata, ricarica il contenuto tramite {@link #refreshRankingArea()}.

     @return pannello Swing della sezione centrale.
     */
    private JPanel buildCenterPanel() {
        configureLists();

        tabs = new JTabbedPane();
        tabs.addTab(TAB_USERS, new JScrollPane(userList));
        tabs.addTab(TAB_TEAMS, new JScrollPane(teamList));

        judgeTabComponent = buildJudgeTab();

        tabs.addChangeListener(e -> {
            updateButtonsState();
            updateProblemPanel();
            updateRankingTabVisibility();

            int idx = tabs.getSelectedIndex();
            if (idx >= 0 && TAB_RANKING.equals(tabs.getTitleAt(idx))) {
                refreshRankingArea();
            }
        });

        details.setEditable(false);
        details.setFont(FONT_MONO);

        rankingArea.setEditable(false);
        rankingArea.setFont(FONT_MONO);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.add(tabs);
        center.add(new JScrollPane(details));

        return center;
    }

    /**
     Costruisce il footer della UI e registra entrambe le varianti (utente/organizzatore) nel relativo CardLayout.

     @return pannello Swing del footer.
     */
    private JPanel buildFooterPanel() {
        footer.add(buildUserFooter(), FOOTER_USER);
        footer.add(buildOrganizerFooter(), FOOTER_ORG);
        return footer;
    }

    /**
     Configura le liste principali (utenti, team, giudici) impostando modalità di selezione ed eventi di selezione.

     Scelte implementative:
     - Selezione singola per evitare ambiguità nella visualizzazione dei dettagli.
     - Mutua esclusione tra liste: selezionando un elemento in una lista, le altre vengono deselezionate.
     */
    private void configureLists() {
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Utente selected = userList.getSelectedValue();
                if (selected != null) {
                    teamList.clearSelection();
                    judgeList.clearSelection();
                }
                showUserDetails(selected);
            }
        });

        teamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teamList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Team selected = teamList.getSelectedValue();
                if (selected != null) {
                    userList.clearSelection();
                    judgeList.clearSelection();
                }
                showTeamDetails(selected);
            }
        });

        judgeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        judgeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Giudice selected = judgeList.getSelectedValue();
                if (selected != null) {
                    userList.clearSelection();
                    teamList.clearSelection();
                }
                showJudgeDetails(selected);
            }
        });
    }

    /**
     Costruisce il footer in modalità utente, con azioni disponibili ai partecipanti:
     - aggiornamento dati,
     - registrazione utente,
     - eliminazione utente,
     - creazione team,
     - invio documento con valutazione.

     @return pannello Swing del footer utente.
     */
    private JPanel buildUserFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnRefresh = new JButton("Aggiorna");
        btnRefresh.addActionListener(e -> reloadAll());

        JButton btnRegisterUser = new JButton("Registra utente");
        btnRegisterUser.addActionListener(e -> registerUser());

        btnDeleteUser = new JButton("Elimina utente");
        btnDeleteUser.addActionListener(e -> deleteUser());

        JButton btnCreateTeam = new JButton("Crea team");
        btnCreateTeam.addActionListener(e -> createTeam());

        JButton btnUploadDoc = new JButton("Invia documento (+voto)");
        btnUploadDoc.addActionListener(e -> uploadDocumentWithVote());

        p.add(btnRefresh);
        p.add(btnRegisterUser);
        p.add(btnDeleteUser);
        p.add(btnCreateTeam);
        p.add(btnUploadDoc);

        return p;
    }

    /**
     Costruisce il footer in modalità organizzatore, con azioni di gestione dell'evento:
     - impostazione data corrente,
     - apertura/chiusura registrazioni,
     - abilitazione/disabilitazione invio documenti,
     - gestione giudici,
     - conclusione gara,
     - simulazione voti finali e visualizzazione classifica.

     @return pannello Swing del footer organizzatore.
     */
    private JPanel buildOrganizerFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnRefresh = new JButton("Aggiorna");
        btnRefresh.addActionListener(e -> reloadAll());

        btnSetDate = new JButton("Imposta data");
        btnSetDate.addActionListener(e -> setCurrentDate());

        btnOpenReg = new JButton("Apri registrazioni");
        btnOpenReg.addActionListener(e -> {
            try {
                controller.apriRegistrazioni(dataCorrente);
                reloadAll();
            } catch (Exception ex) {
                showError(ex);
            }
        });

        btnCloseReg = new JButton("Chiudi registrazioni");
        btnCloseReg.addActionListener(e -> {
            controller.chiudiRegistrazioni();
            reloadAll();
        });

        btnEnableDocs = new JButton("Abilita invio documenti");
        btnEnableDocs.addActionListener(e -> {
            try {
                controller.abilitaInvioDocumenti(dataCorrente);
                reloadAll();
            } catch (Exception ex) {
                showError(ex);
            }
        });

        btnDisableDocs = new JButton("Disabilita invio documenti");
        btnDisableDocs.addActionListener(e -> {
            controller.disabilitaInvioDocumenti();
            reloadAll();
        });

        btnRegisterJudge = new JButton("Registra giudice");
        btnRegisterJudge.addActionListener(e -> registerJudge());

        btnDeleteJudge = new JButton("Elimina giudice");
        btnDeleteJudge.addActionListener(e -> deleteJudge());

        btnConcludeRace = new JButton("Concludi gara");
        btnConcludeRace.addActionListener(e -> concludeRace());

        JButton btnSimulateVotes = new JButton("Simula voti finali");
        btnSimulateVotes.addActionListener(e -> simulateFinalVotesAndShowAverages());

        btnShowRanking = new JButton(TAB_RANKING);
        btnShowRanking.addActionListener(e -> showRanking());
        btnShowRanking.setVisible(false);

        p.add(btnRefresh);
        p.add(btnSetDate);
        p.add(btnOpenReg);
        p.add(btnCloseReg);
        p.add(btnEnableDocs);
        p.add(btnDisableDocs);
        p.add(btnRegisterJudge);
        p.add(btnDeleteJudge);
        p.add(btnConcludeRace);
        p.add(btnSimulateVotes);
        p.add(btnShowRanking);

        return p;
    }

    // =====================================================================================
    // TAB GIUDICI
    // =====================================================================================

    /**
     Costruisce il contenuto della tab "Giudici" (visibile solo in modalità organizzatore).

     La tab include:
     - pannello superiore per visualizzare/inserire la descrizione del problema,
     - lista dei giudici registrati.

     @return componente Swing da inserire nel {@link JTabbedPane}.
     */
    private Component buildJudgeTab() {
        JPanel judgeTabRoot = new JPanel(new BorderLayout(8, 8));
        judgeTabRoot.setBorder(new EmptyBorder(6, 6, 6, 6));

        JPanel problemPanel = new JPanel(new BorderLayout(6, 6));
        problemPanel.setBorder(BorderFactory.createTitledBorder("Problema"));

        problemCurrentLabel.setText("Problema: (non ancora pubblicato)");
        problemPanel.add(problemCurrentLabel, BorderLayout.NORTH);

        problemArea.setLineWrap(true);
        problemArea.setWrapStyleWord(true);
        problemArea.setFont(FONT_TEXT);
        JScrollPane areaScroll = new JScrollPane(problemArea);
        areaScroll.setPreferredSize(new Dimension(10, 90));
        problemPanel.add(areaScroll, BorderLayout.CENTER);

        btnPublishProblemInTab = new JButton("Pubblica problema");
        btnPublishProblemInTab.addActionListener(e -> publishProblemFromTab());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.add(btnPublishProblemInTab);
        problemPanel.add(btnRow, BorderLayout.SOUTH);

        judgeTabRoot.add(problemPanel, BorderLayout.NORTH);
        judgeTabRoot.add(new JScrollPane(judgeList), BorderLayout.CENTER);

        return judgeTabRoot;
    }

    /**
     Aggiorna il pannello problema della tab giudici in base allo stato corrente del dominio.

     Comportamento:
     - Se il problema non è stato pubblicato, mostra uno stato "non ancora pubblicato" e svuota l'area testo.
     - Se il problema è presente, lo mostra nell'area testo.
     - Abilita il bottone di pubblicazione solo se:
     - modalità organizzatore attiva,
     - almeno 3 giudici registrati,
     - la data corrente non è precedente alla data di inizio hackathon,
     - la data non è bloccata (gara non conclusa).

     In caso di UI non inizializzata, il metodo non effettua operazioni.
     */
    private void updateProblemPanel() {
        if (!initialized) return;
        if (btnPublishProblemInTab == null) return; // l’unico che può essere null

        Hackathon h = controller.getHackathon();
        String desc = h.getDescrizioneProblema();

        if (desc == null || desc.isBlank()) {
            problemCurrentLabel.setText("Problema: (non ancora pubblicato)");
            problemArea.setText(""); // opzionale ma pulito
        } else {
            problemCurrentLabel.setText("Problema pubblicato:");
            problemArea.setText(desc);
        }

        boolean okGiudici = controller.getGiudici().size() >= 3;
        boolean dataOk = !dataCorrente.isBefore(h.getDataInizio());
        btnPublishProblemInTab.setEnabled(organizerMode && okGiudici && dataOk && !dataBloccata);
    }

    /**
     Pubblica la descrizione del problema leggendo il testo dall'area presente nella tab giudici.

     Il metodo delega al controller la pubblicazione e gestisce eventuali errori tramite dialogo.
     Al termine ricarica sempre la UI tramite {@link #reloadAll()}.

     Nota: la pubblicazione può fallire per vincoli di dominio (es. data non valida, giudici insufficienti).
     */
    private void publishProblemFromTab() {
        String descrizione = problemArea.getText();
        if (descrizione == null) descrizione = "";

        try {
            controller.pubblicaProblema(descrizione.trim(), dataCorrente);
            showInfo("Problema pubblicato.");
        } catch (Exception ex) {
            showError(ex);
        } finally {
            reloadAll();
        }
    }

    // =====================================================================================
    // MODALITA'
    // =====================================================================================

    /**
     Commuta la UI tra modalità utente e modalità organizzatore.

     Se la modalità organizzatore è già attiva, il metodo riporta la UI in modalità utente.
     In caso contrario richiede la password dell'organizzatore tramite dialogo e abilita la modalità organizzatore
     soltanto se la password risulta corretta.
     */
    private void toggleOrganizerMode() {
        if (organizerMode) {
            showUserMode();
            return;
        }

        JPasswordField pf = new JPasswordField();
        int res = JOptionPane.showConfirmDialog(
                this,
                pf,
                "Inserisci la password dell'organizzatore",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (res != JOptionPane.OK_OPTION) return;

        String attempt = new String(pf.getPassword());
        if (organizzatore == null || !organizzatore.checkPassword(attempt)) {
            JOptionPane.showMessageDialog(this, "Password errata", TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
            return;
        }

        showOrganizerMode();
    }

    /**
     Imposta la UI in modalità UTENTE:
     - aggiorna etichetta e testo del bottone di switch,
     - mostra il footer utente,
     - rimuove la tab giudici (se presente),
     - carica un testo di guida nell'area dettagli,
     - ricarica i dati visualizzati.

     La modalità utente è quella predefinita dopo il setup.
     */
    private void showUserMode() {
        organizerMode = false;
        modeLabel.setText("Modalita': UTENTE");
        if (switchModeButton != null) switchModeButton.setText("Passa a ORGANIZZATORE");
        footerCards.show(footer, FOOTER_USER);

        updateJudgeTabVisibility();

        details.setText("""
Seleziona un utente, un team o un giudice per vedere i dettagli.

Suggerimento: per entrare in modalita' ORGANIZZATORE clicca su 'Organizzatore: ...' in alto a sinistra.
""");
        reloadAll();
    }

    /**
     Imposta la UI in modalità ORGANIZZATORE:
     - aggiorna etichetta e testo del bottone di switch,
     - mostra il footer organizzatore,
     - aggiunge la tab giudici (se assente),
     - carica un testo di guida nell'area dettagli,
     - ricarica i dati visualizzati.

     L'accesso a questa modalità è protetto da password (gestito in {@link #toggleOrganizerMode()}).
     */
    private void showOrganizerMode() {
        organizerMode = true;
        modeLabel.setText("Modalita': ORGANIZZATORE");
        if (switchModeButton != null) switchModeButton.setText("Passa a UTENTE");
        footerCards.show(footer, FOOTER_ORG);

        updateJudgeTabVisibility();

        details.setText("""
Modalita' organizzatore attiva.

- Puoi impostare la data, aprire/chiudere le registrazioni, abilitare/disabilitare invio documenti.
- Nella tab 'Giudici' puoi aggiungere la descrizione del problema (solo in modalita' organizzatore).
- Dopo l'ultimo giorno puoi 'Concludi gara' e visualizzare la classifica.
""");
        reloadAll();
    }

    /**
     Aggiorna la visibilità della tab "Giudici" nel pannello a tab.

     Regola:
     - In modalità organizzatore la tab deve essere presente.
     - In modalità utente la tab deve essere rimossa.

     Il metodo è safe rispetto a chiamate premature (tabs o componenti null).
     */
    private void updateJudgeTabVisibility() {
        if (tabs == null || judgeTabComponent == null) return;

        int idx = tabs.indexOfTab(TAB_JUDGES);
        if (organizerMode) {
            if (idx < 0) tabs.addTab(TAB_JUDGES, judgeTabComponent);
        } else {
            if (idx >= 0) tabs.removeTabAt(idx);
        }
    }

    /**
     Aggiorna la visibilità della tab "Classifica" nel pannello a tab.

     Regola:
     - La tab viene mostrata dopo la fine dell'hackathon (data corrente successiva alla data fine)
     oppure quando la gara viene conclusa manualmente (flag {@link #dataBloccata}).
     - Se la tab non deve essere visibile, viene rimossa.

     Il contenuto della tab è un {@link JScrollPane} che avvolge {@link #rankingArea}.
     */
    private void updateRankingTabVisibility() {
        if (tabs == null) return;

        Hackathon h = controller.getHackathon();
        boolean show = dataCorrente.isAfter(h.getDataFine()) || dataBloccata;

        int idx = tabs.indexOfTab(TAB_RANKING);
        if (show) {
            if (rankingTabComponent == null) {
                rankingTabComponent = new JScrollPane(rankingArea);
            }
            if (idx < 0) tabs.addTab(TAB_RANKING, rankingTabComponent);
        } else {
            if (idx >= 0) tabs.removeTabAt(idx);
        }
    }

    /**
     Aggiorna il contenuto dell'area classifica interrogando il controller.

     In caso di errore, mostra un messaggio di fallback nell'area testo.
     */
    private void refreshRankingArea() {
        try {
            rankingArea.setText(controller.generaSchermataClassificaCompleta(dataCorrente));
        } catch (Exception ex) {
            rankingArea.setText("Impossibile mostrare la classifica:\n" + ex.getMessage());
        }
    }

    // =====================================================================================
    // Reload / Load
    // =====================================================================================

    /**
     Ricarica lo stato della UI a partire dallo stato corrente di dominio.

     Operazioni principali:
     - sincronizza lo stato "registrazioni aperte" e "invio documenti abilitato" con la data corrente,
     - aggiorna visibilità delle tab condizionali,
     - ricarica liste di utenti/team/giudici,
     - aggiorna pannello di stato e abilitazione pulsanti.

     Se la tab selezionata è la classifica, ricarica anche il contenuto di {@link #rankingArea}.
     */
    private void reloadAll() {
        if (!initialized) return;

        controller.syncRegistrazioniConData(dataCorrente);
        controller.syncInvioDocumentiConData(dataCorrente);

        updateJudgeTabVisibility();
        updateRankingTabVisibility();

        loadUsers();
        loadTeams();
        loadJudges();
        updateStatusLabels();
        updateButtonsState();
        updateUserButtons();
        updateProblemPanel();

        if (tabs != null && tabs.getSelectedIndex() >= 0 &&
                TAB_RANKING.equals(tabs.getTitleAt(tabs.getSelectedIndex()))) {
            refreshRankingArea();
        }
    }

    /**
     Aggiorna lo stato dei pulsanti del footer utente, in particolare il pulsante di eliminazione utente.
     */
    private void updateUserButtons() {
        if (btnDeleteUser != null) {
            boolean hasUsers = controller.getHackathon().getRegistrazioni().size() > 0;
            btnDeleteUser.setEnabled(hasUsers);
        }
    }

    /**
     Popola la lista utenti leggendo le registrazioni dell'hackathon corrente.
     */
    private void loadUsers() {
        userModel.clear();
        for (Registrazione r : controller.getHackathon().getRegistrazioni()) {
            userModel.addElement(r.getUtente());
        }
    }

    /**
     Popola la lista team leggendo i team dell'hackathon corrente.
     */
    private void loadTeams() {
        teamModel.clear();
        for (Team t : controller.getHackathon().getTeam()) {
            teamModel.addElement(t);
        }
    }

    /**
     Popola la lista giudici leggendo la collezione di giudici gestita dal controller.
     */
    private void loadJudges() {
        judgeModel.clear();
        for (Giudice g : controller.getGiudici()) {
            judgeModel.addElement(g);
        }
    }

    /**
     Aggiorna le etichette del pannello stato con le informazioni correnti dell'hackathon.

     Le informazioni visualizzate includono:
     - data corrente e flag di blocco,
     - numero giudici e requisito minimo,
     - finestra registrazioni e stato aperto/chiuso,
     - intervallo hackathon, durata e sede,
     - limiti numerici e stato invio documenti,
     - fase e contatore (giorno registrazioni o hackathon).

     @throws NullPointerException se l'hackathon non è disponibile nel controller (non atteso dopo setup).
     */
    private void updateStatusLabels() {
        Hackathon h = controller.getHackathon();

        dateLabel.setText(" Data corrente: " + dataCorrente + (dataBloccata ? "  [BLOCCATA]" : ""));

        int numGiudici = controller.getGiudici().size();
        String statoGiudici = numGiudici >= 3 ? "[OK]" : "[MANCANO " + (3 - numGiudici) + "]";
        judgesInfoLabel.setText(" Giudici registrati: " + numGiudici + "/3 " + statoGiudici);

        LocalDate regStart = h.getDataInizioRegistrazioni();
        LocalDate regEnd = h.getDataChiusuraRegistrazioni();
        registrationInfoLabel.setText(" Registrazioni: " + regStart + " -> " + regEnd + " (" + h.getDurataRegistrazioniGiorni() + " giorni)" +
                (controller.isRegistrazioniAperteDaOrganizzatore() ? "  [APERTE]" : "  [CHIUSE]"));

        LocalDate hStart = h.getDataInizio();
        LocalDate hEnd = h.getDataFine();
        hackathonInfoLabel.setText(" Hackathon: " + hStart + " -> " + hEnd + " (" + h.getDurataHackathonGiorni() + " giorni)" +
                " | Sede: " + h.getSedeAsString());

        int teamRegistrati = h.getTeam().size();
        int teamMax = h.getMaxTeams();
        int utentiRegistrati = h.getRegistrazioni().size();
        int utentiMax = h.getMaxPartecipanti();
        limitsInfoLabel.setText(
                " Limiti: team " + teamRegistrati + "/" + teamMax
                        + " | partecipanti " + utentiRegistrati + "/" + utentiMax
                        + " | max membri/team: " + h.getMaxTeamSize()
        );

        uploadLimitInfoLabel.setText(" Limite upload: max " + h.getMaxDocumentiAlGiornoPerTeam() + " doc/giorno per team" +
                (controller.isInvioDocumentiAbilitatoDaOrganizzatore() ? "  [ABILITATO]" : "  [DISABILITATO]"));

        statusLabel.setText(" Fase: " + getFaseLabel(h, regStart));
        dayCounterLabel.setText(getContatoreLabel(h, regStart, hStart, hEnd));
    }

    /**
     Determina la descrizione sintetica della fase corrente in base alle regole di dominio e alla data corrente.

     @param h hackathon corrente.
     @param regStart data di inizio registrazioni.
     @return stringa descrittiva della fase.
     */
    private String getFaseLabel(Hackathon h, LocalDate regStart) {
        if (h.isDuranteHackathon(dataCorrente)) return "Durante hackathon";
        if (h.isRegistrazioneConsentita(dataCorrente)) return "Periodo registrazioni";
        if (h.isVotazioneConsentita(dataCorrente)) return "Hackathon terminato";
        if (dataCorrente.isBefore(regStart)) return "Registrazioni non ancora aperte";
        return "Registrazioni chiuse (attesa inizio hackathon)";
    }

    /**
     Calcola una stringa di contatore/avanzamento da mostrare nella UI.

     Casi gestiti:
     - Durante hackathon: giorno corrente dell'evento.
     - Durante registrazioni: giorno corrente della finestra di registrazione.
     - Durante votazione (post evento): mostra la data di fine hackathon.
     - Prima dell'apertura registrazioni: giorni mancanti all'apertura.
     - Attesa tra fine registrazioni e inizio hackathon: mostra la data di inizio hackathon.

     @param h hackathon corrente.
     @param regStart data inizio registrazioni.
     @param hStart data inizio hackathon.
     @param hEnd data fine hackathon.
     @return stringa da visualizzare come contatore.
     */
    private String getContatoreLabel(Hackathon h, LocalDate regStart, LocalDate hStart, LocalDate hEnd) {
        if (h.isDuranteHackathon(dataCorrente)) {
            long giorno = java.time.temporal.ChronoUnit.DAYS.between(hStart, dataCorrente) + 1;
            return " Giorno hackathon: " + giorno + "/" + h.getDurataHackathonGiorni();
        }
        if (h.isRegistrazioneConsentita(dataCorrente)) {
            long giorno = java.time.temporal.ChronoUnit.DAYS.between(regStart, dataCorrente) + 1;
            return " Giorno registrazioni: " + giorno + "/" + h.getDurataRegistrazioniGiorni();
        }
        if (h.isVotazioneConsentita(dataCorrente)) {
            return " Fine hackathon: " + hEnd;
        }
        if (dataCorrente.isBefore(regStart)) {
            long giorniMancanti = java.time.temporal.ChronoUnit.DAYS.between(dataCorrente, regStart);
            return " Apertura tra: " + giorniMancanti + " giorni";
        }
        return " Inizio hackathon: " + hStart;
    }

    /**
     Aggiorna lo stato di abilitazione dei pulsanti del footer organizzatore in base a vincoli temporali e requisiti.

     Il metodo non esegue alcuna operazione se la UI non è in modalità organizzatore.
     */
    private void updateButtonsState() {
        if (!organizerMode) return;

        Hackathon h = controller.getHackathon();
        boolean okGiudici = controller.getGiudici().size() >= 3;

        updateDateButton();
        updateConcludeRaceButton(h, okGiudici);
        updateRegistrationButtons(h, okGiudici);
        updateJudgeButton();
        updateDocumentButtons(h, okGiudici);
        updateRankingButton(h, okGiudici);
    }

    /**
     Aggiorna l'abilitazione del pulsante "Imposta data".

     Regola: la data può essere modificata solo se non è bloccata (gara non conclusa).
     */
    private void updateDateButton() {
        if (btnSetDate != null) {
            btnSetDate.setEnabled(!dataBloccata);
        }
    }

    /**
     Aggiorna l'abilitazione del pulsante "Concludi gara".

     Regola: è abilitato soltanto dopo l'ultimo giorno dell'hackathon, con minimo 3 giudici, e se la data non è bloccata.

     @param h hackathon corrente.
     @param okGiudici true se il numero minimo di giudici è soddisfatto.
     */
    private void updateConcludeRaceButton(Hackathon h, boolean okGiudici) {
        if (btnConcludeRace == null) return;

        boolean dopoUltimoGiorno = dataCorrente.isAfter(h.getDataFine());
        btnConcludeRace.setEnabled(!dataBloccata && okGiudici && dopoUltimoGiorno);
    }

    /**
     Aggiorna l'abilitazione dei pulsanti di apertura/chiusura registrazioni.

     Regole principali:
     - Le registrazioni possono essere aperte solo nella finestra prevista, con giudici ok, e se non sono già aperte.
     - Le registrazioni possono essere chiuse solo se sono aperte e nella finestra prevista.
     - Se la data è bloccata non è possibile modificare lo stato.

     @param h hackathon corrente.
     @param okGiudici true se il numero minimo di giudici è soddisfatto.
     */
    private void updateRegistrationButtons(Hackathon h, boolean okGiudici) {
        boolean inFinestraRegistrazioni = h.isRegistrazioneConsentita(dataCorrente);

        if (btnOpenReg != null) {
            boolean puoAprire = !dataBloccata
                    && okGiudici
                    && inFinestraRegistrazioni
                    && !controller.isRegistrazioniAperteDaOrganizzatore();
            btnOpenReg.setEnabled(puoAprire);
        }

        if (btnCloseReg != null) {
            boolean puoChiudere = !dataBloccata
                    && inFinestraRegistrazioni
                    && controller.isRegistrazioniAperteDaOrganizzatore();
            btnCloseReg.setEnabled(puoChiudere);
        }
    }

    /**
     Aggiorna l'abilitazione dei pulsanti di gestione giudici.

     Regole:
     - "Registra giudice" è abilitato se il controller consente l'aggiunta (es. massimo non superato).
     - "Elimina giudice" è abilitato se esiste almeno un giudice registrato.
     */
    private void updateJudgeButton() {
        if (btnRegisterJudge != null) {
            btnRegisterJudge.setEnabled(controller.canAddJudge());
        }

        if (btnDeleteJudge != null) {
            btnDeleteJudge.setEnabled(!controller.getGiudici().isEmpty());
        }
    }

    /**
     Aggiorna l'abilitazione dei pulsanti relativi all'invio documenti.

     Regole principali:
     - Abilitazione invio documenti: solo durante hackathon, con giudici ok, almeno 2 team, data non bloccata,
     e se non è già abilitato.
     - Disabilitazione invio documenti: solo durante hackathon, data non bloccata, e se è attualmente abilitato.

     @param h hackathon corrente.
     @param okGiudici true se il numero minimo di giudici è soddisfatto.
     */
    private void updateDocumentButtons(Hackathon h, boolean okGiudici) {
        boolean duranteHackathon = h.isDuranteHackathon(dataCorrente);

        if (btnEnableDocs != null) {
            boolean okTeamMin = controller.getHackathon().getTeam().size() >= 2;
            boolean puoAbilitare = !dataBloccata
                    && duranteHackathon
                    && okGiudici
                    && okTeamMin
                    && !controller.isInvioDocumentiAbilitatoDaOrganizzatore();
            btnEnableDocs.setEnabled(puoAbilitare);
        }

        if (btnDisableDocs != null) {
            boolean puoDisabilitare = !dataBloccata
                    && duranteHackathon
                    && controller.isInvioDocumentiAbilitatoDaOrganizzatore();
            btnDisableDocs.setEnabled(puoDisabilitare);
        }
    }

    /**
     Aggiorna visibilità e abilitazione del bottone "Classifica" nel footer organizzatore.

     Regole:
     - Il bottone diventa visibile solo dopo la fine dell'hackathon oppure dopo la conclusione manuale della gara
     (flag {@link #dataBloccata}).
     - L'abilitazione richiede anche il vincolo minimo dei giudici.

     @param h hackathon corrente.
     @param okGiudici true se il numero minimo di giudici è soddisfatto.
     */
    private void updateRankingButton(Hackathon h, boolean okGiudici) {
        if (btnShowRanking == null) return;

        boolean finito = dataBloccata || dataCorrente.isAfter(h.getDataFine());
        btnShowRanking.setVisible(finito);
        btnShowRanking.setEnabled(finito && okGiudici);
    }

    // =====================================================================================
    // DETAILS
    // =====================================================================================

    /**
     Mostra nell'area dettagli le informazioni principali dell'utente selezionato.

     @param u utente selezionato nella lista.
     */
    private void showUserDetails(Utente u) {
        if (u == null) return;
        details.setText("""
UTENTE
- Id: %d
- Nome: %s
- Email: %s

Nota: per inviare un documento devi selezionare un TEAM nella tab 'Team'.
""".formatted(u.getId(), u.getNomeCompleto(), u.getEmail()));
    }

    /**
     Mostra nell'area dettagli le informazioni del team selezionato e un riepilogo dei progressi (documenti).

     Il riepilogo include:
     - numero documenti consegnati e mancanti,
     - somma punti dei documenti consegnati,
     - media progressi con penalizzazione a zero per documenti mancanti (delegata al controller),
     - descrizione del problema se presente.

     @param t team selezionato nella lista.
     */
    private void showTeamDetails(Team t) {
        if (t == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append(t.getDetails());

        int consegnati = controller.getNumeroDocumentiConsegnati(t);
        int slots = controller.getSlotTotaliDocumenti();
        int mancanti = controller.getDocumentiMancanti(t);
        int sommaPunti = controller.getSommaPuntiDocumenti(t);
        double mediaProgressi = controller.calcolaMediaProgressiConZeri(t);

        sb.append("\nPROGRESSI (documenti)\n");
        sb.append("- Consegnati: ").append(consegnati).append("/").append(slots).append(" (mancanti: ").append(mancanti).append(")\n");
        sb.append("- Somma punti consegnati: ").append(sommaPunti).append("\n");
        sb.append("- Media progressi (con 0 per mancanti): ").append(String.format(Locale.ITALY, "%.2f", mediaProgressi)).append("\n");

        String problema = controller.getHackathon().getDescrizioneProblema();
        sb.append("\nProblema (se pubblicato):\n");
        sb.append(problema == null ? "(non ancora pubblicato)" : problema);

        details.setText(sb.toString());
    }

    /**
     Mostra nell'area dettagli le informazioni principali del giudice selezionato.

     @param g giudice selezionato nella lista.
     */
    private void showJudgeDetails(Giudice g) {
        if (g == null) return;
        details.setText("""
GIUDICE
- Id: %d
- Nome: %s
- Email: %s

Nota: per iniziare la gara servono almeno 3 giudici registrati.
""".formatted(g.getId(), g.getNomeCompleto(), g.getEmail()));
    }

    // =====================================================================================
    // AZIONI UTENTE
    // =====================================================================================

    /**
     Gestisce la registrazione di un utente tramite dialogo.

     Il metodo:
     - raccoglie i dati utente (id, nome, cognome, email),
     - costruisce un'istanza di {@link model.Utente},
     - delega al controller la registrazione con la data corrente,
     - mostra feedback e ricarica la UI.

     Eventuali errori (validazioni, parsing id, vincoli di registrazione) vengono mostrati in dialogo.
     */
    private void registerUser() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField surnameField = new JTextField();
        JTextField emailField = new JTextField();

        panel.add(new JLabel("Id (numero):"));
        panel.add(idField);
        panel.add(new JLabel("Nome:"));
        panel.add(nameField);
        panel.add(new JLabel("Cognome:"));
        panel.add(surnameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Registrazione utente", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            int id = Integer.parseInt(idField.getText().trim());
            String nome = nameField.getText().trim();
            String cognome = surnameField.getText().trim();
            String email = emailField.getText().trim();

            Utente u = new Utente(id, nome, cognome, email);
            controller.registraUtente(u, dataCorrente);

            JOptionPane.showMessageDialog(this, "Utente registrato correttamente.", TITLE_OK, JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        } finally {
            reloadAll();
        }
    }

    /**
     Gestisce la creazione di un team tramite dialogo.

     Il metodo:
     - determina gli utenti candidati (registrati ma senza team),
     - mostra una lista con selezione "FIFO" fino al massimo consentito,
     - costruisce l'istanza {@link model.Team} assegnando un id progressivo,
     - delega al controller l'inserimento del team nel dominio.

     In caso di assenza candidati mostra un messaggio informativo e termina.
     */
    private void createTeam() {
        Hackathon h = controller.getHackathon();
        List<Utente> candidati = getCandidatiSenzaTeam(h);

        if (candidati.isEmpty()) {
            showInfo("Non ci sono utenti disponibili (o sono gia tutti in un team).");
            return;
        }

        JTextField teamNameField = new JTextField();
        JList<Utente> list = buildCandidatiListConSelezioneFifo(candidati, h.getMaxTeamSize());

        int result = showCreateTeamDialog(h, teamNameField, list);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            Team t = buildTeamFromDialog(h, teamNameField, list);
            controller.aggiungiTeam(t, dataCorrente);
            JOptionPane.showMessageDialog(this, "Team creato correttamente.", TITLE_OK, JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        } finally {
            reloadAll();
        }
    }

    /**
     Restituisce la lista di utenti registrati che non appartengono ad alcun team.

     @param h hackathon corrente.
     @return lista di candidati che possono essere inseriti in un nuovo team.
     */
    private List<Utente> getCandidatiSenzaTeam(Hackathon h) {
        List<Utente> candidati = new ArrayList<>();
        for (Registrazione r : h.getRegistrazioni()) {
            Utente u = r.getUtente();
            if (h.trovaTeamDiUtente(u) == null) {
                candidati.add(u);
            }
        }
        return candidati;
    }

    /**
     Costruisce una lista Swing di candidati con selezione multipla limitata e gestita in modo FIFO.

     La selezione FIFO permette all'utente di selezionare con click singoli senza usare Ctrl:
     quando si supera il limite {@code maxSel}, la selezione più vecchia viene rimossa automaticamente.

     @param candidati lista di utenti candidati.
     @param maxSel numero massimo di utenti selezionabili.
     @return lista Swing configurata con selezione multipla e gestione FIFO.
     */
    private JList<Utente> buildCandidatiListConSelezioneFifo(List<Utente> candidati, int maxSel) {
        DefaultListModel<Utente> model = new DefaultListModel<>();
        for (Utente u : candidati) model.addElement(u);

        JList<Utente> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // rimuovo listener di default per gestire click FIFO senza Ctrl
        for (java.awt.event.MouseListener ml : list.getMouseListeners()) list.removeMouseListener(ml);
        for (java.awt.event.MouseMotionListener ml : list.getMouseMotionListeners()) list.removeMouseMotionListener(ml);

        attachFifoSelection(list, maxSel);
        return list;
    }

    /**
     Aggancia alla lista un comportamento di selezione FIFO con limite massimo.

     Implementazione:
     - mantiene una coda degli indici selezionati nell'ordine di selezione,
     - se si seleziona un elemento già selezionato viene deselezionato e rimosso dalla coda,
     - se il numero di selezioni supera {@code maxSel} viene rimosso l'elemento selezionato per primo.

     @param list lista Swing a cui applicare il comportamento.
     @param maxSel numero massimo di elementi selezionabili.
     */
    private void attachFifoSelection(JList<Utente> list, int maxSel) {
        final Deque<Integer> ordineSelezione = new ArrayDeque<>();

        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index < 0) return;

                if (list.isSelectedIndex(index)) {
                    list.removeSelectionInterval(index, index);
                    ordineSelezione.remove(index);
                    return;
                }

                list.addSelectionInterval(index, index);
                ordineSelezione.remove(index);
                ordineSelezione.addLast(index);

                while (ordineSelezione.size() > maxSel) {
                    int oldest = ordineSelezione.removeFirst();
                    list.removeSelectionInterval(oldest, oldest);
                }
            }
        });
    }

    /**
     Mostra il dialog di creazione team contenente:
     - campo nome team,
     - lista membri selezionabili.

     @param h hackathon corrente (usato per mostrare il limite di selezione).
     @param teamNameField campo di input del nome team.
     @param list lista di utenti candidati.
     @return risultato del dialog Swing (OK/CANCEL).
     */
    private int showCreateTeamDialog(Hackathon h, JTextField teamNameField, JList<Utente> list) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("Nome team:"), BorderLayout.NORTH);
        panel.add(teamNameField, BorderLayout.CENTER);
        panel.add(new JScrollPane(list), BorderLayout.SOUTH);

        return JOptionPane.showConfirmDialog(
                this,
                panel,
                "Crea team (seleziona fino a " + h.getMaxTeamSize() + " membri)",
                JOptionPane.OK_CANCEL_OPTION
        );
    }

    /**
     Costruisce l'istanza di {@link model.Team} a partire dai dati inseriti nel dialog di creazione.

     @param h hackathon corrente.
     @param teamNameField campo contenente il nome del team.
     @param list lista candidati da cui prelevare i membri selezionati.
     @return team costruito e popolato con i membri selezionati.
     @throws IllegalArgumentException se non è selezionato alcun membro o se si supera il limite di membri.
     */
    private Team buildTeamFromDialog(Hackathon h, JTextField teamNameField, JList<Utente> list) {
        String teamName = teamNameField.getText().trim();
        List<Utente> selected = list.getSelectedValuesList();

        if (selected.isEmpty()) throw new IllegalArgumentException("Devi selezionare almeno un membro");
        if (selected.size() > h.getMaxTeamSize())
            throw new IllegalArgumentException("Troppi membri: massimo " + h.getMaxTeamSize());

        int newId = generateNextTeamId(h.getTeam());
        Team t = new Team(newId, teamName);
        for (Utente u : selected) t.aggiungiMembro(u);
        return t;
    }

    /**
     Calcola il prossimo id di team disponibile come (massimo id esistente + 1).

     @param existing lista di team esistenti.
     @return nuovo id progressivo.
     */
    private int generateNextTeamId(List<Team> existing) {
        int max = 0;
        for (Team t : existing) {
            if (t.getId() > max) max = t.getId();
        }
        return max + 1;
    }

    /**
     Gestisce l'invio di un documento di avanzamento per un team selezionato e la relativa valutazione.

     Regole lato UI:
     - è necessario selezionare un team nella tab "Team";
     - il contenuto viene richiesto tramite dialogo.

     Il caricamento e la valutazione vengono delegati al controller tramite
     {@link controller.Controller#caricaDocumentoEValuta(Team, Documento, LocalDate)}.
     */
    private void uploadDocumentWithVote() {
        Team team = teamList.getSelectedValue();
        if (team == null) {
            showInfo("Seleziona prima un team (tab 'Team').");
            return;
        }

        String contenuto = JOptionPane.showInputDialog(this, "Contenuto del documento di avanzamento:");
        if (contenuto == null) return;

        try {
            Documento d = new Documento(contenuto.trim(), dataCorrente);
            String riepilogo = controller.caricaDocumentoEValuta(team, d, dataCorrente);
            JOptionPane.showMessageDialog(this, "Documento inviato!\n\n" + riepilogo, TITLE_OK, JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        } finally {
            reloadAll();
        }
    }

    // =====================================================================================
    // AZIONI ORGANIZZATORE
    // =====================================================================================

    /**
     Gestisce la registrazione di un giudice tramite dialogo.

     L'id del giudice viene assegnato automaticamente come (numero giudici attuali + 1), assumendo una numerazione 1..3.
     La creazione dell'istanza {@link model.Giudice} e l'aggiunta vengono delegate al controller.
     */
    private void registerJudge() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField nameField = new JTextField();
        JTextField surnameField = new JTextField();
        JTextField emailField = new JTextField();

        int nextId = controller.getGiudici().size() + 1; // 1..3

        panel.add(new JLabel("Id (auto):"));
        panel.add(new JLabel(String.valueOf(nextId)));
        panel.add(new JLabel("Nome:"));
        panel.add(nameField);
        panel.add(new JLabel("Cognome:"));
        panel.add(surnameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Registra giudice", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String nome = nameField.getText().trim();
            String cognome = surnameField.getText().trim();
            String email = emailField.getText().trim();

            controller.aggiungiGiudice(new Giudice(nextId, nome, cognome, email));
            JOptionPane.showMessageDialog(
                    this,
                    "Giudice registrato. Totale giudici: " + controller.getGiudici().size() + "/3",
                    TITLE_OK,
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception ex) {
            showError(ex);
        } finally {
            reloadAll();
        }
    }

    /**
     Gestisce l'eliminazione di un giudice tramite richiesta dell'id.

     Delegando al controller l'operazione, eventuali errori (id non valido o giudice non trovato) vengono gestiti
     con dialogo.

     Nota: a livello informativo, la UI segnala che id/email possono essere riutilizzati dopo l'eliminazione.
     */
    private void deleteJudge() {
        if (controller.getGiudici().isEmpty()) {
            showInfo("Nessun giudice da eliminare.");
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Inserisci ID giudice da eliminare:");
        if (input == null) return;

        try {
            int id = Integer.parseInt(input.trim());
            controller.eliminaGiudicePerId(id);
            showInfo("Giudice eliminato (id=" + id + "). Ora puoi riutilizzare id/email.");
        } catch (Exception ex) {
            showError(ex);
        } finally {
            reloadAll();
        }
    }

    /**
     Gestisce l'eliminazione di un utente tramite richiesta dell'id.

     L'eliminazione viene delegata al controller; eventuali errori (id non valido o utente non trovato) vengono mostrati.
     */
    private void deleteUser() {
        Hackathon h = controller.getHackathon();
        if (h.getRegistrazioni().isEmpty()) {
            showInfo("Nessun utente registrato da eliminare.");
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Inserisci ID utente da eliminare:");
        if (input == null) return;

        try {
            int id = Integer.parseInt(input.trim());
            controller.eliminaUtentePerId(id);
            showInfo("Utente eliminato (id=" + id + "). Ora puoi riutilizzare id/email.");
        } catch (Exception ex) {
            showError(ex);
        } finally {
            reloadAll();
        }
    }

    /**
     Imposta la data corrente usata dall'applicazione per determinare la fase dell'evento e abilitare le operazioni.

     La data può essere modificata solo se {@link #dataBloccata} è false.
     Il formato richiesto è ISO-8601 (YYYY-MM-DD); in caso di parsing fallito viene mostrato un dialog di errore.
     */
    private void setCurrentDate() {
        if (dataBloccata) {
            showInfo("La data e' bloccata: hai terminato la gara.");
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Inserisci la data corrente (YYYY-MM-DD):", dataCorrente.toString());
        if (input == null) return;

        try {
            dataCorrente = LocalDate.parse(input.trim());
            reloadAll();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Formato data non valido. Usa YYYY-MM-DD (es. 2026-01-09).",
                    TITLE_ERROR,
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     Conclude la gara bloccando la data e abilitando le funzionalità legate alla classifica.

     Vincolo lato UI:
     - la gara può essere conclusa solo dal giorno successivo alla data di fine hackathon.

     Effetti:
     - imposta {@link #dataBloccata} a true,
     - ricarica la UI,
     - mostra un messaggio guida nell'area dettagli.
     */
    private void concludeRace() {
        Hackathon h = controller.getHackathon();

        if (!dataCorrente.isAfter(h.getDataFine())) {
            showInfo("Puoi concludere la gara solo dal giorno SUCCESSIVO alla fine dell'hackathon (dopo " + h.getDataFine() + ").");
            return;
        }

        dataBloccata = true;
        reloadAll();

        details.setText("""
Gara conclusa.

Ora puoi premere il tasto 'Classifica' per vedere la classifica finale.
""");
    }

    /**
     Richiede al controller la simulazione dei voti finali e mostra un report testuale nell'area dettagli.

     Il metodo è pensato come funzionalità di supporto/diagnostica: delega al controller la generazione della schermata
     e poi ricarica la UI.
     */
    private void simulateFinalVotesAndShowAverages() {
        try {
            String screen = controller.simulaVotiFinaliEReport(dataCorrente);
            details.setText(screen);
        } catch (Exception ex) {
            showError(ex);
        } finally {
            reloadAll();
        }
    }

    /**
     Mostra la classifica completa nell'area dettagli, richiedendola al controller.

     In caso di errore mostra un dialog di errore.
     */
    private void showRanking() {
        try {
            details.setText(controller.generaSchermataClassificaCompleta(dataCorrente));
        } catch (Exception ex) {
            showError(ex);
        }
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    /**
     Mostra un dialog di errore contenente il messaggio dell'eccezione.

     @param ex eccezione da visualizzare.
     */
    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
    }

    /**
     Mostra un dialog informativo con un messaggio.

     @param msg messaggio informativo da visualizzare.
     */
    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, TITLE_INFO, JOptionPane.INFORMATION_MESSAGE);
    }
}
