package sn.jaayxamxam.api.admin;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hachage et vérification du mot de passe de l'administration.
 *
 * <p>PBKDF2-HMAC-SHA256, 600 000 itérations, sel aléatoire de 16 octets : ce sont les paramètres
 * recommandés par l'OWASP pour PBKDF2. Argon2id serait un meilleur choix dans l'absolu, mais il
 * demande une dépendance externe ; PBKDF2 est fourni par le JDK, donc rien à ajouter et chaque
 * ligne reste vérifiable.
 *
 * <p>Pourquoi un hachage lent plutôt qu'un jeton comparé directement : un jeton se stocke en clair
 * dans la configuration du serveur, dans l'historique du terminal et dans les variables
 * d'environnement du service d'hébergement. Le hachage, lui, ne permet pas de retrouver le mot de
 * passe, et les 600 000 itérations rendent une attaque par dictionnaire coûteuse même si le fichier
 * de configuration fuite.
 *
 * <p>Format stocké : {@code pbkdf2$<itérations>$<sel base64>$<empreinte base64>}. Les itérations
 * sont dans la chaîne pour qu'on puisse les augmenter plus tard sans invalider l'existant.
 *
 * <p>Pour générer son hachage :
 * <pre>java -cp target/classes sn.jaayxamxam.api.admin.MotDePasse "mon mot de passe"</pre>
 */
public final class MotDePasse {

    private static final String ALGORITHME = "PBKDF2WithHmacSHA256";
    private static final String PREFIXE = "pbkdf2";
    private static final int ITERATIONS = 600_000;
    private static final int OCTETS_SEL = 16;
    private static final int BITS_CLE = 256;

    private static final SecureRandom ALEA = new SecureRandom();

    private MotDePasse() {
    }

    /** Produit la chaîne à placer dans la configuration. */
    public static String hacher(String enClair) {
        byte[] sel = new byte[OCTETS_SEL];
        ALEA.nextBytes(sel);
        byte[] empreinte = deriver(enClair, sel, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return PREFIXE + "$" + ITERATIONS + "$" + b64.encodeToString(sel) + "$" + b64.encodeToString(empreinte);
    }

    /**
     * Vérifie un mot de passe contre une empreinte stockée.
     *
     * <p>Renvoie faux sur toute anomalie — empreinte absente, mal formée, algorithme inconnu —
     * plutôt que de lever une exception : une erreur qui remonte au client lui apprendrait que la
     * configuration est cassée, et une pile d'appel en dit toujours trop.
     */
    public static boolean verifier(String enClair, String stocke) {
        if (enClair == null || stocke == null || stocke.isBlank()) return false;

        String[] parts = stocke.trim().split("\\$");
        if (parts.length != 4 || !PREFIXE.equals(parts[0])) return false;

        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 1000) return false;          // une empreinte trop faible est refusée
            byte[] sel = Base64.getDecoder().decode(parts[2]);
            byte[] attendue = Base64.getDecoder().decode(parts[3]);
            byte[] calculee = deriver(enClair, sel, iterations);
            // Comparaison à temps constant : sinon la durée de réponse trahit les octets justes
            return MessageDigest.isEqual(calculee, attendue);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] deriver(String enClair, byte[] sel, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(enClair.toCharArray(), sel, iterations, BITS_CLE);
            try {
                return SecretKeyFactory.getInstance(ALGORITHME).generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();   // efface le mot de passe de la mémoire du tableau
            }
        } catch (Exception e) {
            // Un algorithme absent du JDK est une panne de configuration, pas une entrée invalide
            throw new IllegalStateException("PBKDF2 indisponible sur cette machine virtuelle", e);
        }
    }

    /** Petit utilitaire en ligne de commande pour fabriquer l'empreinte à mettre en configuration. */
    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            System.out.println("Usage : java -cp target/classes sn.jaayxamxam.api.admin.MotDePasse \"mon mot de passe\"");
            System.out.println("Choisissez une phrase longue plutôt qu'un mot court et compliqué.");
            return;
        }
        if (args[0].length() < 12) {
            System.out.println("Refusé : au moins 12 caractères. Une phrase de quatre mots fait l'affaire.");
            return;
        }
        System.out.println();
        System.out.println("Placez cette ligne dans la variable d'environnement ADMIN_HASH :");
        System.out.println();
        System.out.println(hacher(args[0]));
        System.out.println();
        System.out.println("Le mot de passe en clair n'est enregistré nulle part. Ne le perdez pas.");
    }

    /** Longueur minimale acceptée, partagée avec le contrôleur de connexion. */
    public static int longueurMinimale() {
        return 12;
    }

    static {
        // Vérifie au chargement de la classe que l'algorithme existe, plutôt qu'au premier visiteur
        deriver("verification", new byte[OCTETS_SEL], 1);
    }

    /** Encodage utilisé pour les messages, exposé pour les tests. */
    static byte[] octets(String texte) {
        return texte.getBytes(StandardCharsets.UTF_8);
    }
}
