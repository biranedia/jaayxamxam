package sn.jaayxamxam.api.web;

// Réponse à la création d'une course : le code de suivi est à conserver par le client
public record CourseResponse(Long id, String codeSuivi, String statut, int prixEstime, int distanceM) {
}
