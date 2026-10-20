package sn.jaayxamxam.api.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Course;
import sn.jaayxamxam.api.domain.Demande;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.domain.StatutCourse;
import sn.jaayxamxam.api.domain.StatutDemande;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.CourseRepository;
import sn.jaayxamxam.api.repository.DemandeRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * L'espace du travailleur : il s'identifie avec le code de gestion reçu à l'inscription.
 *
 * <p>Pas de compte, pas de mot de passe, pas de SMS. Le code à neuf caractères est le seul secret,
 * et il suffit pour ce qu'on en fait : se déclarer disponible, lire le travail reçu, l'accepter ou
 * le refuser. Aucun paiement ne transite, et un code compromis se régénère. Le jour où de l'argent
 * passera par la plateforme, il faudra vérifier le téléphone — c'est écrit dans le README.
 *
 * <p>Le code voyage dans le corps de la requête et jamais dans l'adresse : une URL se retrouve dans
 * les journaux du serveur, dans l'historique du navigateur et dans l'en-tête Referer envoyé aux
 * sites tiers. Même raison que pour l'envoi des photos.
 */
@RestController
@RequestMapping("/api/espace")
public class EspaceController {

    /** Un code de gestion : une lettre de type puis huit caractères de l'alphabet sans ambiguïté. */
    private static final Pattern FORME = Pattern.compile("[ACDL][ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}");

    /**
     * Les seuls changements qu'un travailleur peut faire lui-même.
     *
     * <p>NOUVELLE n'y est pas : on ne revient pas en arrière sur une demande déjà prise en charge,
     * sinon le client verrait son travail retourner à l'état initial sans explication.
     */
    private static final Set<String> CHANGEMENTS_PERMIS =
            Set.of("ACCEPTEE", "EN_ROUTE", "REFUSEE", "TERMINEE");

    private final ArtisanRepository artisans;
    private final LivreurRepository livreurs;
    private final DemandeRepository demandes;
    private final CourseRepository courses;

    public EspaceController(ArtisanRepository artisans, LivreurRepository livreurs,
                            DemandeRepository demandes, CourseRepository courses) {
        this.artisans = artisans;
        this.livreurs = livreurs;
        this.demandes = demandes;
        this.courses = courses;
    }

    /** POST /api/espace — ouvre l'espace : le profil et le travail reçu. */
    @PostMapping
    @Transactional(readOnly = true)
    public EspaceResponse ouvrir(@Valid @RequestBody EspaceRequetes.Connexion requete) {
        String code = exigerCode(requete.code());
        Optional<Artisan> artisan = artisans.findByCodeGestion(code);
        if (artisan.isPresent()) return depuisArtisan(artisan.get());
        return depuisLivreur(livreurs.findByCodeGestion(code).orElseThrow(EspaceController::inconnu));
    }

    /**
     * POST /api/espace/position — « je travaille aujourd'hui ».
     *
     * <p>C'est cet appel, et lui seul, qui fait apparaître quelqu'un comme disponible :
     * {@code derniereActivite} est l'horodatage que la recherche compare pour décider de la
     * présence. La position est enregistrée telle quelle ; l'arrondi n'a lieu qu'à l'affichage
     * public, jamais ici — le travailleur a le droit de savoir où le système le situe.
     */
    @PostMapping("/position")
    @Transactional
    public EspaceResponse position(@Valid @RequestBody EspaceRequetes.Position requete) {
        String code = exigerCode(requete.code());
        LocalDateTime maintenant = LocalDateTime.now();

        Optional<Artisan> trouve = artisans.findByCodeGestion(code);
        if (trouve.isPresent()) {
            Artisan a = exigerValide(trouve.get(), trouve.get().isActif(), trouve.get().isRefuse());
            a.setLatitude(requete.latitude());
            a.setLongitude(requete.longitude());
            a.setDerniereActivite(maintenant);
            return depuisArtisan(artisans.save(a));
        }

        Livreur brut = livreurs.findByCodeGestion(code).orElseThrow(EspaceController::inconnu);
        Livreur l = exigerValide(brut, brut.isActif(), brut.isRefuse());
        l.setLatitude(requete.latitude());
        l.setLongitude(requete.longitude());
        l.setDerniereActivite(maintenant);
        return depuisLivreur(livreurs.save(l));
    }

    /**
     * POST /api/espace/hors-ligne — le travailleur ne veut plus être appelé.
     *
     * <p>On efface l'horodatage plutôt que de le reculer : aucune date « assez vieille » ne reste
     * vraie si l'on change un jour le seuil de présence. La position, elle, est conservée : la
     * distance affichée garde ainsi un sens, et le travailleur n'a pas à la redonner demain.
     */
    @PostMapping("/hors-ligne")
    @Transactional
    public EspaceResponse horsLigne(@Valid @RequestBody EspaceRequetes.Connexion requete) {
        String code = exigerCode(requete.code());

        Optional<Artisan> trouve = artisans.findByCodeGestion(code);
        if (trouve.isPresent()) {
            Artisan a = trouve.get();
            a.setDerniereActivite(null);
            return depuisArtisan(artisans.save(a));
        }

        Livreur l = livreurs.findByCodeGestion(code).orElseThrow(EspaceController::inconnu);
        l.setDerniereActivite(null);
        return depuisLivreur(livreurs.save(l));
    }

    /**
     * POST /api/espace/travaux/{id}/statut — accepter, refuser ou terminer un travail.
     *
     * <p>Le point sensible de tout ce contrôleur : il faut vérifier que le travail appartient bien
     * à celui qui présente le code. Sans cette vérification, connaître un code suffirait pour
     * accepter ou refuser le travail de n'importe qui d'autre. Un travail qui n'appartient pas au
     * demandeur renvoie 404 et non 403 : répondre « interdit » confirmerait qu'il existe.
     */
    @PostMapping("/travaux/{id}/statut")
    @Transactional
    public EspaceResponse changerStatut(@PathVariable Long id,
                                        @Valid @RequestBody EspaceRequetes.ChangementStatut requete) {
        String code = exigerCode(requete.code());
        String vise = requete.statut() == null ? "" : requete.statut().trim().toUpperCase();

        Optional<Artisan> artisan = artisans.findByCodeGestion(code);
        if (artisan.isPresent()) {
            Artisan a = artisan.get();
            Demande d = demandes.findById(id).filter(x -> appartient(x.getArtisan(), a.getId()))
                    .orElseThrow(EspaceController::travailInconnu);
            d.setStatut(exigerStatut(vise, StatutDemande.class));
            demandes.save(d);
            return depuisArtisan(a);
        }

        Livreur l = livreurs.findByCodeGestion(code).orElseThrow(EspaceController::inconnu);
        Course c = courses.findById(id).filter(x -> appartient(x.getLivreur(), l.getId()))
                .orElseThrow(EspaceController::travailInconnu);
        c.setStatut(exigerStatut(vise, StatutCourse.class));
        courses.save(c);
        return depuisLivreur(l);
    }

    // ---------- Vérifications ----------

    /**
     * Nettoie puis contrôle la forme du code. Un code collé depuis un message arrive souvent avec
     * des espaces ou des tirets, et parfois en minuscules : on les retire plutôt que de renvoyer
     * une erreur incompréhensible.
     *
     * <p>Un code mal formé est traité comme un code inconnu, et non comme une requête invalide :
     * une réponse différente apprendrait à qui essaie des codes au hasard à quoi ressemble un code
     * valide, et lui épargnerait l'essentiel du travail.
     */
    private static String exigerCode(String brut) {
        String propre = brut == null ? "" : brut.replaceAll("[\\s-]", "").toUpperCase();
        if (!FORME.matcher(propre).matches()) throw inconnu();
        return propre;
    }

    /** Se déclarer disponible suppose un profil validé par l'administration. */
    private static <T> T exigerValide(T profil, boolean actif, boolean refuse) {
        if (refuse) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Votre profil a été refusé. Contactez-nous pour en connaître la raison.");
        }
        if (!actif) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Votre profil attend encore sa validation. Vous serez visible dès qu'elle sera faite.");
        }
        return profil;
    }

    private static boolean appartient(Object proprietaire, Long attendu) {
        if (proprietaire instanceof Artisan a) return attendu.equals(a.getId());
        if (proprietaire instanceof Livreur l) return attendu.equals(l.getId());
        return false;
    }

    private static <E extends Enum<E>> E exigerStatut(String vise, Class<E> type) {
        if (!CHANGEMENTS_PERMIS.contains(vise)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "État de travail inconnu : " + vise);
        }
        return EnumSet.allOf(type).stream()
                .filter(e -> e.name().equals(vise))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "État de travail inconnu : " + vise));
    }

    private static ResponseStatusException inconnu() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Code inconnu. Vérifiez celui reçu à l'inscription.");
    }

    private static ResponseStatusException travailInconnu() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Travail introuvable");
    }

    // ---------- Construction des réponses ----------

    private EspaceResponse depuisArtisan(Artisan a) {
        List<EspaceResponse.TravailDto> travaux =
                demandes.findByArtisanIdOrderByCreeLeDesc(a.getId()).stream()
                        .map(EspaceController::depuisDemande)
                        .toList();
        return new EspaceResponse(
                "artisan", a.getId(), a.getNom(), a.getTelephone(), a.getPhoto(),
                a.isVerifie(), a.isActif(), a.isRefuse(), etat(a.isActif(), a.isRefuse()),
                a.getMetier() == null ? null : a.getMetier().getNom(),
                a.getMetier() == null ? null : a.getMetier().getIcone(),
                a.getMetier() == null ? null : a.getMetier().getFamille(),
                a.getZone() == null ? null : a.getZone().getQuartier(),
                a.getZone() == null ? null : a.getZone().getVille(),
                null, null, null, null,
                a.getLatitude(), a.getLongitude(), a.getDerniereActivite(), travaux);
    }

    private EspaceResponse depuisLivreur(Livreur l) {
        List<EspaceResponse.TravailDto> travaux =
                courses.findByLivreurIdOrderByCreeLeDesc(l.getId()).stream()
                        .map(EspaceController::depuisCourse)
                        .toList();
        return new EspaceResponse(
                "livreur", l.getId(), l.getNom(), l.getTelephone(), l.getPhoto(),
                l.isVerifie(), l.isActif(), l.isRefuse(), etat(l.isActif(), l.isRefuse()),
                null, null, null, null, null,
                l.getVehicule(), l.getChargeMaxKg(), l.getPrixParKm(), l.getPrixMinimum(),
                l.getLatitude(), l.getLongitude(), l.getDerniereActivite(), travaux);
    }

    /** Un seul mot pour l'interface, au lieu de lui faire combiner deux booléens. */
    private static String etat(boolean actif, boolean refuse) {
        if (refuse) return "REFUSE";
        return actif ? "VALIDE" : "EN_ATTENTE";
    }

    private static EspaceResponse.TravailDto depuisDemande(Demande d) {
        String lieu = d.getZone() == null ? null
                : d.getZone().getQuartier() + ", " + d.getZone().getVille();
        return new EspaceResponse.TravailDto(
                d.getId(), d.getCodeSuivi(), d.getStatut().name(),
                d.getClientNom(), d.getClientTelephone(), d.getDescription(),
                d.isUrgente(), lieu, null, null, d.getCreeLe());
    }

    private static EspaceResponse.TravailDto depuisCourse(Course c) {
        String lieu = c.getDepart() == null || c.getDestination() == null ? null
                : c.getDepart().getQuartier() + " → " + c.getDestination().getQuartier();
        return new EspaceResponse.TravailDto(
                c.getId(), c.getCodeSuivi(), c.getStatut().name(),
                c.getClientNom(), c.getClientTelephone(), c.getDescription(),
                false, lieu, c.getPrixEstime(), c.getDistanceM(), c.getCreeLe());
    }
}
