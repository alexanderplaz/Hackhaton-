package dao;

import model.Documento;

import java.util.List;

/**
  DAO per la persistenza dei documenti di avanzamento caricati dai team.
 */
public interface DocumentoDAO {

    /**
      Salva un documento associandolo ad un team.
     */
    void salvaDocumento(int teamId, Documento documento);

    /**
      Legge tutti i documenti associati ad un team (ordinati per timestamp).
     */
    List<Documento> leggiDocumentiPerTeam(int teamId);
}
