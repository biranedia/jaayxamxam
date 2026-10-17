package sn.jaayxamxam.api.domain;

import java.security.SecureRandom;

/**
 * Codes remis aux clients et aux travailleurs : 9 caractères, faciles à recopier au téléphone.
 *
 * <p>L'alphabet exclut I, L, O, 0 et 1, qui se confondent à la lecture.
 *
 * <p>La première lettre dit de quoi il s'agit (D comme demande, C comme course, A comme artisan,
 * L comme livreur). Sans elle, deux codes identiques pourraient exister dans deux tables
 * différentes — chaque table ayant sa propre contrainte d'unicité — et un code ne désignerait plus
 * une seule chose. La lettre règle le problème par construction.
 */
public final class CodeSuivi {

    public static final char DEMANDE = 'D';
    public static final char COURSE = 'C';
    public static final char ARTISAN = 'A';
    public static final char LIVREUR = 'L';

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CARACTERES_ALEATOIRES = 8;
    private static final SecureRandom ALEA = new SecureRandom();

    private CodeSuivi() {
    }

    public static String generer(char type) {
        StringBuilder code = new StringBuilder(CARACTERES_ALEATOIRES + 1).append(type);
        for (int i = 0; i < CARACTERES_ALEATOIRES; i++) {
            code.append(ALPHABET.charAt(ALEA.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
