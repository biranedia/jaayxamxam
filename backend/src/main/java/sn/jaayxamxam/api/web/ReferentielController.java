package sn.jaayxamxam.api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.jaayxamxam.api.domain.Metier;
import sn.jaayxamxam.api.domain.Zone;
import sn.jaayxamxam.api.repository.MetierRepository;
import sn.jaayxamxam.api.repository.ZoneRepository;

import java.util.List;

// Listes de référence pour les filtres de recherche
@RestController
@RequestMapping("/api")
public class ReferentielController {

    private final MetierRepository metiers;
    private final ZoneRepository zones;

    public ReferentielController(MetierRepository metiers, ZoneRepository zones) {
        this.metiers = metiers;
        this.zones = zones;
    }

    @GetMapping("/metiers")
    public List<Metier> metiers() {
        return metiers.findByActifTrueOrderByFamilleAscNomAsc();
    }

    @GetMapping("/zones")
    public List<Zone> zones() {
        return zones.findAll();
    }
}
