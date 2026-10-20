package sn.jaayxamxam.api.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.CodeSuivi;
import sn.jaayxamxam.api.domain.Course;
import sn.jaayxamxam.api.domain.Demande;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.domain.StatutCourse;
import sn.jaayxamxam.api.domain.StatutDemande;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.AvisRepository;
import sn.jaayxamxam.api.repository.CourseRepository;
import sn.jaayxamxam.api.repository.DemandeRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Le reste du back-office : tableau de bord, listes complètes, travaux, avis, journal.
 *
 * <p>Séparé de {@link AdminController} qui ne traite que la file d'attente : ce sont deux usages
 * distincts, l'un quotidien et rapide, l'autre pour chercher et comprendre.
 *
 * <p>Tout est en lecture sauf la régénération d'un code de gestion. Les listes sont bornées à 100
 * ou 200 lignes : à ce stade la plateforme n'a pas besoin de pagination, et une requête sans borne
 * est une panne qui attend son heure.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminBackOfficeController {

    /** Au-delà, une demande que personne n'a prise est un problème, pas une attente. */
    private static final int HEURES_AVANT_ALERTE = 48;

    private final ArtisanRepository artisans;
    private final LivreurRepository livreurs;
    private final DemandeRepository demandes;
    private final CourseRepository courses;
    private final AvisRepository avis;
    private final JournalAdmin journal;
    private final Sessions sessions;

    public AdminBackOfficeController(ArtisanRepository artisans, LivreurRepository livreurs,
                                     DemandeRepository demandes, CourseRepository courses,
                                     AvisRepository avis, JournalAdmin journal, Sessions sessions) {
        this.artisans = artisans;
        this.livreurs = livreurs;
        this.demandes = demandes;
        this.courses = courses;
        this.avis = avis;
        this.journal = journal;
        this.sessions = sessions;
    }

    // ---------- Tableau de bord ----------

    public record Alerte(String niveau, String texte) {
    }

    public record TableauDeBord(
            long artisansVisibles, long livreursVisibles,
            long enAttente, long refuses,
            long demandesTotal, long demandesSansReponse,
            long coursesTotal, long coursesSansReponse,
            int sessionsOuvertes,
            List<Alerte> alertes) {
    }

    /**
     * Ce qu'on regarde en ouvrant le back-office le matin.
     *
     * <p>Les alertes sont le cœur : une plateforme en bonne santé n'a rien à signaler. Une demande
     * que personne n'a prise depuis deux jours signifie qu'un client attend pour rien — c'est ce
     * qui tue une plateforme de mise en relation, bien avant un défaut technique.
     */
    @GetMapping("/tableau-de-bord")
    @Transactional(readOnly = true)
    public TableauDeBord tableauDeBord() {
        LocalDateTime limite = LocalDateTime.now().minusHours(HEURES_AVANT_ALERTE);

        long enAttente = artisans.findByActifFalseAndRefuseFalseOrderByCreeLeAsc().size()
                + livreurs.findByActifFalseAndRefuseFalseOrderByCreeLeAsc().size();
        long demandesSansReponse = demandes.countByStatutAndCreeLeBefore(StatutDemande.NOUVELLE, limite);
        long coursesSansReponse = courses.countByStatutAndCreeLeBefore(StatutCourse.NOUVELLE, limite);
        long artisansVisibles = artisans.countByActifTrue();
        long livreursVisibles = livreurs.countByActifTrue();

        List<Alerte> alertes = new ArrayList<>();
        if (enAttente > 0) {
            alertes.add(new Alerte("attention",
                    enAttente + (enAttente > 1 ? " inscriptions attendent" : " inscription attend")
                            + " votre validation."));
        }
        if (demandesSansReponse + coursesSansReponse > 0) {
            long n = demandesSansReponse + coursesSansReponse;
            alertes.add(new Alerte("urgent",
                    n + (n > 1 ? " clients attendent" : " client attend") + " depuis plus de "
                            + HEURES_AVANT_ALERTE + " h sans qu'on leur réponde."));
        }
        if (artisansVisibles == 0) {
            alertes.add(new Alerte("urgent",
                    "Aucun artisan visible : la recherche ne renvoie rien aux visiteurs."));
        }
        if (alertes.isEmpty()) {
            alertes.add(new Alerte("calme", "Rien à signaler."));
        }

        return new TableauDeBord(artisansVisibles, livreursVisibles, enAttente,
                artisans.countByRefuseTrue() + livreurs.countByRefuseTrue(),
                demandes.count(), demandesSansReponse,
                courses.count(), coursesSansReponse,
                sessions.ouvertes(), alertes);
    }

    // ---------- Liste complète des travailleurs ----------

    public record Travailleur(Long id, String type, String nom, String telephone, String detail,
                              String etat, boolean verifie, String photo, String codeGestion,
                              LocalDateTime derniereActivite, LocalDateTime creeLe) {
    }

    /**
     * Tous les inscrits, filtrables.
     *
     * <p>Le code de gestion figure ici : c'est le seul endroit où le retrouver quand un artisan
     * appelle en disant qu'il l'a perdu. C'est aussi pour cela que cet écran est protégé comme
     * il l'est — il contient de quoi se faire passer pour n'importe lequel d'entre eux.
     */
    @GetMapping("/travailleurs")
    @Transactional(readOnly = true)
    public List<Travailleur> travailleurs(@RequestParam(required = false) String etat,
                                          @RequestParam(required = false) String q) {
        String recherche = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Travailleur> tous = new ArrayList<>();

        artisans.findAll().forEach(a -> tous.add(new Travailleur(
                a.getId(), "artisan", a.getNom(), a.getTelephone(),
                a.getMetier().getNom() + " · " + a.getZone().getQuartier(),
                etatDe(a.isActif(), a.isRefuse()), a.isVerifie(), a.getPhoto(),
                a.getCodeGestion(), a.getDerniereActivite(), a.getCreeLe())));

        livreurs.findAll().forEach(l -> tous.add(new Travailleur(
                l.getId(), "livreur", l.getNom(), l.getTelephone(),
                l.getVehicule() + " · jusqu'à " + l.getChargeMaxKg() + " kg",
                etatDe(l.isActif(), l.isRefuse()), l.isVerifie(), l.getPhoto(),
                l.getCodeGestion(), l.getDerniereActivite(), l.getCreeLe())));

        return tous.stream()
                .filter(t -> etat == null || etat.isBlank() || etat.equalsIgnoreCase(t.etat()))
                .filter(t -> recherche.isEmpty()
                        || t.nom().toLowerCase(Locale.ROOT).contains(recherche)
                        || (t.telephone() != null && t.telephone().contains(recherche)))
                .sorted(Comparator.comparing(Travailleur::creeLe).reversed())
                .limit(200)
                .toList();
    }

    // ---------- Demandes et courses ----------

    public record Travail(Long id, String type, String codeSuivi, String statut, String clientNom,
                          String clientTelephone, String resume, String vocal, String assigneA,
                          LocalDateTime creeLe) {
    }

    @GetMapping("/travaux")
    @Transactional(readOnly = true)
    public List<Travail> travaux(@RequestParam(required = false) String statut) {
        List<Travail> tous = new ArrayList<>();

        for (Demande d : demandes.findTop100ByOrderByCreeLeDesc()) {
            tous.add(new Travail(d.getId(), "demande", d.getCodeSuivi(), d.getStatut().name(),
                    d.getClientNom(), d.getClientTelephone(),
                    (d.isUrgente() ? "URGENT · " : "") + texte(d.getDescription()), d.getVocal(),
                    d.getArtisan() == null ? null : d.getArtisan().getNom(), d.getCreeLe()));
        }
        for (Course c : courses.findTop100ByOrderByCreeLeDesc()) {
            tous.add(new Travail(c.getId(), "course", c.getCodeSuivi(), c.getStatut().name(),
                    c.getClientNom(), c.getClientTelephone(),
                    c.getDepart().getQuartier() + " → " + c.getDestination().getQuartier(), null,
                    c.getLivreur() == null ? null : c.getLivreur().getNom(), c.getCreeLe()));
        }

        return tous.stream()
                .filter(t -> statut == null || statut.isBlank() || statut.equalsIgnoreCase(t.statut()))
                .sorted(Comparator.comparing(Travail::creeLe).reversed())
                .limit(100)
                .toList();
    }

    // ---------- Avis à modérer ----------

    public record AvisAdmin(Long id, int note, String commentaire, boolean masque,
                            String surQui, LocalDateTime creeLe) {
    }

    @GetMapping("/avis")
    @Transactional(readOnly = true)
    public List<AvisAdmin> avis() {
        return avis.findTop100ByOrderByCreeLeDesc().stream()
                .map(a -> new AvisAdmin(a.getId(), a.getNote(), a.getCommentaire(), a.isMasque(),
                        a.getArtisan() != null ? a.getArtisan().getNom()
                                : (a.getLivreur() != null ? a.getLivreur().getNom() : "—"),
                        a.getCreeLe()))
                .toList();
    }

    // ---------- Journal ----------

    public record LigneJournal(Long id, String adresse, String action, String cible,
                               String detail, LocalDateTime faiteLe) {
    }

    @GetMapping("/journal")
    @Transactional(readOnly = true)
    public List<LigneJournal> journal() {
        return journal.dernieres().stream()
                .map(a -> new LigneJournal(a.getId(), a.getAdresse(), a.getAction(),
                        a.getCible(), a.getDetail(), a.getFaiteLe()))
                .toList();
    }

    // ---------- Régénérer un code de gestion ----------

    public record NouveauCode(String codeGestion) {
    }

    /**
     * Donne un nouveau code à un travailleur. L'ancien cesse immédiatement de fonctionner.
     *
     * <p>Deux usages : il a perdu son code, ou il pense que quelqu'un d'autre l'a. Dans les deux
     * cas, régénérer est la bonne réponse — c'est l'équivalent du changement de mot de passe.
     */
    @PostMapping("/{type}/{id}/code")
    @Transactional
    public NouveauCode regenererCode(@PathVariable String type, @PathVariable Long id,
                                     HttpServletRequest http) {
        String nouveau;
        if ("artisan".equals(type)) {
            Artisan a = artisans.findById(id).orElseThrow(AdminBackOfficeController::introuvable);
            nouveau = CodeSuivi.generer(CodeSuivi.ARTISAN);
            a.setCodeGestion(nouveau);
            artisans.save(a);
        } else if ("livreur".equals(type)) {
            Livreur l = livreurs.findById(id).orElseThrow(AdminBackOfficeController::introuvable);
            nouveau = CodeSuivi.generer(CodeSuivi.LIVREUR);
            l.setCodeGestion(nouveau);
            livreurs.save(l);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type inconnu : " + type);
        }

        // Le code lui-même n'entre pas dans le journal : il y resterait lisible pour toujours
        journal.enregistrer(adresse(http), "REGENERER_CODE", type + ":" + id, null);
        return new NouveauCode(nouveau);
    }

    // ---------- Petits utilitaires ----------

    private static String etatDe(boolean actif, boolean refuse) {
        if (refuse) return "REFUSE";
        return actif ? "VISIBLE" : "EN_ATTENTE";
    }

    private static String texte(String brut) {
        if (brut == null || brut.isBlank()) return "message vocal";
        return brut.length() <= 90 ? brut : brut.substring(0, 90) + "…";
    }

    private static ResponseStatusException introuvable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Introuvable");
    }

    private static String adresse(HttpServletRequest requete) {
        String transmise = requete.getHeader("X-Forwarded-For");
        if (transmise != null && !transmise.isBlank()) return transmise.split(",")[0].trim();
        return requete.getRemoteAddr();
    }
}
