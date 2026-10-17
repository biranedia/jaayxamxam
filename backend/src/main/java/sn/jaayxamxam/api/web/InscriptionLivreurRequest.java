package sn.jaayxamxam.api.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// Corps de POST /api/inscriptions/livreur ; les prix sont facultatifs
public record InscriptionLivreurRequest(
        @NotBlank @jakarta.validation.constraints.Size(max = 120) String nom,
        @NotBlank String telephone,
        @NotBlank String vehicule,
        @Min(1) @Max(5000) int chargeMaxKg,
        @Min(0) Integer prixParKm,
        @Min(0) Integer prixMinimum) {
}
