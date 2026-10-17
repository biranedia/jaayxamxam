package sn.jaayxamxam.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompteurAppelsTest {

    private static final long UNE_HEURE = 3_600_000L;

    @Test
    @DisplayName("Laisse passer jusqu'à la limite, puis refuse")
    void limiteAtteinte() {
        CompteurAppels compteur = new CompteurAppels();
        long t = 1_000_000L;

        for (int i = 0; i < 3; i++) {
            assertThat(compteur.autoriser("ip", 3, UNE_HEURE, t)).isTrue();
        }
        assertThat(compteur.autoriser("ip", 3, UNE_HEURE, t)).isFalse();
    }

    @Test
    @DisplayName("La fenêtre écoulée remet le compteur à zéro")
    void fenetreGlissante() {
        CompteurAppels compteur = new CompteurAppels();
        long t = 1_000_000L;

        compteur.autoriser("ip", 1, UNE_HEURE, t);
        assertThat(compteur.autoriser("ip", 1, UNE_HEURE, t + UNE_HEURE - 1)).isFalse();
        assertThat(compteur.autoriser("ip", 1, UNE_HEURE, t + UNE_HEURE)).isTrue();
    }

    @Test
    @DisplayName("Une adresse bloquée n'empêche pas les autres de passer")
    void clesIndependantes() {
        CompteurAppels compteur = new CompteurAppels();
        long t = 1_000_000L;

        compteur.autoriser("ip-a", 1, UNE_HEURE, t);
        assertThat(compteur.autoriser("ip-a", 1, UNE_HEURE, t)).isFalse();
        assertThat(compteur.autoriser("ip-b", 1, UNE_HEURE, t)).isTrue();
    }

    @Test
    @DisplayName("Les appels refusés ne sont pas comptés deux fois")
    void refusNonCompte() {
        CompteurAppels compteur = new CompteurAppels();
        long t = 1_000_000L;

        compteur.autoriser("ip", 2, UNE_HEURE, t);
        compteur.autoriser("ip", 2, UNE_HEURE, t);
        compteur.autoriser("ip", 2, UNE_HEURE, t);   // refusé
        assertThat(compteur.compte("ip", UNE_HEURE, t)).isEqualTo(2);
    }

    @Test
    @DisplayName("Le ménage ne perd pas les compteurs encore valables")
    void menageSansEffetDeBord() {
        CompteurAppels compteur = new CompteurAppels();
        long t = 1_000_000L;

        compteur.autoriser("ip", 5000, UNE_HEURE, t);
        for (int i = 0; i < 1200; i++) {
            compteur.autoriser("ip-" + i, 5000, UNE_HEURE, t);
        }
        assertThat(compteur.compte("ip", UNE_HEURE, t)).isEqualTo(1);
    }
}
