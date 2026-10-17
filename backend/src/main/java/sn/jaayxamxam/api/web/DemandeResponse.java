package sn.jaayxamxam.api.web;

// Réponse à la création d'une demande : le code de suivi est à conserver par le client
public record DemandeResponse(Long id, String codeSuivi, String statut) {
}
