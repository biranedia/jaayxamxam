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
import sn.jaayxamxam.api.domain.Avis;
import sn.jaayxamxam.api.domain.Course;
import sn.jaayxamxam.api.domain.Demande;
import sn.jaayxamxam.api.repository.AvisRepository;
import sn.jaayxamxam.api.repository.CourseRepository;
import sn.jaayxamxam.api.repository.DemandeRepository;

import java.util.Optional;

@RestController
@RequestMapping("/api/avis")
public class AvisController {

    private final AvisRepository avis;
    private final DemandeRepository demandes;
    private final CourseRepository courses;

    public AvisController(AvisRepository avis, DemandeRepository demandes, CourseRepository courses) {
        this.avis = avis;
        this.demandes = demandes;
        this.courses = courses;
    }

    // POST /api/avis : seul le détenteur du code de suivi peut noter, et une seule fois
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AvisDto creer(@Valid @RequestBody AvisRequest req) {
        // Le code est tolérant à la casse et aux espaces (saisie sur téléphone)
        String code = req.codeSuivi().trim().toUpperCase();

        Avis a = new Avis();
        a.setNote(req.note());
        a.setCommentaire(req.commentaire() == null || req.commentaire().isBlank()
                ? null : req.commentaire().trim());

        Optional<Demande> demande = demandes.findByCodeSuivi(code);
        if (demande.isPresent()) {
            remplirDepuisDemande(a, demande.get());
        } else {
            Course c = courses.findByCodeSuivi(code)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
            remplirDepuisCourse(a, c);
        }

        return AvisDto.depuis(avis.save(a));
    }

    private void remplirDepuisDemande(Avis a, Demande d) {
        if (d.getArtisan() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette demande n'est liée à aucun artisan");
        }
        if (avis.existsByDemandeId(d.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un avis existe déjà pour cette demande");
        }
        a.setArtisan(d.getArtisan());
        a.setDemande(d);
    }

    private void remplirDepuisCourse(Avis a, Course c) {
        if (c.getLivreur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette course n'est liée à aucun livreur");
        }
        if (avis.existsByCourseId(c.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un avis existe déjà pour cette course");
        }
        a.setLivreur(c.getLivreur());
        a.setCourse(c);
    }
}
