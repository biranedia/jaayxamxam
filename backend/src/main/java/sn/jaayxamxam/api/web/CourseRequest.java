package sn.jaayxamxam.api.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Corps de POST /api/courses
public record CourseRequest(
        @NotBlank @Size(max = 120) String clientNom,
        @NotBlank String clientTelephone,
        @NotBlank String vehicule,
        @NotNull Long departZoneId,
        @NotNull Long destinationZoneId,
        @Size(max = 200) String precision,
        @Size(max = 500) String description,
        Long livreurId) {
}
