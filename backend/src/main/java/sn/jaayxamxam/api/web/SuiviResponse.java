package sn.jaayxamxam.api.web;

import java.time.LocalDateTime;

// Ce que le client voit avec son code de suivi, pour une demande comme pour une course
public record SuiviResponse(
        String type,              // "demande" ou "course"
        String codeSuivi,
        String statut,
        LocalDateTime creeLe,
        String resume,
        Integer prixEstime,       // seulement pour une course
        IntervenantDto intervenant) {

    // L'artisan ou le livreur qui s'occupe de la demande, null tant que personne ne l'a prise
    public record IntervenantDto(
            String nom,
            String telephone,
            String cle,           // icône du métier, ou véhicule : donne le dessin
            String famille,       // famille du métier, ou véhicule : donne la couleur
            String photo,
            Double latitude,
            Double longitude,
            LocalDateTime derniereActivite) {
    }
}
