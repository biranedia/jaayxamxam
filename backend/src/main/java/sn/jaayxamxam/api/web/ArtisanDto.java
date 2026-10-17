package sn.jaayxamxam.api.web;

import sn.jaayxamxam.api.domain.Artisan;

import java.time.LocalDateTime;

// Objet renvoyé au frontend (évite d'exposer directement l'entité JPA).
// Pas de téléphone ici : il se demande profil par profil via /api/contacts (voir ContactController).
public record ArtisanDto(
        Long id,
        String nom,
        String description,
        int anneesExperience,
        Integer tarifIndicatif,
        boolean verifie,
        Long metierId,
        String metier,
        String metierIcone,
        String metierFamille,
        String photo,          // nom du fichier, à demander via /api/photos/{nom}
        Long zoneId,
        String ville,
        String quartier,
        Double latitude,        // arrondie à ~110 m (voir Positions)
        Double longitude,
        LocalDateTime derniereActivite,
        Integer distanceM,        // null si le visiteur n'a pas partagé sa position
        Double noteMoyenne,       // null tant qu'il n'y a aucun avis
        long nombreAvis) {

    public static ArtisanDto depuis(Artisan a) {
        return depuis(a, null, null, 0);
    }

    public static ArtisanDto depuis(Artisan a, Integer distanceM, Double noteMoyenne, long nombreAvis) {
        return new ArtisanDto(
                a.getId(), a.getNom(), a.getDescription(),
                a.getAnneesExperience(), a.getTarifIndicatif(), a.isVerifie(),
                a.getMetier().getId(), a.getMetier().getNom(), a.getMetier().getIcone(), a.getMetier().getFamille(), a.getPhoto(),
                a.getZone().getId(), a.getZone().getVille(), a.getZone().getQuartier(),
                Positions.flouter(a.getLatitude()), Positions.flouter(a.getLongitude()),
                a.getDerniereActivite(),
                distanceM, noteMoyenne, nombreAvis);
    }
}
