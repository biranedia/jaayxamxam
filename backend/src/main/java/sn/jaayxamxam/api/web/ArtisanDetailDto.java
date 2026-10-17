package sn.jaayxamxam.api.web;

import java.util.List;

// Fiche détaillée : l'artisan, sa note moyenne (null sans avis) et ses avis visibles
public record ArtisanDetailDto(ArtisanDto artisan, Double noteMoyenne, int nombreAvis, List<AvisDto> avis) {
}
