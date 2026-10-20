package sn.jaayxamxam.api.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SessionsTest {

    private static final Instant T0 = Instant.parse("2026-10-20T08:00:00Z");

    @Test
    @DisplayName("ouvre une session valable et renvoie l'adresse d'origine")
    void ouvreEtValide() {
        Sessions s = new Sessions();
        String jeton = s.ouvrir("41.82.0.1", T0);

        assertThat(jeton).isNotBlank();
        assertThat(s.valider(jeton, T0.plusSeconds(60))).contains("41.82.0.1");
        assertThat(s.ouvertes()).isEqualTo(1);
    }

    @Test
    @DisplayName("refuse un jeton inconnu, vide ou nul")
    void jetonInconnu() {
        Sessions s = new Sessions();
        s.ouvrir("41.82.0.1", T0);

        assertThat(s.valider("inconnu", T0)).isEmpty();
        assertThat(s.valider("", T0)).isEmpty();
        assertThat(s.valider(null, T0)).isEmpty();
    }

    @Test
    @DisplayName("ferme la session après 45 minutes sans activité")
    void expireParInactivite() {
        Sessions s = new Sessions();
        String jeton = s.ouvrir("41.82.0.1", T0);

        assertThat(s.valider(jeton, T0.plusSeconds(44 * 60))).isPresent();
        // Chaque validation repousse l'échéance : 44 min après la précédente, pas après T0
        assertThat(s.valider(jeton, T0.plusSeconds(44 * 60 + 46 * 60))).isEmpty();
    }

    @Test
    @DisplayName("ferme la session au bout de 8 heures, même si elle sert en continu")
    void expireAuMaximum() {
        Sessions s = new Sessions();
        String jeton = s.ouvrir("41.82.0.1", T0);

        // On s'en sert toutes les 30 minutes : l'inactivité ne joue jamais
        for (int minutes = 30; minutes <= 7 * 60 + 30; minutes += 30) {
            assertThat(s.valider(jeton, T0.plusSeconds(minutes * 60L)))
                    .as("à %d minutes", minutes).isPresent();
        }
        // La durée absolue, elle, finit par tomber
        assertThat(s.valider(jeton, T0.plusSeconds((8 * 60 + 1) * 60L))).isEmpty();
    }

    /** La déconnexion doit être réelle : c'est tout l'intérêt d'une session côté serveur. */
    @Test
    @DisplayName("la fermeture rend le jeton inutilisable immédiatement")
    void fermeture() {
        Sessions s = new Sessions();
        String jeton = s.ouvrir("41.82.0.1", T0);

        s.fermer(jeton);
        assertThat(s.valider(jeton, T0.plusSeconds(1))).isEmpty();
        assertThat(s.ouvertes()).isZero();
    }

    @Test
    @DisplayName("fermer toutes les sessions les ferme vraiment toutes")
    void fermerToutes() {
        Sessions s = new Sessions();
        String a = s.ouvrir("41.82.0.1", T0);
        String b = s.ouvrir("41.82.0.2", T0);

        s.fermerToutes();
        assertThat(s.valider(a, T0)).isEmpty();
        assertThat(s.valider(b, T0)).isEmpty();
    }

    /** Deux ouvertures ne doivent jamais produire le même jeton. */
    @Test
    @DisplayName("tire un jeton différent à chaque ouverture")
    void jetonsDistincts() {
        Sessions s = new Sessions();
        assertThat(s.ouvrir("41.82.0.1", T0)).isNotEqualTo(s.ouvrir("41.82.0.1", T0));
    }

    /**
     * Une session expirée doit disparaître de la mémoire, pas seulement être refusée :
     * sinon la carte grossit indéfiniment.
     */
    @Test
    @DisplayName("oublie une session expirée au lieu de la garder en mémoire")
    void menage() {
        Sessions s = new Sessions();
        String jeton = s.ouvrir("41.82.0.1", T0);

        assertThat(s.valider(jeton, T0.plusSeconds(60 * 60))).isEmpty();
        assertThat(s.ouvertes()).isZero();
    }
}
