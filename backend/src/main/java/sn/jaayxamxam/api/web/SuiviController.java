package sn.jaayxamxam.api.web;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Course;
import sn.jaayxamxam.api.domain.Demande;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.domain.StatutCourse;
import sn.jaayxamxam.api.domain.StatutDemande;
import sn.jaayxamxam.api.repository.CourseRepository;
import sn.jaayxamxam.api.repository.DemandeRepository;

import java.util.Optional;

// Le code de suivi est le seul secret du client : il remplace un compte pour l'instant
@RestController
@RequestMapping("/api/suivi")
public class SuiviController {

    private final DemandeRepository demandes;
    private final CourseRepository courses;

    public SuiviController(DemandeRepository demandes, CourseRepository courses) {
        this.demandes = demandes;
        this.courses = courses;
    }

    // GET /api/suivi/{code}
    @GetMapping("/{code}")
    @Transactional(readOnly = true)
    public SuiviResponse suivre(@PathVariable String code) {
        String propre = code.trim().toUpperCase();

        Optional<Demande> demande = demandes.findByCodeSuivi(propre);
        if (demande.isPresent()) return depuisDemande(demande.get());

        return courses.findByCodeSuivi(propre)
                .map(SuiviController::depuisCourse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
    }

    private static Double position(Double coordonnee, boolean exacte) {
        return exacte ? coordonnee : Positions.flouter(coordonnee);
    }

    private static SuiviResponse depuisDemande(Demande d) {
        Artisan a = d.getArtisan();
        // Position exacte seulement quand le travail est engagé : le client doit voir l'artisan arriver.
        boolean enCours = d.getStatut() == StatutDemande.ACCEPTEE || d.getStatut() == StatutDemande.EN_ROUTE;
        SuiviResponse.IntervenantDto intervenant = a == null ? null : new SuiviResponse.IntervenantDto(
                a.getNom(), a.getTelephone(), a.getMetier().getIcone(), a.getMetier().getFamille(), a.getPhoto(),
                position(a.getLatitude(), enCours), position(a.getLongitude(), enCours),
                a.getDerniereActivite());

        return new SuiviResponse("demande", d.getCodeSuivi(), d.getStatut().name(), d.getCreeLe(),
                d.getDescription(), null, intervenant);
    }

    private static SuiviResponse depuisCourse(Course c) {
        Livreur l = c.getLivreur();
        boolean enCours = c.getStatut() == StatutCourse.ACCEPTEE || c.getStatut() == StatutCourse.EN_ROUTE;
        SuiviResponse.IntervenantDto intervenant = l == null ? null : new SuiviResponse.IntervenantDto(
                l.getNom(), l.getTelephone(), l.getVehicule(), l.getVehicule(), l.getPhoto(),
                position(l.getLatitude(), enCours), position(l.getLongitude(), enCours),
                l.getDerniereActivite());

        String trajet = c.getDepart().getQuartier() + " → " + c.getDestination().getQuartier();
        String resume = c.getDescription() == null ? trajet : trajet + " · " + c.getDescription();

        return new SuiviResponse("course", c.getCodeSuivi(), c.getStatut().name(), c.getCreeLe(),
                resume, c.getPrixEstime(), intervenant);
    }
}
