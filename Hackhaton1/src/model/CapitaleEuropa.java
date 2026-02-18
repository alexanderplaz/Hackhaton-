package model;

/**
 Enum che rappresenta le possibili sedi dell'hackathon.

 La sede deve essere una capitale europea e il dominio ammissibile
 è modellato tramite enum per garantire validazione a compile-time.

 Ogni costante è associata al nome del relativo paese in formato maiuscolo.
 La rappresentazione testuale è pensata per la UI.
 */
public enum CapitaleEuropa {

    ROMA("ITALIA"),
    PARIGI("FRANCIA"),
    BERLINO("GERMANIA"),
    MADRID("SPAGNA"),
    LISBONA("PORTOGALLO"),
    LONDRA("REGNO_UNITO"),
    DUBLINO("IRLANDA"),
    BRUXELLES("BELGIO"),
    AMSTERDAM("PAESI_BASSI"),
    LUSSEMBURGO("LUSSEMBURGO"),
    VIENNA("AUSTRIA"),
    PRAGA("REPUBBLICA_CECA"),
    VARSAVIA("POLONIA"),
    BUDAPEST("UNGHERIA"),
    BRATISLAVA("SLOVACCHIA"),
    LJUBLJANA("SLOVENIA"),
    ZAGABRIA("CROAZIA"),
    SARAJEVO("BOSNIA_ERZEGOVINA"),
    BELGRADO("SERBIA"),
    PODGORICA("MONTENEGRO"),
    TIRANA("ALBANIA"),
    SKOPJE("MACEDONIA_DEL_NORD"),
    ATENE("GRECIA"),
    SOFIA("BULGARIA"),
    BUCAREST("ROMANIA"),
    CHISINAU("MOLDOVA"),
    KYIV("UCRAINA"),
    HELSINKI("FINLANDIA"),
    STOCCOLMA("SVEZIA"),
    OSLO("NORVEGIA"),
    COPENHENAGEN("DANIMARCA"),
    REYKJAVIK("ISLANDA"),
    TALLINN("ESTONIA"),
    RIGA("LETTONIA"),
    VILNIUS("LITUANIA"),
    VALLETTA("MALTA"),
    NICOSIA("CIPRO"),
    ANKARA("TURCHIA"),
    MOSCA("RUSSIA"),
    MINSK("BIELORUSSIA");

    /**
     Nome del paese associato alla capitale.
     */
    private final String paeseCaps;

    /**
     Costruisce la costante enum associando il paese alla capitale.
     */
    CapitaleEuropa(String paeseCaps) {
        this.paeseCaps = paeseCaps;
    }

    /**
     Restituisce la rappresentazione testuale nel formato:
     CAPITALE (PAESE).
     */
    @Override
    public String toString() {
        return name() + " (" + paeseCaps + ")";
    }
}
