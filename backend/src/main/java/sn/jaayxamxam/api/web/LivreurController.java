package sn.jaayxamxam.api.web;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.domain.Vehicule;
import sn.jaayxamxam.api.repository.AvisRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/livreurs")
public class LivreurController {

    private final LivreurRepository livreurs;
    private final AvisRepository avis;

    public LivreurController(LivreurRepository livreurs, AvisRepository avis) {
        this.livreurs = livreurs;
        this.avis = avis;
    }

    // GET /api/livreurs?vehicule=moto&lat=&lon= (tous les paramètres sont facultatifs)
    @GetMapping
    @Transactional(readOnly = true)
    public List<LivreurDto> rechercher(@RequestParam(required = false) String vehicule,
                                       @RequestParam(required = false) Double lat,
                                       @RequestParam(required = false) Double lon) {
        if (vehicule != null && !Vehicule.existe(vehicule)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Véhicule inconnu");
        }

        List<Livreur> trouves = livreurs.rechercher(vehicule);
        if (trouves.isEmpty()) return List.of();

        Map<Long, Stats> stats = Stats.depuis(
                avis.statsLivreurs(trouves.stream().map(Livreur::getId).toList()));

        List<LivreurDto> resultats = trouves.stream().map(l -> {
            Stats s = stats.getOrDefault(l.getId(), Stats.AUCUN);
            Integer distance = Geo.distance(lat, lon, l.getLatitude(), l.getLongitude());
            return LivreurDto.depuis(l, distance, s.moyenne(), s.nombre());
        }).toList();

        // Avec la position du visiteur, les plus proches passent devant
        if (lat != null && lon != null) {
            return resultats.stream()
                    .sorted(Comparator.comparing(d -> d.distanceM() == null ? Integer.MAX_VALUE : d.distanceM()))
                    .toList();
        }
        return resultats;
    }
}
