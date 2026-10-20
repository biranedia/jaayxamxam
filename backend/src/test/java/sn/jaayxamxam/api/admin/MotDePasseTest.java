package sn.jaayxamxam.api.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MotDePasseTest {

    private static final String PHRASE = "atelier medina plombier 2026";

    @Test
    @DisplayName("accepte le bon mot de passe et refuse les autres")
    void verifie() {
        String empreinte = MotDePasse.hacher(PHRASE);
        assertThat(MotDePasse.verifier(PHRASE, empreinte)).isTrue();
        assertThat(MotDePasse.verifier(PHRASE + " ", empreinte)).isFalse();
        assertThat(MotDePasse.verifier("autre chose", empreinte)).isFalse();
        assertThat(MotDePasse.verifier("", empreinte)).isFalse();
        assertThat(MotDePasse.verifier(null, empreinte)).isFalse();
    }

    /**
     * Le sel aléatoire est ce qui empêche de reconnaître deux comptes au même mot de passe,
     * et rend inutile toute table précalculée.
     */
    @Test
    @DisplayName("produit une empreinte différente à chaque fois, grâce au sel")
    void selAleatoire() {
        String a = MotDePasse.hacher(PHRASE);
        String b = MotDePasse.hacher(PHRASE);
        assertThat(a).isNotEqualTo(b);
        assertThat(MotDePasse.verifier(PHRASE, a)).isTrue();
        assertThat(MotDePasse.verifier(PHRASE, b)).isTrue();
    }

    @Test
    @DisplayName("respecte le format attendu, itérations comprises")
    void format() {
        String[] parts = MotDePasse.hacher(PHRASE).split("\\$");
        assertThat(parts).hasSize(4);
        assertThat(parts[0]).isEqualTo("pbkdf2");
        // 600 000 : la recommandation OWASP pour PBKDF2-HMAC-SHA256
        assertThat(Integer.parseInt(parts[1])).isEqualTo(600_000);
    }

    /**
     * Une empreinte absente ou abîmée doit refuser, jamais lever : une exception qui remonte
     * apprendrait à l'attaquant que la configuration est cassée.
     */
    @Test
    @DisplayName("refuse sans lever sur une empreinte absente ou abîmée")
    void empreinteInvalide() {
        assertThat(MotDePasse.verifier(PHRASE, null)).isFalse();
        assertThat(MotDePasse.verifier(PHRASE, "")).isFalse();
        assertThat(MotDePasse.verifier(PHRASE, "   ")).isFalse();
        assertThat(MotDePasse.verifier(PHRASE, "nimporte quoi")).isFalse();
        assertThat(MotDePasse.verifier(PHRASE, "pbkdf2$600000$pas-du-base64$non-plus")).isFalse();
        assertThat(MotDePasse.verifier(PHRASE, "md5$1$aaaa$bbbb")).isFalse();
    }

    /** Une empreinte au nombre d'itérations ridicule est refusée, même si elle correspond. */
    @Test
    @DisplayName("refuse une empreinte au coût trop faible")
    void iterationsTropFaibles() {
        assertThat(MotDePasse.verifier(PHRASE, "pbkdf2$10$AAAAAAAAAAAAAAAAAAAAAA$AAAA")).isFalse();
    }
}
