public class Voto {
    private int punteggio; // da 0 a 10
    private Giudice giudice;
    private Team team;

    public Voto(int punteggio, Giudice giudice, Team team) {
        this.punteggio = punteggio;
        this.giudice = giudice;
        this.team = team;
    }

    public int getPunteggio() { return punteggio; }
    public Giudice getGiudice() { return giudice; }
    public Team getTeam() { return team; }
}

