package sn.jaayxamxam.api.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Corps de POST /api/demandes
public record DemandeRequest(
        @NotBlank @Size(max = 120) String clientNom,
        @NotBlank String clientTelephone,
        @NotBlank @Size(max = 2000) String description,
        boolean urgente,
        @NotNull Long metierId,
        @NotNull Long zoneId,
        Long artisanId,
        Double latitude,
        Double longitude) {
}
