package sn.jaayxamxam.api.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ce qui se passe quand un code tiré au hasard tombe sur un code déjà pris.
 *
 * <p>Sans ce garde-fou, l'insertion échouait sur la contrainte d'unicité et le client recevait
 * une erreur 500 : rare, mais incompréhensible le jour où ça arrive.
 */
class CodesTest {

    @Test
    @DisplayName("renvoie directement un code quand il est libre")
    void codeLibre() {
        String code = Codes.libre(CodeSuivi.DEMANDE, c -> false);
        assertThat(code).startsWith("D").hasSize(9);
    }

    @Test
    @DisplayName("retire un nouveau code quand le premier est déjà pris")
    void reTireSiPris() {
        AtomicInteger appels = new AtomicInteger();
        // Le premier code proposé est déclaré pris, le suivant est accepté
        String code = Codes.libre(CodeSuivi.COURSE, c -> appels.incrementAndGet() == 1);

        assertThat(appels.get()).isEqualTo(2);
        assertThat(code).startsWith("C");
    }

    @Test
    @DisplayName("abandonne plutôt que de boucler si tout semble pris")
    void abandonneApresCinqEssais() {
        AtomicInteger appels = new AtomicInteger();
        assertThatThrownBy(() -> Codes.libre(CodeSuivi.ARTISAN, c -> {
            appels.incrementAndGet();
            return true;                       // tout est pris : situation impossible en vrai
        })).isInstanceOf(IllegalStateException.class);

        // Cinq essais, pas une boucle infinie qui bloquerait la requête
        assertThat(appels.get()).isEqualTo(5);
    }

    @Test
    @DisplayName("ne propose jamais deux fois le même code au cours d'une recherche")
    void codesDistinctsEntreEssais() {
        Set<String> proposes = new java.util.HashSet<>();
        AtomicInteger appels = new AtomicInteger();
        Codes.libre(CodeSuivi.LIVREUR, c -> {
            proposes.add(c);
            return appels.incrementAndGet() < 3;   // les deux premiers sont pris
        });
        assertThat(proposes).hasSize(3);
    }
}
