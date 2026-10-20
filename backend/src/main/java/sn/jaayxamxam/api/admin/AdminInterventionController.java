package sn.jaayxamxam.api.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Course;
import sn.jaayxamxam.api.domain.Demande;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.domain.Metier;
import sn.jaayxamxam.api.domain.StatutCourse;
import sn.jaayxamxam.api.domain.StatutDemande;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.CourseRepository;
import sn.jaayxamxam.api.repository.DemandeRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;
import sn.jaayxamxam.api.repository.MetierRepository;

import java.util.List;
import java.util.Set;

/**
 * Les actions qui permettent de reprendre la main sur une situation.
 *
 * <p>Le reste du back-office montre. Ici on intervient : confier une demande que personne n'a
 * prise, corriger un état, ouvrir ou fermer un métier. C'est ce qui fait la différence entre
 * regarder sa plateforme et la piloter.
 *
 * <p>Toutes les actions sont réversibles et toutes laissent une trace dans le journal. Rien n'est
 * jamais effacé : une erreur à 23 h ne doit pas détruire le travail de quelqu'un.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminInterventionController {

    /** Les états qu'on peut poser à la main. NOUVELLE y figure pour pouvoir annuler une erreur. */
    private static final Set<String> ETATS_PERMIS =
            Set.of("NOUVELLE", "ACCEPTEE", "EN_ROUTE", "REFUSEE", "TERMINEE");

    private final DemandeRepository demandes;
    private final CourseRepository courses;
    private final ArtisanRepository artisans;
    private final LivreurRepository livreurs;
    private final MetierRepository metiers;
    private final JournalAdmin journal;

    public AdminInterventionController(DemandeRepository demandes, CourseRepository courses,
                                       ArtisanRepository artisans, LivreurRepository livreurs,
                                       MetierRepository metiers, JournalAdmin journal) {
        this.demandes = demandes;
        this.courses = courses;
        this.artisans = artisans;
        this.livreurs = livreurs;
        this.metiers = metiers;
        this.journal = journal;
    }

    // ---------- Confier un travail ----------

    public record Assignation(@NotNull Long travailleurId) {
    }

    public record Fait(String message) {
    }

    /**
     * Confie une demande à un artisan, ou une course à un livreur.
     *
     * <p>L'intervention qui compte le plus : un client attend depuis deux jours, personne n'a
     * répondu, et c'est à toi de décrocher le téléphone et de désigner quelqu'un. Sans ça, le
     * back-office ne sert qu'à constater les dégâts.
     *
     * <p>L'état passe à ACCEPTEE : du point de vue du client, quelqu'un a pris son travail — ce
     * qui est vrai, puisque tu viens de t'en occuper.
     */
    @PostMapping("/demande/{id}/confier")
    @Transactional
    public Fait confierDemande(@PathVariable Long id, @Valid @RequestBody Assignation requete,
                               HttpServletRequest http) {
        Demande d = demandes.findById(id).orElseThrow(() -> introuvable("Demande"));
        Artisan a = artisans.findById(requete.travailleurId())
                .orElseThrow(() -> introuvable("Artisan"));

        if (!a.isActif()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cet artisan n'est pas visible : validez-le d'abord.");
        }

        d.setArtisan(a);
        d.setStatut(StatutDemande.ACCEPTEE);
        demandes.save(d);
        tracer(http, "CONFIER_DEMANDE", "demande:" + id, a.getNom());
        return new Fait("Demande confiée à " + a.getNom() + ".");
    }

    @PostMapping("/course/{id}/confier")
    @Transactional
    public Fait confierCourse(@PathVariable Long id, @Valid @RequestBody Assignation requete,
                              HttpServletRequest http) {
        Course c = courses.findById(id).orElseThrow(() -> introuvable("Course"));
        Livreur l = livreurs.findById(requete.travailleurId())
                .orElseThrow(() -> introuvable("Livreur"));

        if (!l.isActif()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce livreur n'est pas visible : validez-le d'abord.");
        }

        c.setLivreur(l);
        c.setStatut(StatutCourse.ACCEPTEE);
        courses.save(c);
        tracer(http, "CONFIER_COURSE", "course:" + id, l.getNom());
        return new Fait("Course confiée à " + l.getNom() + ".");
    }

    // ---------- Corriger un état ----------

    /**
     * Pose un état à la main sur une demande ou une course.
     *
     * <p>Sert à clore ce qui s'est réglé par téléphone sans passer par l'application, et à
     * rattraper une fausse manœuvre — d'où la possibilité de revenir à NOUVELLE.
     */
    @PostMapping("/{type}/{id}/etat")
    @Transactional
    public Fait changerEtat(@PathVariable String type, @PathVariable Long id,
                            @RequestParam String valeur, HttpServletRequest http) {
        String vise = valeur == null ? "" : valeur.trim().toUpperCase();
        if (!ETATS_PERMIS.contains(vise)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "État inconnu : " + vise);
        }

        if ("demande".equals(type)) {
            Demande d = demandes.findById(id).orElseThrow(() -> introuvable("Demande"));
            d.setStatut(StatutDemande.valueOf(vise));
            demandes.save(d);
        } else if ("course".equals(type)) {
            Course c = courses.findById(id).orElseThrow(() -> introuvable("Course"));
            c.setStatut(StatutCourse.valueOf(vise));
            courses.save(c);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type inconnu : " + type);
        }

        tracer(http, "ETAT_" + type.toUpperCase(), type + ":" + id, vise);
        return new Fait("État passé à " + vise + ".");
    }

    // ---------- Qui peut prendre ce travail ----------

    public record Candidat(Long id, String nom, String detail, boolean verifie, boolean disponible) {
    }

    /**
     * Les travailleurs à qui l'on peut confier un travail donné.
     *
     * <p>Pour une demande, on propose en priorité le métier demandé ; les disponibles d'abord,
     * puis les vérifiés. C'est l'ordre dans lequel on décrocherait son téléphone.
     */
    @GetMapping("/demande/{id}/candidats")
    @Transactional(readOnly = true)
    public List<Candidat> candidatsDemande(@PathVariable Long id) {
        Demande d = demandes.findById(id).orElseThrow(() -> introuvable("Demande"));
        Long metierId = d.getMetier() == null ? null : d.getMetier().getId();

        return artisans.rechercher(metierId, null).stream()
                .map(a -> new Candidat(a.getId(), a.getNom(),
                        a.getMetier().getNom() + " · " + a.getZone().getQuartier(),
                        a.isVerifie(), a.getDerniereActivite() != null))
                .sorted((x, y) -> {
                    if (x.disponible() != y.disponible()) return x.disponible() ? -1 : 1;
                    if (x.verifie() != y.verifie()) return x.verifie() ? -1 : 1;
                    return x.nom().compareToIgnoreCase(y.nom());
                })
                .limit(30)
                .toList();
    }

    @GetMapping("/course/{id}/candidats")
    @Transactional(readOnly = true)
    public List<Candidat> candidatsCourse(@PathVariable Long id) {
        Course c = courses.findById(id).orElseThrow(() -> introuvable("Course"));

        return livreurs.rechercher(c.getVehicule()).stream()
                .map(l -> new Candidat(l.getId(), l.getNom(),
                        l.getVehicule() + " · jusqu'à " + l.getChargeMaxKg() + " kg",
                        l.isVerifie(), l.getDerniereActivite() != null))
                .sorted((x, y) -> {
                    if (x.disponible() != y.disponible()) return x.disponible() ? -1 : 1;
                    if (x.verifie() != y.verifie()) return x.verifie() ? -1 : 1;
                    return x.nom().compareToIgnoreCase(y.nom());
                })
                .limit(30)
                .toList();
    }

    // ---------- Ouvrir ou fermer un métier ----------

    public record MetierAdmin(Long id, String nom, String famille, String icone, boolean actif,
                              long artisans) {
    }

    /**
     * Tous les métiers, actifs ou non.
     *
     * <p>Jusqu'ici, ouvrir un métier demandait d'écrire une migration SQL. Pour quelqu'un qui
     * dirige sa plateforme, c'est absurde : il doit pouvoir décider lui-même qu'on accepte les
     * frigoristes à partir de lundi.
     */
    @GetMapping("/metiers")
    @Transactional(readOnly = true)
    public List<MetierAdmin> metiers() {
        return metiers.findAll().stream()
                .map(m -> new MetierAdmin(m.getId(), m.getNom(), m.getFamille(), m.getIcone(),
                        m.isActif(), artisans.rechercher(m.getId(), null).size()))
                .sorted((a, b) -> {
                    int f = a.famille().compareTo(b.famille());
                    return f != 0 ? f : a.nom().compareToIgnoreCase(b.nom());
                })
                .toList();
    }

    @PostMapping("/metier/{id}/actif")
    @Transactional
    public Fait activerMetier(@PathVariable Long id,
                              @RequestParam(defaultValue = "true") boolean valeur,
                              HttpServletRequest http) {
        Metier m = metiers.findById(id).orElseThrow(() -> introuvable("Métier"));
        m.setActif(valeur);
        metiers.save(m);
        tracer(http, valeur ? "OUVRIR_METIER" : "FERMER_METIER", "metier:" + id, m.getNom());
        return new Fait(m.getNom() + (valeur ? " est proposé aux visiteurs."
                : " n'est plus proposé. Les artisans déjà inscrits restent visibles."));
    }

    // ---------- Utilitaires ----------

    private static ResponseStatusException introuvable(String quoi) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, quoi + " introuvable");
    }

    private void tracer(HttpServletRequest http, String action, String cible, String detail) {
        String transmise = http.getHeader("X-Forwarded-For");
        String adresse = (transmise != null && !transmise.isBlank())
                ? transmise.split(",")[0].trim() : http.getRemoteAddr();
        journal.enregistrer(adresse, action, cible, detail);
    }
}
