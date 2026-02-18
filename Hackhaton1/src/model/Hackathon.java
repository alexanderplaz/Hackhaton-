package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 Rappresenta l'entità principale (aggregate root) dell'evento Hackathon nel modello di dominio.

 La classe incapsula lo stato e le regole di business fondamentali dell'evento:
 - finestre temporali per registrazioni e svolgimento dell'hackathon
 - gestione dei giudici e pubblicazione del problema
 - registrazione degli utenti
 - gestione dei team e dei vincoli relativi (capienza, unicità nome, appartenenza utenti)
 - caricamento dei documenti di progresso durante l'hackathon con vincolo giornaliero

 Vincoli temporali implementati:
 - la durata dell'hackathon è fissata a 5 giorni inclusivi
 - il periodo di registrazione dura 2 giorni
 - la registrazione deve risultare chiusa con un anticipo tale da lasciare 2 giorni "morti" prima dell'inizio dell'evento
 (vedi getDataChiusuraRegistrazioni e getDataInizioRegistrazioni)

 Vincoli quantitativi implementati:
 - numero massimo di team registrabili pari a 20
 - numero massimo effettivo di partecipanti derivato da MAX_TEAMS * maxTeamSize
 - massimo 3 documenti al giorno per team

 La classe gestisce collezioni interne (giudici, registrazioni, team) che vengono esposte verso l'esterno
 tramite viste non modificabili (unmodifiableList) per preservare l'incapsulamento.

 Invarianti principali:
 - titolo non nullo e non vuoto
 - sede non nulla
 - dataInizio e dataFine non nulle, con dataFine non precedente a dataInizio
 - durata evento pari a DURATA_HACKATHON_GIORNI (inclusivi)
 - maxTeamSize strettamente positivo
 - organizzatore non nullo
 - maxPartecipanti definito come MAX_TEAMS * maxTeamSize

 Nota: la logica applicativa fa riferimento a LocalDate per la gestione delle finestre temporali.
 */
public class Hackathon {

    /**
     Durata del periodo di registrazione in giorni (inclusivi).
     */
    private static final int DURATA_REGISTRAZIONI_GIORNI = 2;

    /**
     Durata dell'hackathon in giorni (inclusivi).
     */
    private static final int DURATA_HACKATHON_GIORNI = 5;

    /**
     Numero massimo di documenti caricabili da un team in un singolo giorno.
     */
    private static final int MAX_DOCUMENTI_AL_GIORNO_PER_TEAM = 3;

    /**
     Numero massimo di team ammessi all'hackathon.
     */
    private static final int MAX_TEAMS = 20;

    private final String titolo;
    private final CapitaleEuropa sede;
    private final LocalDate dataInizio;
    private final LocalDate dataFine;

    /**
     Numero massimo effettivo di partecipanti, determinato dal vincolo sul numero massimo di team
     e dalla dimensione massima del team.

     Il valore è sempre calcolato come MAX_TEAMS * maxTeamSize, indipendentemente dal parametro
     maxPartecipanti passato al costruttore.
     */
    private final int maxPartecipanti;

    private final int maxTeamSize;

    /**
     Organizzatore responsabile dell'evento.
     Il campo ha visibilità di package per esigenze progettuali del sistema (come definito nel codice).
     */
    final Organizzatore organizzatore;

    private final List<Giudice> giudici = new ArrayList<>();
    private final List<Registrazione> registrazioni = new ArrayList<>();
    private final List<Team> teams = new ArrayList<>();

    /**
     Descrizione testuale del problema pubblicato per l'hackathon.
     È null finché il problema non viene pubblicato.
     */
    private String descrizioneProblema;

    /**
     Costruisce un nuovo Hackathon validando i principali vincoli di dominio.

     Vincoli verificati:
     - titolo non nullo e non vuoto
     - sede non nulla
     - date non nulle e consistenti (dataFine non precedente a dataInizio)
     - durata dell'evento pari a DURATA_HACKATHON_GIORNI (giorni inclusivi)
     - maxTeamSize > 0
     - organizzatore non nullo

     Vincoli derivati:
     - maxPartecipanti viene imposto a MAX_TEAMS * maxTeamSize per coerenza con il vincolo sul numero massimo di team.
     Il parametro maxPartecipanti in input viene ignorato per preservare la firma del costruttore e compatibilità con il resto del codice.

     @param titolo titolo dell'evento
     @param sede sede dell'evento (capitale europea)
     @param dataInizio data di inizio dell'evento
     @param dataFine data di fine dell'evento
     @param maxPartecipanti parametro mantenuto per compatibilità; il valore effettivo viene determinato internamente
     @param maxTeamSize dimensione massima consentita per un team
     @param organizzatore organizzatore responsabile dell'evento
     @throws IllegalArgumentException se uno o più parametri non rispettano i vincoli di validazione
     */
    public Hackathon(String titolo,
                     CapitaleEuropa sede,
                     LocalDate dataInizio,
                     LocalDate dataFine,
                     int maxPartecipanti,
                     int maxTeamSize,
                     Organizzatore organizzatore) {

        if (titolo == null || titolo.isBlank()) {
            throw new IllegalArgumentException("Titolo non valido");
        }
        if (sede == null) {
            throw new IllegalArgumentException("Sede non valida");
        }
        if (dataInizio == null || dataFine == null || dataFine.isBefore(dataInizio)) {
            throw new IllegalArgumentException("Date non valide");
        }

        long durata = ChronoUnit.DAYS.between(dataInizio, dataFine) + 1L;
        if (durata != DURATA_HACKATHON_GIORNI) {
            throw new IllegalArgumentException(
                    "Durata hackathon non valida: deve essere " + DURATA_HACKATHON_GIORNI + " giorni"
            );
        }

        if (maxTeamSize <= 0) {
            throw new IllegalArgumentException("Max team size non valido");
        }
        if (organizzatore == null) {
            throw new IllegalArgumentException("Organizzatore nullo");
        }

        this.titolo = titolo;
        this.sede = sede;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.maxTeamSize = maxTeamSize;
        this.organizzatore = organizzatore;

        this.maxPartecipanti = MAX_TEAMS * maxTeamSize;

        @SuppressWarnings("unused")
        int ignored = maxPartecipanti;
    }

    /**
     Restituisce il titolo dell'hackathon.

     @return titolo dell'evento
     */
    public String getTitolo() {
        return titolo;
    }

    /**
     Restituisce la sede dell'hackathon in formato testuale.
     Questo metodo è pensato come utilità per la GUI.

     @return rappresentazione testuale della sede
     */
    public String getSedeAsString() {
        return sede.toString();
    }

    /**
     Restituisce la data di inizio dell'hackathon.

     @return data di inizio
     */
    public LocalDate getDataInizio() {
        return dataInizio;
    }

    /**
     Restituisce la data di fine dell'hackathon.

     @return data di fine
     */
    public LocalDate getDataFine() {
        return dataFine;
    }

    /**
     Restituisce il numero massimo di partecipanti consentiti.

     Il valore è determinato internamente come MAX_TEAMS * maxTeamSize.

     @return massimo partecipanti
     */
    public int getMaxPartecipanti() {
        return maxPartecipanti;
    }

    /**
     Restituisce la dimensione massima consentita per un team.

     @return massimo numero di membri per team
     */
    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    /**
     Restituisce il numero massimo di team ammessi.

     @return massimo numero di team
     */
    public int getMaxTeams() {
        return MAX_TEAMS;
    }

    /**
     Restituisce la durata dell'hackathon in giorni inclusivi.

     @return durata in giorni
     */
    public int getDurataHackathonGiorni() {
        return DURATA_HACKATHON_GIORNI;
    }

    /**
     Restituisce la durata del periodo di registrazione in giorni inclusivi.

     @return durata registrazioni in giorni
     */
    public int getDurataRegistrazioniGiorni() {
        return DURATA_REGISTRAZIONI_GIORNI;
    }

    /**
     Restituisce il numero massimo di documenti caricabili al giorno per team.

     @return massimo documenti giornalieri per team
     */
    public int getMaxDocumentiAlGiornoPerTeam() {
        return MAX_DOCUMENTI_AL_GIORNO_PER_TEAM;
    }

    /**
     Restituisce la lista dei giudici registrati.

     La collezione restituita è una vista non modificabile della lista interna.

     @return lista non modificabile dei giudici
     */
    public List<Giudice> getGiudici() {
        return Collections.unmodifiableList(giudici);
    }

    /**
     Restituisce la lista delle registrazioni degli utenti.

     La collezione restituita è una vista non modificabile della lista interna.

     @return lista non modificabile delle registrazioni
     */
    public List<Registrazione> getRegistrazioni() {
        return Collections.unmodifiableList(registrazioni);
    }

    /**
     Restituisce la lista dei team registrati.

     La collezione restituita è una vista non modificabile della lista interna.

     @return lista non modificabile dei team
     */
    public List<Team> getTeam() {
        return Collections.unmodifiableList(teams);
    }

    /**
     Calcola la data di inizio del periodo di registrazione.

     Il periodo di registrazione:
     - ha durata DURATA_REGISTRAZIONI_GIORNI giorni inclusivi
     - termina alla data restituita da getDataChiusuraRegistrazioni

     @return data di inizio delle registrazioni
     */
    public LocalDate getDataInizioRegistrazioni() {
        return getDataChiusuraRegistrazioni().minusDays(DURATA_REGISTRAZIONI_GIORNI - 1L);
    }

    /**
     Calcola la data di chiusura delle registrazioni.

     La chiusura è definita in modo da lasciare un intervallo di giorni non utilizzabili
     prima dell'inizio dell'hackathon. Nel modello adottato:
     - le registrazioni devono risultare chiuse a partire da (dataInizio - 2)
     - di conseguenza l'ultimo giorno utile per iscriversi è (dataInizio - 3)

     @return ultimo giorno utile per registrarsi
     */
    public LocalDate getDataChiusuraRegistrazioni() {
        return dataInizio.minusDays(3);
    }

    /**
     Verifica se, in una certa data, è consentita la registrazione di un utente.

     La registrazione è consentita esclusivamente se la data fornita rientra
     nel periodo [getDataInizioRegistrazioni, getDataChiusuraRegistrazioni] (estremi inclusi).

     @param oggi data su cui effettuare la verifica
     @return true se la registrazione è consentita, false altrimenti
     @throws IllegalArgumentException se oggi è null
     */
    public boolean isRegistrazioneConsentita(LocalDate oggi) {
        if (oggi == null) {
            throw new IllegalArgumentException("Data nulla");
        }
        return (!oggi.isBefore(getDataInizioRegistrazioni()))
                && (!oggi.isAfter(getDataChiusuraRegistrazioni()));
    }

    /**
     Verifica se una data ricade nel periodo di svolgimento dell'hackathon.

     Il periodo considerato è [dataInizio, dataFine] inclusivo.

     @param oggi data da verificare
     @return true se oggi è durante l'hackathon, false altrimenti
     @throws IllegalArgumentException se oggi è null
     */
    public boolean isDuranteHackathon(LocalDate oggi) {
        if (oggi == null) {
            throw new IllegalArgumentException("Data nulla");
        }
        return (!oggi.isBefore(dataInizio)) && (!oggi.isAfter(dataFine));
    }

    /**
     Verifica se, in una certa data, è consentito avviare o considerare la fase di votazione.

     Nel modello implementato la votazione è consentita a partire dalla data di fine evento (inclusa).

     @param oggi data da verificare
     @return true se la votazione è consentita, false altrimenti
     @throws IllegalArgumentException se oggi è null
     */
    public boolean isVotazioneConsentita(LocalDate oggi) {
        if (oggi == null) {
            throw new IllegalArgumentException("Data nulla");
        }
        return oggi.isAfter(dataFine) || oggi.isEqual(dataFine);
    }

    /**
     Aggiunge un giudice all'hackathon.

     Vincoli:
     - il giudice non può essere null
     - lo stesso giudice non può essere inserito due volte

     Effetti collaterali:
     - modifica la lista interna dei giudici

     @param g giudice da aggiungere
     @throws IllegalArgumentException se g è null
     @throws IllegalStateException se il giudice è già presente
     */
    public void aggiungiGiudice(Giudice g) {
        if (g == null) {
            throw new IllegalArgumentException("Giudice nullo");
        }
        if (giudici.contains(g)) {
            throw new IllegalStateException("Giudice già presente");
        }
        giudici.add(g);
    }

    /**
     Rimuove un giudice dalla lista interna.

     Metodo di utilità utilizzato tipicamente per rollback applicativo in caso di fallimento
     nello strato di persistenza.

     Se il parametro è null il metodo non produce effetti.

     Effetti collaterali:
     - modifica la lista interna dei giudici

     @param g giudice da rimuovere (se presente)
     */
    public void rimuoviGiudice(Giudice g) {
        if (g == null) {
            return;
        }
        giudici.remove(g);
    }

    /**
     Registra un utente all'hackathon in una data specifica.

     Vincoli:
     - l'utente non può essere null
     - la registrazione deve avvenire nel periodo consentito
     - non è possibile superare il limite massimo di partecipanti (maxPartecipanti)
     - lo stesso utente non può registrarsi più volte

     Effetti collaterali:
     - aggiunge una nuova Registrazione alla collezione interna

     @param u utente da registrare
     @param oggi data in cui si tenta la registrazione
     @throws IllegalArgumentException se u è null
     @throws IllegalStateException se la registrazione non è consentita, se l'evento è pieno o se l'utente è già registrato
     */
    public void registraUtente(Utente u, LocalDate oggi) {
        if (u == null) {
            throw new IllegalArgumentException("Utente nullo");
        }
        if (!isRegistrazioneConsentita(oggi)) {
            throw new IllegalStateException(
                    "Registrazioni chiuse. Periodo: "
                            + getDataInizioRegistrazioni()
                            + " -> "
                            + getDataChiusuraRegistrazioni()
            );
        }

        if (registrazioni.size() >= maxPartecipanti) {
            throw new IllegalStateException(
                    "Hackathon pieno: raggiunto maxPartecipanti (" + maxPartecipanti + ")"
            );
        }

        boolean already = registrazioni.stream().anyMatch(r -> r.getUtente().equals(u));
        if (already) {
            throw new IllegalStateException("Utente già registrato");
        }

        registrazioni.add(new Registrazione(u, this));
    }

    /**
     Rimuove la registrazione associata a un utente, se presente.

     Metodo di utilità utilizzato tipicamente per rollback applicativo in caso di fallimento
     nello strato di persistenza.

     Se il parametro è null il metodo non produce effetti.

     Effetti collaterali:
     - modifica la collezione interna delle registrazioni

     @param u utente la cui registrazione deve essere rimossa
     */
    public void rimuoviRegistrazione(Utente u) {
        if (u == null) {
            return;
        }
        registrazioni.removeIf(r -> r.getUtente().equals(u));
    }

    /**
     Verifica se un utente risulta registrato all'hackathon.

     @param u utente da verificare
     @return true se l'utente è registrato, false altrimenti (anche se u è null)
     */
    public boolean isUtenteRegistrato(Utente u) {
        if (u == null) {
            return false;
        }
        return registrazioni.stream().anyMatch(r -> r.getUtente().equals(u));
    }

    /**
     Registra un team all'hackathon, applicando i vincoli di dominio relativi ai team.

     Vincoli:
     - t non può essere null
     - non si può superare MAX_TEAMS
     - il nome del team deve essere univoco rispetto ai team già registrati (case-insensitive)
     - la creazione/modifica dei team è consentita solo nel periodo di registrazione
     - il team non deve risultare già presente nella lista interna
     - il team deve avere almeno un membro
     - il numero di membri non può superare maxTeamSize
     - ogni membro deve risultare registrato all'hackathon
     - un utente non può appartenere a più di un team contemporaneamente

     Effetti collaterali:
     - aggiunge il team alla collezione interna dei team

     @param t team da aggiungere
     @param oggi data di riferimento per il controllo del periodo di registrazione
     @throws IllegalArgumentException se t è null, se il team è vuoto o se supera maxTeamSize, o se il team non risulta registrato nel contesto corretto
     @throws IllegalStateException se si viola un vincolo di dominio (massimo team, nome duplicato, periodo non valido, duplicazione team, membri non registrati o già assegnati)
     */
    public void aggiungiTeam(Team t, LocalDate oggi) {
        if (t == null) {
            throw new IllegalArgumentException("Team nullo");
        }

        if (teams.size() >= MAX_TEAMS) {
            throw new IllegalStateException("Numero massimo di team raggiunto (" + MAX_TEAMS + ")");
        }

        String newName = normalizeTeamName(t.getNome());
        boolean nameUsed = teams.stream()
                .anyMatch(existing -> normalizeTeamName(existing.getNome()).equals(newName));
        if (nameUsed) {
            throw new IllegalStateException("Nome team già in uso: " + t.getNome());
        }

        if (!isRegistrazioneConsentita(oggi)) {
            throw new IllegalStateException("Non puoi creare/modificare team dopo la chiusura registrazioni");
        }
        if (teams.contains(t)) {
            throw new IllegalStateException("Team già presente");
        }
        if (t.getMembri().isEmpty()) {
            throw new IllegalArgumentException("Team senza membri");
        }
        if (t.getMembri().size() > maxTeamSize) {
            throw new IllegalArgumentException("Team troppo grande (max " + maxTeamSize + ")");
        }

        for (Utente u : t.getMembri()) {
            if (!isUtenteRegistrato(u)) {
                throw new IllegalStateException("Membro non registrato all'hackathon: " + u);
            }
            if (trovaTeamDiUtente(u) != null) {
                throw new IllegalStateException("L'utente è già in un team: " + u);
            }
        }

        teams.add(t);
    }

    /**
     Rimuove un team dalla lista interna, se presente.

     Metodo di utilità utilizzato tipicamente per rollback applicativo in caso di fallimento
     nello strato di persistenza.

     Se il parametro è null il metodo non produce effetti.

     Effetti collaterali:
     - modifica la collezione interna dei team

     @param t team da rimuovere
     */
    public void rimuoviTeam(Team t) {
        if (t == null) {
            return;
        }
        teams.remove(t);
    }

    /**
     Restituisce il team a cui appartiene un determinato utente, se presente.

     @param u utente da ricercare
     @return il team dell'utente, oppure null se l'utente non appartiene ad alcun team o se u è null
     */
    public Team trovaTeamDiUtente(Utente u) {
        if (u == null) {
            return null;
        }
        for (Team t : teams) {
            if (t.getMembri().contains(u)) {
                return t;
            }
        }
        return null;
    }

    /**
     Pubblica la descrizione del problema dell'hackathon.

     Vincoli:
     - descrizione non può essere null o vuota
     - oggi non può essere null
     - la pubblicazione è consentita solo a partire dalla data di inizio dell'hackathon (oggi >= dataInizio)

     Effetti collaterali:
     - imposta il campo descrizioneProblema

     @param descrizione testo del problema
     @param oggi data in cui si tenta la pubblicazione
     @throws IllegalArgumentException se descrizione è nulla/vuota o se oggi è null
     @throws IllegalStateException se si tenta di pubblicare prima dell'inizio dell'evento
     */
    public void pubblicaProblema(String descrizione, LocalDate oggi) {
        if (descrizione == null || descrizione.isBlank()) {
            throw new IllegalArgumentException("Descrizione problema non valida");
        }
        if (oggi == null) {
            throw new IllegalArgumentException("Data nulla");
        }
        if (oggi.isBefore(dataInizio)) {
            throw new IllegalStateException("Il problema può essere pubblicato solo dall'inizio dell'hackathon");
        }
        this.descrizioneProblema = descrizione;
    }

    /**
     Restituisce la descrizione del problema pubblicato.

     @return descrizione del problema, oppure null se non è stata ancora pubblicata
     */
    public String getDescrizioneProblema() {
        return descrizioneProblema;
    }

    /**
     Carica un documento per un team in una data specifica, applicando i vincoli di dominio.

     Vincoli:
     - team e documento non possono essere null
     - il team deve risultare registrato in questo hackathon
     - il caricamento è consentito solo durante il periodo di svolgimento dell'hackathon
     - il problema deve essere stato pubblicato prima di consentire i caricamenti
     - un team non può caricare più di MAX_DOCUMENTI_AL_GIORNO_PER_TEAM documenti nello stesso giorno

     Effetti collaterali:
     - delega al team il caricamento del documento (modificando lo stato del team)

     @param team team che carica il documento
     @param d documento da caricare
     @param oggi data in cui avviene il caricamento
     @throws IllegalArgumentException se team o documento sono null, o se il team non risulta registrato
     @throws IllegalStateException se il caricamento non è consentito (periodo errato, problema non pubblicato, limite giornaliero raggiunto)
     */
    public void caricaDocumento(Team team, Documento d, LocalDate oggi) {
        if (team == null) {
            throw new IllegalArgumentException("Team nullo");
        }
        if (d == null) {
            throw new IllegalArgumentException("Documento nullo");
        }
        if (!teams.contains(team)) {
            throw new IllegalArgumentException("Team non registrato");
        }
        if (!isDuranteHackathon(oggi)) {
            throw new IllegalStateException("Puoi caricare documenti solo durante l'hackathon");
        }

        if (descrizioneProblema == null) {
            throw new IllegalStateException("Il problema non è ancora stato pubblicato dall'organizzatore");
        }

        long caricatiOggi = team.getDocumenti().stream()
                .filter(doc -> doc.getTimestamp().toLocalDate().isEqual(oggi))
                .count();

        if (caricatiOggi >= MAX_DOCUMENTI_AL_GIORNO_PER_TEAM) {
            throw new IllegalStateException(
                    "Limite giornaliero raggiunto: max "
                            + MAX_DOCUMENTI_AL_GIORNO_PER_TEAM
                            + " documenti al giorno per team"
            );
        }

        team.caricaDocumento(d);
    }

    /**
     Normalizza un nome di team ai fini del confronto case-insensitive.

     La normalizzazione applica trim e conversione in maiuscolo usando Locale.ROOT
     per evitare dipendenze dal locale della JVM.

     @param name nome da normalizzare
     @return stringa normalizzata; stringa vuota se name è null
     */
    private static String normalizeTeamName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toUpperCase(Locale.ROOT);
    }
}
