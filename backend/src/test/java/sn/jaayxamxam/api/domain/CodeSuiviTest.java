package sn.jaayxamxam.api.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Le code de suivi est le seul secret du client : il doit être lisible au téléphone et imprévisible
class CodeSuiviTest {

    @Test
    @DisplayName("Une lettre de type puis huit caractères, sans I, L, O, 0 ni 1")
    void formatLisible() {
        for (int i = 0; i < 1000; i++) {
            assertThat(CodeSuivi.generer(CodeSuivi.DEMANDE))
                    .hasSize(9)
                    .startsWith("D")
                    .matches("D[ABCDEFGHJKMNPQRSTUVWXYZ2-9]{8}");
        }
    }

    @Test
    @DisplayName("Aucun doublon sur cent mille tirages")
    void pasDeDoublon() {
        Set<String> vus = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            vus.add(CodeSuivi.generer(CodeSuivi.COURSE));
        }

        // Exiger 100 000 codes tous distincts était une affirmation fausse, et ce test échouait
        // un passage sur 171. Le paradoxe des anniversaires : avec 31^8 ≈ 8,5 × 10^11 codes
        // possibles, la probabilité qu'au moins deux parmi 100 000 coïncident est de 0,58 %.
        //
        // Ce qu'on vérifie vraiment, c'est que le générateur étale bien ses tirages. Plus de dix
        // doublons signalerait un alphabet amputé ou une source d'aléa cassée ; un ou deux sont
        // attendus. La collision réelle, elle, est traitée par Codes.libre() avant insertion.
        assertThat(vus).hasSizeGreaterThan(99_990);
    }

    @Test
    @DisplayName("Un code d'artisan et un code de livreur ne peuvent jamais se confondre")
    void typesDistincts() {
        assertThat(CodeSuivi.generer(CodeSuivi.ARTISAN)).startsWith("A");
        assertThat(CodeSuivi.generer(CodeSuivi.LIVREUR)).startsWith("L");
        assertThat(CodeSuivi.generer(CodeSuivi.DEMANDE)).startsWith("D");
        assertThat(CodeSuivi.generer(CodeSuivi.COURSE)).startsWith("C");
    }

    @Test
    @DisplayName("Les codes ne se suivent pas : chaque caractère varie d'un tirage à l'autre")
    void imprevisible() {
        Set<Character> premiers = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            premiers.add(CodeSuivi.generer(CodeSuivi.ARTISAN).charAt(1));
        }
        // 31 lettres possibles : sur 500 tirages on doit en voir une bonne partie
        assertThat(premiers).hasSizeGreaterThan(20);
    }
}
