package sn.jaayxamxam.api.web;

// Le profil attend d'être validé ; le code de gestion est à conserver par le travailleur
public record InscriptionResponse(Long id, String codeGestion, String statut) {
}
