package sn.jaayxamxam.api.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps des requêtes de l'espace travailleur.
 *
 * <p>Le code de gestion voyage toujours dans le corps, jamais dans l'adresse : une URL se retrouve
 * dans les journaux du serveur, dans l'historique du navigateur et dans l'en-tête Referer envoyé
 * aux sites tiers. Ce code est le seul secret du travailleur, il ne doit traîner nulle part.
 *
 * <p>La taille n'est pas contrainte à neuf ici : quelqu'un qui colle son code depuis un message
 * l'amène souvent avec des espaces ou des tirets. C'est le contrôleur qui nettoie puis vérifie.
 */
public final class EspaceRequetes {

    private EspaceRequetes() {
    }

    /** Ouverture de l'espace : on ne fournit que son code. */
    public record Connexion(
            @NotBlank(message = "Le code est obligatoire")
            @Size(max = 20, message = "Code trop long")
            String code) {
    }

    /** Envoi de position : « je suis disponible, et je suis ici ». */
    public record Position(
            @NotBlank(message = "Le code est obligatoire")
            @Size(max = 20, message = "Code trop long")
            String code,

            @NotNull(message = "La latitude est obligatoire")
            @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
            Double latitude,

            @NotNull(message = "La longitude est obligatoire")
            @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
            Double longitude) {
    }

    /** Changement d'état d'une demande ou d'une course par le travailleur concerné. */
    public record ChangementStatut(
            @NotBlank(message = "Le code est obligatoire")
            @Size(max = 20, message = "Code trop long")
            String code,

            @NotBlank(message = "Le nouvel état est obligatoire")
            String statut) {
    }
}
