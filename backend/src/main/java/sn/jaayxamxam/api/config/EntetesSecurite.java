package sn.jaayxamxam.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * En-têtes de sécurité posés sur toutes les réponses.
 *
 * <p>L'API ne sert que du JSON et des images. Ces en-têtes disent au navigateur de ne rien faire
 * d'autre : ne pas deviner le type d'un fichier, ne pas afficher la réponse dans un cadre, ne pas
 * exécuter de script. Ils coûtent trois lignes et ferment des classes entières d'attaques.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EntetesSecurite extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain suite)
            throws ServletException, IOException {

        // Ne pas deviner le type : une image mal nommée ne sera jamais exécutée comme du script
        reponse.setHeader("X-Content-Type-Options", "nosniff");

        // Rien de cette API ne doit s'afficher dans un cadre chez quelqu'un d'autre
        reponse.setHeader("X-Frame-Options", "DENY");

        // Une réponse d'API n'a aucune raison de charger quoi que ce soit.
        // Réservé à /api : si un jour ce serveur sert aussi les pages Angular,
        // cette règle les empêcherait de fonctionner.
        if (requete.getRequestURI().startsWith("/api")) {
            reponse.setHeader("Content-Security-Policy",
                    "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'");
        }

        // Ne pas transmettre l'adresse complète de la page aux sites tiers
        reponse.setHeader("Referrer-Policy", "no-referrer");

        // Aucun usage du micro, de la caméra ou de la position depuis cette origine
        reponse.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");

        suite.doFilter(requete, reponse);
    }
}
