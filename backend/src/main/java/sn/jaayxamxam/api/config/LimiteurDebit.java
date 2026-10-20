package sn.jaayxamxam.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sn.jaayxamxam.api.web.CompteurAppels;

import java.io.IOException;
import java.time.Duration;

/**
 * Freine deux abus qui n'ont pas besoin de compte pour être tentés :
 * l'envoi en masse de fausses demandes, et l'essai de codes de suivi au hasard.
 */
@Component
public class LimiteurDebit extends OncePerRequestFilter {

    private static final long FENETRE_MS = Duration.ofHours(1).toMillis();

    private final CompteurAppels compteur = new CompteurAppels();

    // Une personne ne crée pas dix demandes par heure ; un robot, si
    @Value("${app.limites.creations-par-heure:10}")
    private int creationsParHeure;

    // Un code de suivi fait 9 caractères : essayer au hasard doit rester inutile
    @Value("${app.limites.suivis-par-heure:30}")
    private int suivisParHeure;

    // Un client appelle quelques artisans ; un aspirateur d'annuaire en appelle des milliers
    @Value("${app.limites.contacts-par-heure:60}")
    private int contactsParHeure;

    // Le jeton est long et tiré au hasard ; cette limite n'est qu'une précaution de plus
    @Value("${app.limites.admin-par-heure:200}")
    private int adminParHeure;

    // Un travailleur envoie sa position régulièrement pendant sa journée : la limite doit être
    // large. Elle sert surtout contre l'essai de codes de gestion au hasard, que les neuf
    // caractères rendent déjà sans espoir.
    @Value("${app.limites.espace-par-heure:120}")
    private int espaceParHeure;

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain suite)
            throws ServletException, IOException {

        Integer maximum = limitePour(requete);
        if (maximum != null) {
            String cle = categorie(requete.getRequestURI()) + ":" + adresse(requete);

            if (!compteur.autoriser(cle, maximum, FENETRE_MS, System.currentTimeMillis())) {
                // jakarta.servlet n'a pas de constante pour 429 : on prend celle de Spring
                reponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                reponse.setContentType("application/json;charset=UTF-8");
                reponse.getWriter().write(
                        "{\"message\":\"Trop de tentatives. Réessayez dans une heure.\"}");
                return;
            }
        }
        suite.doFilter(requete, reponse);
    }

    // Un compteur par catégorie : épuiser ses essais de code ne doit pas empêcher d'appeler
    private static String categorie(String chemin) {
        if (chemin.startsWith("/api/suivi")) return "suivi";
        if (chemin.startsWith("/api/espace")) return "espace";
        if (chemin.startsWith("/api/contacts")) return "contact";
        if (chemin.startsWith("/api/admin")) return "admin";
        return "creation";
    }

    // null quand l'appel n'est pas limité (lectures de la recherche, référentiels, santé)
    private Integer limitePour(HttpServletRequest requete) {
        String chemin = requete.getRequestURI();
        if ("POST".equals(requete.getMethod())
                && (chemin.startsWith("/api/demandes")
                || chemin.startsWith("/api/courses")
                || chemin.startsWith("/api/avis")
                || chemin.startsWith("/api/inscriptions")
                || chemin.startsWith("/api/photos")
                || chemin.startsWith("/api/vocaux"))) {
            return creationsParHeure;
        }
        if ("GET".equals(requete.getMethod()) && chemin.startsWith("/api/suivi")) {
            return suivisParHeure;
        }
        if ("GET".equals(requete.getMethod()) && chemin.startsWith("/api/contacts")) {
            return contactsParHeure;
        }
        if (chemin.startsWith("/api/admin")) {
            return adminParHeure;
        }
        // Sans cette règle, l'espace travailleur serait le seul point d'entrée sans limite,
        // donc le seul endroit où essayer des codes au hasard ne coûte rien.
        if (chemin.startsWith("/api/espace")) {
            return espaceParHeure;
        }
        return null;
    }

    /**
     * Adresse du visiteur. Derrière un proxy (Nginx, Cloudflare), la vraie adresse est dans
     * X-Forwarded-For — mais cet en-tête est falsifiable si le serveur est joignable en direct.
     * À n'exposer qu'à travers un proxy de confiance, qui réécrit lui-même cet en-tête.
     */
    private static String adresse(HttpServletRequest requete) {
        String transmise = requete.getHeader("X-Forwarded-For");
        if (transmise != null && !transmise.isBlank()) {
            return transmise.split(",")[0].trim();
        }
        return requete.getRemoteAddr();
    }
}
