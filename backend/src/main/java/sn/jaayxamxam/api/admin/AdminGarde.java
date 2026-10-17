package sn.jaayxamxam.api.admin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protection de l'administration par un jeton partagé.
 *
 * <p>Ce n'est pas un système de comptes : c'est un mot de passe unique, pour une seule personne,
 * en attendant l'authentification. Trois règles le rendent acceptable :
 *
 * <ul>
 *   <li><strong>Fermé par défaut.</strong> Sans la variable d'environnement {@code ADMIN_TOKEN},
 *       l'administration est totalement injoignable. Un déploiement qui oublie de la configurer
 *       n'expose rien — il n'y a pas d'« admin/admin ».
 *   <li><strong>Comparaison à temps constant.</strong> Comparer deux chaînes avec {@code equals}
 *       s'arrête au premier caractère différent : le temps de réponse révèle alors le jeton,
 *       caractère par caractère.
 *   <li><strong>Jeton jamais journalisé</strong>, et transmis dans un en-tête, pas dans l'adresse.
 * </ul>
 */
@Component
public class AdminGarde extends OncePerRequestFilter {

    private static final Logger JOURNAL = LoggerFactory.getLogger(AdminGarde.class);
    private static final String ENTETE = "X-Admin-Token";

    @Value("${app.admin.jeton:}")
    private String jeton;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requete) {
        return !requete.getRequestURI().startsWith("/api/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain suite)
            throws ServletException, IOException {

        if (jeton == null || jeton.isBlank()) {
            JOURNAL.warn("Administration appelée alors qu'aucun jeton n'est configuré : refusée");
            refuser(reponse, "Administration non configurée sur ce serveur.");
            return;
        }
        if (!identiques(requete.getHeader(ENTETE), jeton)) {
            JOURNAL.warn("Jeton d'administration invalide depuis {}", requete.getRemoteAddr());
            refuser(reponse, "Jeton d'administration invalide.");
            return;
        }
        suite.doFilter(requete, reponse);
    }

    private static boolean identiques(String fourni, String attendu) {
        if (fourni == null) return false;
        return MessageDigest.isEqual(
                fourni.getBytes(StandardCharsets.UTF_8),
                attendu.getBytes(StandardCharsets.UTF_8));
    }

    private static void refuser(HttpServletResponse reponse, String message) throws IOException {
        reponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        reponse.setContentType("application/json;charset=UTF-8");
        reponse.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
