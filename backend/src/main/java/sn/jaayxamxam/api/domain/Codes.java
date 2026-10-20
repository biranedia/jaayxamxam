package sn.jaayxamxam.api.domain;

import java.util.function.Predicate;

/**
 * Tire un code en s'assurant qu'il n'est pas déjà pris.
 *
 * <p>Pourquoi cette classe existe. Un code fait huit caractères tirés dans un alphabet de 31,
 * soit 852 891 037 441 combinaisons. C'est beaucoup, mais le paradoxe des anniversaires rattrape
 * vite l'intuition : sur 100 000 codes, la probabilité qu'au moins deux soient identiques est de
 * <strong>0,58 %</strong>. Pas une sur un milliard — une sur cent soixante-dix.
 *
 * <p>Sans ce contrôle, cette collision ferait échouer l'insertion sur la contrainte d'unicité, et
 * le client recevrait une erreur 500 sans explication, une fois sur deux cents à ce volume. Une
 * seule lecture indexée avant l'écriture supprime le problème.
 *
 * <p>Reste une course possible : deux requêtes simultanées pourraient tirer le même code et le
 * croire libre toutes les deux. La contrainte d'unicité de la base les rattrape, et la
 * probabilité que cela arrive est le produit de deux événements déjà très rares.
 */
public final class Codes {

    /** Au-delà, ce n'est plus une collision mais une panne du générateur aléatoire. */
    private static final int ESSAIS = 5;

    private Codes() {
    }

    /**
     * @param type        la lettre de type : {@code CodeSuivi.DEMANDE}, {@code ARTISAN}…
     * @param dejaPris    comment demander à la base si un code existe déjà
     * @return un code libre
     */
    public static String libre(char type, Predicate<String> dejaPris) {
        for (int essai = 0; essai < ESSAIS; essai++) {
            String code = CodeSuivi.generer(type);
            if (!dejaPris.test(code)) return code;
        }
        // Cinq collisions d'affilée sur 8,5 × 10^11 possibilités : ce n'est plus du hasard
        throw new IllegalStateException(
                "Impossible de tirer un code libre après " + ESSAIS + " essais");
    }
}
