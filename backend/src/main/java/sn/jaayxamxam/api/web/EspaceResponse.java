package sn.jaayxamxam.api.web;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ce que voit un travailleur dans son espace.
 *
 * <p>Son propre numéro y figure — c'est le sien. Sa position est exacte, pas floutée : lui a le
 * droit de savoir où le système le croit. Le floutage ne concerne que ce qu'on montre au public.
 */
public record EspaceResponse(
        String type,                    // « artisan » ou « livreur »
        Long id,
        String nom,
        String telephone,
        String photo,
        boolean verifie,
        boolean actif,                  // false tant que l'administration n'a pas validé
        boolean refuse,
        String etat,                    // EN_ATTENTE, REFUSE ou VALIDE : lisible côté interface
        String metier,                  // artisan seulement
        String metierIcone,
        String metierFamille,
        String quartier,
        String ville,
        String vehicule,                // livreur seulement
        Integer chargeMaxKg,
        Integer prixParKm,
        Integer prixMinimum,
        Double latitude,
        Double longitude,
        LocalDateTime derniereActivite,
        List<TravailDto> travaux) {

    /** Une demande (artisan) ou une course (livreur) qui lui est adressée. */
    public record TravailDto(
            Long id,
            String codeSuivi,
            String statut,
            String clientNom,
            String clientTelephone,
            String description,
            boolean urgente,
            String lieu,
            Integer prixEstime,         // course seulement
            Integer distanceM,          // course seulement
            LocalDateTime creeLe) {
    }
}
