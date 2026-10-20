package sn.jaayxamxam.api.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.web.CompteurAppels;

import java.time.Duration;
import java.time.Instant;

/**
 * Connexion et déconnexion de l'administration.
 *
 * <p>Le mot de passe n'est jamais journalisé, ni renvoyé, ni comparé autrement qu'à travers
 * {@link MotDePasse}. Le jeton de session part dans un cookie posé par le serveur : le navigateur
 * le renverra tout seul, et le JavaScript de la page ne peut pas le lire.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private static final Logger JOURNAL = LoggerFactory.getLogger(AdminAuthController.class);

    /**
     * Essais de mot de passe tolérés par heure et par adresse.
     *
     * <p>Cinq, contre 200 pour le reste de l'administration. Un humain qui se trompe recommence
     * deux ou trois fois ; au-delà, c'est une machine. Avec 600 000 itérations par essai, chaque
     * tentative coûte déjà cher au serveur — la limite protège aussi le serveur lui-même.
     */
    private static final long FENETRE_MS = Duration.ofHours(1).toMillis();

    @Value("${app.limites.connexions-admin-par-heure:5}")
    private int essaisParHeure;

    private final CompteurAppels essais = new CompteurAppels();
    private final Sessions sessions;
    private final JournalAdmin journal;

    @Value("${app.admin.hash:}")
    private String hash;

    /**
     * Cookie marqué {@code Secure} par défaut : il ne partira qu'en HTTPS. En développement local,
     * http://localhost n'est pas HTTPS, d'où le réglage — à laisser à {@code true} en production.
     */
    @Value("${app.admin.cookie-securise:true}")
    private boolean cookieSecurise;

    public AdminAuthController(Sessions sessions, JournalAdmin journal) {
        this.sessions = sessions;
        this.journal = journal;
    }

    public record Connexion(
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(max = 200, message = "Mot de passe trop long")
            String motDePasse) {
    }

    /** Ce que l'interface a besoin de savoir, sans rien de sensible. */
    public record Etat(boolean connecte, int minutesInactivite, int sessionsOuvertes) {
    }

    @PostMapping("/connexion")
    public Etat connexion(@Valid @RequestBody Connexion requete,
                          HttpServletRequest http, HttpServletResponse reponse) {

        String adresse = adresse(http);

        if (!essais.autoriser("admin-connexion:" + adresse, essaisParHeure, FENETRE_MS,
                System.currentTimeMillis())) {
            JOURNAL.warn("Trop de tentatives de connexion à l'administration depuis {}", adresse);
            journal.enregistrer(adresse, "CONNEXION_BLOQUEE", null, "trop de tentatives");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives. Réessayez dans une heure.");
        }

        if (!MotDePasse.verifier(requete.motDePasse(), hash)) {
            // Même message et même délai qu'il s'agisse d'une erreur de frappe ou d'une attaque
            JOURNAL.warn("Mot de passe d'administration refusé depuis {}", adresse);
            journal.enregistrer(adresse, "CONNEXION_REFUSEE", null, null);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect.");
        }

        String jeton = sessions.ouvrir(adresse, Instant.now());
        reponse.addHeader("Set-Cookie", cookie(jeton, Sessions.inactiviteMaximale()).toString());
        JOURNAL.info("Connexion à l'administration depuis {}", adresse);
        journal.enregistrer(adresse, "CONNEXION", null, null);
        return etat(true);
    }

    @PostMapping("/deconnexion")
    public Etat deconnexion(HttpServletRequest http, HttpServletResponse reponse) {
        String jeton = AdminGarde.jetonDuCookie(http);
        sessions.fermer(jeton);
        // Cookie vidé et expiré immédiatement : le navigateur l'efface
        reponse.addHeader("Set-Cookie", cookie("", Duration.ZERO).toString());
        journal.enregistrer(adresse(http), "DECONNEXION", null, null);
        return etat(false);
    }

    /** Permet à l'interface de savoir s'il faut afficher la connexion ou le back-office. */
    @GetMapping("/moi")
    public Etat moi(HttpServletRequest http) {
        boolean valide = sessions.valider(AdminGarde.jetonDuCookie(http), Instant.now()).isPresent();
        return etat(valide);
    }

    /**
     * Ferme toutes les sessions, y compris la sienne.
     *
     * <p>À utiliser si l'on soupçonne qu'un navigateur est resté ouvert ailleurs.
     */
    @PostMapping("/fermer-tout")
    public Etat fermerTout(HttpServletRequest http, HttpServletResponse reponse) {
        sessions.fermerToutes();
        reponse.addHeader("Set-Cookie", cookie("", Duration.ZERO).toString());
        journal.enregistrer(adresse(http), "FERMETURE_TOUTES_SESSIONS", null, null);
        return etat(false);
    }

    private Etat etat(boolean connecte) {
        return new Etat(connecte,
                (int) Sessions.inactiviteMaximale().toMinutes(),
                sessions.ouvertes());
    }

    private ResponseCookie cookie(String valeur, Duration duree) {
        return ResponseCookie.from(AdminGarde.COOKIE, valeur)
                .httpOnly(true)                 // invisible au JavaScript : une XSS ne le vole pas
                .secure(cookieSecurise)         // HTTPS seulement, sauf en développement local
                .sameSite("Strict")             // jamais envoyé depuis un autre site : anti-CSRF
                .path("/api/admin")             // limité à l'administration, pas au reste du site
                .maxAge(duree)
                .build();
    }

    /** Même raisonnement que dans le limiteur de débit : X-Forwarded-For derrière un proxy sûr. */
    private static String adresse(HttpServletRequest requete) {
        String transmise = requete.getHeader("X-Forwarded-For");
        if (transmise != null && !transmise.isBlank()) return transmise.split(",")[0].trim();
        return requete.getRemoteAddr();
    }
}
