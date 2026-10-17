package sn.jaayxamxam.api.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Corps de POST /api/inscriptions/artisan
public record InscriptionArtisanRequest(
        @NotBlank @Size(max = 120) String nom,
        @NotBlank String telephone,
        @NotNull Long metierId,
        @NotNull Long zoneId,
        @Size(max = 500) String description,
        @Min(0) @Max(70) int anneesExperience,
        @Min(0) Integer tarifIndicatif) {
}
