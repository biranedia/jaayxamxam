package sn.jaayxamxam.api.web;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.AvisRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/artisans")
public class ArtisanController {

    private final ArtisanRepository artisans;
    private final AvisRepository avis;

    public ArtisanController(ArtisanRepository artisans, AvisRepository avis) {
        this.artisans = artisans;
        this.avis = avis;
    }

    // GET /api/artisans?metierId=1&zoneId=2&lat=&lon= (tous les paramètres sont facultatifs)
    @GetMapping
    @Transactional(readOnly = true)
    public List<ArtisanDto> rechercher(@RequestParam(required = false) Long metierId,
                                       @RequestParam(required = false) Long zoneId,
                                       @RequestParam(required = false) Double lat,
                                       @RequestParam(required = false) Double lon) {
        List<Artisan> trouves = artisans.rechercher(metierId, zoneId);
        if (trouves.isEmpty()) return List.of();

        // Une seule requête pour toutes les notes, au lieu d'une par artisan
        Map<Long, Stats> stats = Stats.depuis(
                avis.statsArtisans(trouves.stream().map(Artisan::getId).toList()));

        List<ArtisanDto> resultats = trouves.stream().map(a -> {
            Stats s = stats.getOrDefault(a.getId(), Stats.AUCUN);
            Integer distance = Geo.distance(lat, lon, a.getLatitude(), a.getLongitude());
            return ArtisanDto.depuis(a, distance, s.moyenne(), s.nombre());
        }).toList();

        // Avec la position du visiteur, les plus proches passent devant
        if (lat != null && lon != null) {
            return resultats.stream()
                    .sorted(Comparator.comparing(d -> d.distanceM() == null ? Integer.MAX_VALUE : d.distanceM()))
                    .toList();
        }
        return resultats;
    }

    // GET /api/artisans/{id} : fiche détaillée avec note moyenne et avis visibles
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ArtisanDetailDto detail(@PathVariable Long id,
                                   @RequestParam(required = false) Double lat,
                                   @RequestParam(required = false) Double lon) {
        Artisan a = artisans.findById(id)
                .filter(Artisan::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artisan introuvable"));

        List<AvisDto> liste = avis.findByArtisanIdAndMasqueFalseOrderByCreeLeDesc(id)
                .stream().map(AvisDto::depuis).toList();

        Double moyenne = avis.moyenne(id);
        Integer distance = Geo.distance(lat, lon, a.getLatitude(), a.getLongitude());
        return new ArtisanDetailDto(
                ArtisanDto.depuis(a, distance, moyenne, liste.size()), moyenne, liste.size(), liste);
    }
}
