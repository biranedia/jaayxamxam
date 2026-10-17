package sn.jaayxamxam.api.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Corps de POST /api/avis
public record AvisRequest(
        @NotBlank String codeSuivi,
        @Min(1) @Max(5) int note,
        @Size(max = 1000) String commentaire) {
}
