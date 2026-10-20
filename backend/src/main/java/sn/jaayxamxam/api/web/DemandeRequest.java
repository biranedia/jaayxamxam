package sn.jaayxamxam.api.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corps de POST /api/demandes.
 *
 * <p>La description n'est plus obligatoire : le client peut décrire son problème à la voix.
 * Le contrôleur vérifie qu'il y a bien l'un ou l'autre — une annotation ne sait pas exprimer
 * « ce champ ou celui-là », et une règle de métier lisible vaut mieux qu'un validateur maison.
 */
public record DemandeRequest(
        @NotBlank @Size(max = 120) String clientNom,
        @NotBlank String clientTelephone,
        @Size(max = 2000) String description,

        /**
         * Nom du fichier rendu par POST /api/vocaux. Sa forme est vérifiée ici pour qu'un nom
         * fabriqué à la main ne traverse jamais jusqu'au disque.
         */
        @Size(max = 64)
        @Pattern(regexp = "[0-9a-f]{32}\\.(webm|ogg|m4a)", message = "Enregistrement invalide")
        String vocal,

        boolean urgente,
        @NotNull Long metierId,
        @NotNull Long zoneId,
        Long artisanId,
        Double latitude,
        Double longitude) {
}
