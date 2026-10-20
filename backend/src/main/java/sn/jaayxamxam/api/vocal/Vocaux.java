package sn.jaayxamxam.api.vocal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Enregistrement et lecture des messages vocaux laissés par les clients.
 *
 * <p>Beaucoup de gens parlent mieux qu'ils n'écrivent. Un champ « décrivez le travail » écarte
 * précisément les clients que cette plateforme doit servir ; un bouton d'enregistrement les fait
 * revenir.
 *
 * <h2>Ce que cette classe fait, et ce qu'elle ne fait pas</h2>
 *
 * <p>Elle <strong>vérifie et contraint</strong> : octets d'en-tête, taille, nom tiré au hasard.
 * Elle <strong>n'assainit pas</strong>, contrairement à {@code Photos} qui ré-encode chaque image
 * et produit donc un fichier fabriqué ici, nettoyé de tout ce qui s'y cachait. Ré-encoder de
 * l'audio demanderait ffmpeg ; on accepte donc le fichier du navigateur tel quel, et on le sert
 * avec un type MIME fixe et {@code X-Content-Type-Options: nosniff} pour qu'aucun navigateur ne
 * le prenne pour autre chose. C'est écrit dans le README, section « non traité ».
 *
 * <p>Pas de dépendance Spring ici, volontairement : la classe se teste seule, ce qui compte pour
 * du code qui reçoit des fichiers venus de l'extérieur.
 */
public class Vocaux {

    /**
     * Une minute d'Opus à 24 kbit/s pèse environ 180 ko. On laisse de la marge pour les
     * navigateurs plus bavards, mais pas au point d'accepter un film.
     */
    public static final long TAILLE_MAX = 1_500_000L;

    /** En dessous, ce n'est pas un enregistrement mais un clic malheureux. */
    private static final long TAILLE_MIN = 1_000L;

    /** 32 caractères hexadécimaux : 128 bits, indevinables. */
    private static final Pattern NOM_VALIDE = Pattern.compile("[0-9a-f]{32}\\.(webm|ogg|m4a)");

    // Les trois contenants que produisent réellement les navigateurs
    private static final byte[] DEBUT_WEBM = { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3 };  // Matroska
    private static final byte[] DEBUT_OGG = { 'O', 'g', 'g', 'S' };
    private static final byte[] FTYP = { 'f', 't', 'y', 'p' };                           // MP4, à l'octet 4

    private static final SecureRandom ALEA = new SecureRandom();

    private final Path dossier;

    public Vocaux(Path dossier) throws IOException {
        this.dossier = dossier.toAbsolutePath().normalize();
        Files.createDirectories(this.dossier);
    }

    /**
     * Reconnaît le contenant d'après ses premiers octets, et renvoie l'extension correspondante.
     *
     * <p>On ne regarde ni le nom donné par le navigateur ni le type qu'il annonce : les deux
     * viennent du client et se falsifient. Seuls les octets ne mentent pas.
     *
     * <p>Renvoie {@code null} si ce n'est aucun des contenants attendus.
     */
    public static String extension(byte[] octets) {
        if (octets == null || octets.length < 12) return null;
        if (commencePar(octets, DEBUT_WEBM, 0)) return "webm";
        if (commencePar(octets, DEBUT_OGG, 0)) return "ogg";
        // MP4 : « ....ftyp », la taille de boîte occupe les quatre premiers octets
        if (commencePar(octets, FTYP, 4)) return "m4a";
        return null;
    }

    /**
     * Enregistre un vocal et renvoie son nom de fichier.
     *
     * @throws VocalRefuse si le fichier est vide, trop gros, trop petit, ou d'un format inattendu
     */
    public String enregistrer(byte[] octets) {
        if (octets == null || octets.length == 0) {
            throw new VocalRefuse("Aucun enregistrement reçu.");
        }
        if (octets.length > TAILLE_MAX) {
            throw new VocalRefuse("Message trop long. Une minute au maximum.");
        }
        if (octets.length < TAILLE_MIN) {
            throw new VocalRefuse("Enregistrement trop court. Parlez après avoir appuyé.");
        }

        String ext = extension(octets);
        if (ext == null) {
            throw new VocalRefuse("Ce fichier n'est pas un enregistrement audio.");
        }

        // Nom tiré au hasard : un vocal parle de la maison de quelqu'un, son adresse ne se devine pas
        byte[] alea = new byte[16];
        ALEA.nextBytes(alea);
        String nom = HexFormat.of().formatHex(alea) + "." + ext;

        try {
            Path destination = dossier.resolve(nom);
            // Écriture dans un fichier temporaire puis déplacement : jamais de fichier à moitié écrit
            Path temporaire = Files.createTempFile(dossier, "en-cours-", ".part");
            Files.write(temporaire, octets);
            Files.move(temporaire, destination, StandardCopyOption.ATOMIC_MOVE);
            return nom;
        } catch (IOException e) {
            throw new VocalRefuse("L'enregistrement n'a pas pu être conservé.");
        }
    }

    /**
     * Chemin d'un vocal, ou {@code null} si le nom est invalide ou le fichier absent.
     *
     * <p>Le contrôle du nom est la défense contre la traversée de répertoire : sans lui,
     * {@code ../../application.yml} serait un nom de fichier acceptable.
     */
    public Path chemin(String nom) {
        if (nom == null || !NOM_VALIDE.matcher(nom).matches()) return null;
        Path fichier = dossier.resolve(nom).normalize();
        // Ceinture et bretelles : même si le motif laissait passer quelque chose, on reste au chaud
        if (!fichier.startsWith(dossier) || !Files.isRegularFile(fichier)) return null;
        return fichier;
    }

    /** Type MIME à servir, déduit de l'extension et jamais de ce que le client a annoncé. */
    public static String typeMime(String nom) {
        if (nom == null) return "application/octet-stream";
        if (nom.endsWith(".webm")) return "audio/webm";
        if (nom.endsWith(".ogg")) return "audio/ogg";
        if (nom.endsWith(".m4a")) return "audio/mp4";
        return "application/octet-stream";
    }

    public void supprimer(String nom) {
        Path fichier = chemin(nom);
        if (fichier == null) return;
        try {
            Files.deleteIfExists(fichier);
        } catch (IOException e) {
            // Un vocal qui reste sur le disque n'est pas une panne : on n'interrompt rien pour ça
        }
    }

    private static boolean commencePar(byte[] octets, byte[] motif, int decalage) {
        if (octets.length < decalage + motif.length) return false;
        for (int i = 0; i < motif.length; i++) {
            if (octets[decalage + i] != motif[i]) return false;
        }
        return true;
    }
}
