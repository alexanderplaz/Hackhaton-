public class Registrazione {
    private Utente utente;
    private Hackathon hackathon;

    public Registrazione(Utente utente, Hackathon hackathon) {
        this.utente = utente;
        this.hackathon = hackathon;
    }

    public Utente getUtente() { return utente; }
    public Hackathon getHackathon() { return hackathon; }
}