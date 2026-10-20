package sn.jaayxamxam.api.admin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

/**
 * Porte d'entrée de l'administration.
 *
 * <p>Quatre règles, chacune pour une raison précise :
 *
 * <ul>
 *   <li><strong>Fermée par défaut.</strong> Sans {@code ADMIN_HASH} configuré, tout est refusé.
 *       Un déploiement qui oublie la configuration n'expose rien — il n'y a pas d'« admin/admin ».
 *   <li><strong>Session obligatoire.</strong> Le cookie porte un jeton tiré au hasard, validé
 *       côté serveur à chaque appel. Pas de jeton permanent, pas de secret partagé.
 *   <li><strong>Cookie {@code HttpOnly}.</strong> Le JavaScript de la page ne peut pas le lire :
 *       une faille d'injection ailleurs dans l'application ne permet pas de voler la session.
 *   <li><strong>Protection CSRF.</strong> Un cookie part tout seul avec chaque requête, y compris
 *       celles déclenchées par un site malveillant. On exige donc un en-tête que seul un appel
 *       JavaScript de même origine peut poser — un formulaire HTML croisé en est incapable.
 *       {@code SameSite=Strict} couvre déjà l'essentiel ; cet en-tête est la deuxième serrure.
 * </ul>
 */
@Component
public class AdminGarde extends OncePerRequestFilter {

    private static final Logger JOURNAL = LoggerFactory.getLogger(AdminGarde.class);

    public static final String COOKIE = "admin_session";
    /** En-tête exigé sur les appels qui modifient quelque chose. */
    public static final String ENTETE_CSRF = "X-Admin-Requete";

    /** Seuls chemins atteignables sans session : se connecter, et savoir si l'on est connecté. */
    private static final String CONNEXION = "/api/admin/connexion";
    private static final String MOI = "/api/admin/moi";

    private final Sessions sessions;

    @Value("${app.admin.hash:}")
    private String hash;

    public AdminGarde(Sessions sessions) {
        this.sessions = sessions;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requete) {
        return !requete.getRequestURI().startsWith("/api/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain suite)
            throws ServletException, IOException {

        String chemin = requete.getRequestURI();

        // Aucune empreinte configurée : l'administration n'existe pas sur ce serveur
        if (hash == null || hash.isBlank()) {
            JOURNAL.warn("Administration appelée alors qu'aucun mot de passe n'est configuré : refusée");
            refuser(reponse, HttpStatus.FORBIDDEN, "Administration non configurée sur ce serveur.");
            return;
        }

        // La connexion et la vérification d'état sont forcément accessibles sans session
        if (CONNEXION.equals(chemin) || MOI.equals(chemin)) {
            suite.doFilter(requete, reponse);
            return;
        }

        Optional<String> session = sessions.valider(jetonDuCookie(requete), Instant.now());
        if (session.isEmpty()) {
            refuser(reponse, HttpStatus.UNAUTHORIZED, "Session expirée. Reconnectez-vous.");
            return;
        }

        // Un GET ne change rien : on n'exige l'en-tête que sur ce qui modifie
        if (!"GET".equals(requete.getMethod()) && requete.getHeader(ENTETE_CSRF) == null) {
            JOURNAL.warn("Appel d'administration sans en-tête {} depuis {} : refusé",
                    ENTETE_CSRF, requete.getRemoteAddr());
            refuser(reponse, HttpStatus.FORBIDDEN, "Requête refusée.");
            return;
        }

        suite.doFilter(requete, reponse);
    }

    static String jetonDuCookie(HttpServletRequest requete) {
        Cookie[] cookies = requete.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private static void refuser(HttpServletResponse reponse, HttpStatus statut, String message) throws IOException {
        reponse.setStatus(statut.value());
        reponse.setContentType("application/json;charset=UTF-8");
        reponse.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
