-- ================================
-- SEZIONE DI PULIZIA (RESET SCHEMA)
-- ================================

-- Rimozione preventiva degli indici se esistono.
-- Serve a evitare errori in fase di DROP TABLE o ricreazione schema.
DROP INDEX IF EXISTS idx_utente_email;
DROP INDEX IF EXISTS idx_giudice_email;
DROP INDEX IF EXISTS idx_voto_team;
DROP INDEX IF EXISTS idx_voto_giudice;

-- Eliminazione delle tabelle in ordine logico inverso rispetto alle dipendenze.
-- CASCADE rimuove automaticamente eventuali vincoli o oggetti dipendenti.
DROP TABLE IF EXISTS "Voto" CASCADE;
DROP TABLE IF EXISTS "Documento" CASCADE;
DROP TABLE IF EXISTS "Team" CASCADE;
DROP TABLE IF EXISTS "Hackathon" CASCADE;
DROP TABLE IF EXISTS "Giudice" CASCADE;
DROP TABLE IF EXISTS "Utente" CASCADE;
DROP TABLE IF EXISTS "Organizzatore" CASCADE;


-- ================================
-- CREAZIONE TABELLE ANAGRAFICHE
-- ================================

-- Tabella degli utenti partecipanti.
-- Email è univoca per garantire identificazione logica del soggetto.
CREATE TABLE "Utente" (
                          "Id"       INTEGER PRIMARY KEY,              -- Identificativo applicativo (gestito dal codice)
                          "Nome"     VARCHAR(100) NOT NULL,            -- Nome dell'utente
                          "Cognome"  VARCHAR(100) NOT NULL,            -- Cognome dell'utente
                          "Email"    VARCHAR(255) NOT NULL UNIQUE      -- Email univoca
);

-- Tabella dei giudici.
-- Struttura simile a Utente ma separata per ruolo logico distinto.
CREATE TABLE "Giudice" (
                           "Id"       INTEGER PRIMARY KEY,
                           "Nome"     VARCHAR(100) NOT NULL,
                           "Cognome"  VARCHAR(100) NOT NULL,
                           "Email"    VARCHAR(255) NOT NULL UNIQUE
);

-- Tabella degli organizzatori.
-- La password è memorizzata come stringa (in produzione andrebbe salvata hashata).
CREATE TABLE "Organizzatore" (
                                 "Id"       INTEGER PRIMARY KEY,
                                 "Nome"     VARCHAR(100) NOT NULL,
                                 "Cognome"  VARCHAR(100) NOT NULL,
                                 "Password" VARCHAR(255)
);


-- ================================
-- TABELLE CORE DELL'HACKATHON
-- ================================

-- Tabella Hackathon.
-- SERIAL genera automaticamente l'identificativo.
CREATE TABLE "Hackathon" (
                             "Id" SERIAL PRIMARY KEY,
                             "Titolo" VARCHAR(200) NOT NULL
);

-- Tabella Team.
-- RefHackathon è una foreign key verso Hackathon.
CREATE TABLE "Team" (
                        "Id"           SERIAL PRIMARY KEY,
                        "Nome"         VARCHAR(150) NOT NULL,
                        "RefHackathon" INTEGER,
                        FOREIGN KEY ("RefHackathon") REFERENCES "Hackathon"("Id")
);


-- ================================
-- TABELLE OPERATIVE (DOCUMENTI E VOTI)
-- ================================

-- Tabella Documento.
-- Ogni documento è associato obbligatoriamente a un Team.
-- ON DELETE CASCADE garantisce che eliminando un Team vengano eliminati anche i documenti associati.
CREATE TABLE "Documento" (
                             "Id"        SERIAL PRIMARY KEY,
                             "RefTeam"   INTEGER NOT NULL,
                             "Timestamp" TIMESTAMP NOT NULL,
                             "Contenuto" TEXT NOT NULL,
                             FOREIGN KEY ("RefTeam") REFERENCES "Team"("Id") ON DELETE CASCADE
);

-- Tabella Voto.
-- Ogni voto è associato a una coppia (Giudice, Team).
-- Il vincolo UNIQUE impedisce che lo stesso giudice voti due volte lo stesso team.
-- Il CHECK vincola il punteggio all'intervallo 0..10.
CREATE TABLE "Voto" (
                        "Id"         SERIAL PRIMARY KEY,
                        "Punteggio"  INTEGER NOT NULL CHECK ("Punteggio" BETWEEN 0 AND 10),
                        "RefGiudice" INTEGER NOT NULL,
                        "RefTeam"    INTEGER NOT NULL,
                        FOREIGN KEY ("RefGiudice") REFERENCES "Giudice"("Id") ON DELETE CASCADE,
                        FOREIGN KEY ("RefTeam") REFERENCES "Team"("Id") ON DELETE CASCADE,
                        UNIQUE ("RefGiudice", "RefTeam")
);


-- ================================
-- CREAZIONE INDICI
-- ================================

-- Indici sulle email per velocizzare ricerche per login o lookup anagrafico.
CREATE INDEX idx_utente_email  ON "Utente"("Email");
CREATE INDEX idx_giudice_email ON "Giudice"("Email");

-- Indici sui riferimenti di Voto per velocizzare query aggregate per team o giudice.
CREATE INDEX idx_voto_team     ON "Voto"("RefTeam");
CREATE INDEX idx_voto_giudice  ON "Voto"("RefGiudice");

-- Indice sui documenti per team per velocizzare conteggi o recupero storico.
CREATE INDEX idx_documento_team ON "Documento"("RefTeam");
