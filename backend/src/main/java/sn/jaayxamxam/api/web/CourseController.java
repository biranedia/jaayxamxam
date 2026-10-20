package sn.jaayxamxam.api.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.CodeSuivi;
import sn.jaayxamxam.api.domain.Codes;
import sn.jaayxamxam.api.domain.Course;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.domain.Vehicule;
import sn.jaayxamxam.api.domain.Zone;
import sn.jaayxamxam.api.repository.CourseRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;
import sn.jaayxamxam.api.repository.ZoneRepository;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRepository courses;
    private final ZoneRepository zones;
    private final LivreurRepository livreurs;

    public CourseController(CourseRepository courses, ZoneRepository zones, LivreurRepository livreurs) {
        this.courses = courses;
        this.zones = zones;
        this.livreurs = livreurs;
    }

    // POST /api/courses : enregistre la course et renvoie le prix annoncé au client
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CourseResponse creer(@Valid @RequestBody CourseRequest req) {
        String telephone = Telephone.normaliser(req.clientTelephone());
        if (telephone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numéro de téléphone invalide");
        }
        if (!Vehicule.existe(req.vehicule())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Véhicule inconnu");
        }

        Zone depart = quartier(req.departZoneId());
        Zone destination = quartier(req.destinationZoneId());

        Course course = new Course();
        course.setClientNom(req.clientNom().trim());
        course.setClientTelephone(telephone);
        course.setVehicule(req.vehicule());
        course.setDepart(depart);
        course.setDestination(destination);
        course.setRepere(vide(req.precision()));
        course.setDescription(vide(req.description()));

        int metres = Geo.distanceRoute(
                depart.getLatitude(), depart.getLongitude(),
                destination.getLatitude(), destination.getLongitude());
        course.setDistanceM(metres);

        // Les prix du livreur choisi l'emportent sur le tarif indicatif du véhicule
        if (req.livreurId() != null) {
            Livreur l = livreurs.findById(req.livreurId())
                    .filter(Livreur::isActif)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Livreur introuvable"));
            if (!l.getVehicule().equals(req.vehicule())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ce livreur conduit un autre véhicule");
            }
            course.setLivreur(l);
            course.setPrixEstime(Geo.prix(metres, l.getPrixParKm(), l.getPrixMinimum()));
        } else {
            Tarifs.Tarif tarif = Tarifs.pour(req.vehicule());
            course.setPrixEstime(Geo.prix(metres, tarif.parKm(), tarif.minimum()));
        }

        course.setCodeSuivi(Codes.libre(CodeSuivi.COURSE, c -> courses.findByCodeSuivi(c).isPresent()));
        Course creee = courses.save(course);
        return new CourseResponse(creee.getId(), creee.getCodeSuivi(), creee.getStatut().name(),
                creee.getPrixEstime(), creee.getDistanceM());
    }

    private Zone quartier(Long id) {
        Zone z = zones.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quartier inconnu"));
        if (z.getLatitude() == null || z.getLongitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce quartier n'a pas encore de position : impossible d'estimer le prix");
        }
        return z;
    }

    private static String vide(String texte) {
        if (texte == null) return null;
        String propre = texte.trim();
        return propre.isEmpty() ? null : propre;
    }
}
