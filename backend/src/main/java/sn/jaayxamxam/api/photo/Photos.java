package sn.jaayxamxam.api.photo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Enregistrement des photos de profil.
 *
 * <p>Une photo envoyée par un inconnu est une pièce jointe hostile jusqu'à preuve du contraire.
 * Quatre précautions, dans cet ordre :
 *
 * <ol>
 *   <li><strong>Les octets, pas le nom.</strong> Le type est déduit des premiers octets du fichier.
 *       Un « .jpg » qui commence par autre chose est refusé.
 *   <li><strong>Les dimensions avant le décodage.</strong> Une image de 30 000 × 30 000 pixels pèse
 *       quelques kilo-octets compressée et plusieurs gigaoctets en mémoire : c'est la « bombe de
 *       décompression ». On lit la taille d'abord, on refuse, et on ne décode jamais.
 *   <li><strong>Ré-encodage systématique.</strong> L'image est décodée puis ré-écrite en JPEG. Cela
 *       efface les métadonnées EXIF — <em>dont les coordonnées GPS du lieu de la photo</em>, souvent
 *       le domicile du travailleur — et neutralise les fichiers hybrides image/script.
 *   <li><strong>Nom tiré au hasard.</strong> Le nom d'origine n'est jamais réutilisé : ni chemin
 *       glissé dedans, ni extension choisie par l'envoyeur.
 * </ol>
 *
 * <p>Classe volontairement sans annotation Spring : elle se construit avec un simple dossier, donc
 * elle se teste seule, hors de toute application. Le branchement se fait dans {@code PhotoConfig}.
 */
public class Photos {

    private static final Logger JOURNAL = LoggerFactory.getLogger(Photos.class);

    /** Assez grand pour une fiche, assez petit pour une connexion 3G. */
    private static final int COTE_MAX = 640;

    /** Au-delà, on refuse sans décoder : 40 millions de pixels, soit bien plus qu'un téléphone. */
    private static final long PIXELS_MAX = 40_000_000L;

    private static final float QUALITE = 0.82f;

    /** Les seuls noms acceptés en lecture : 32 caractères hexadécimaux et rien d'autre. */
    private static final Pattern NOM_VALIDE = Pattern.compile("[0-9a-f]{32}\\.jpg");

    private static final byte[] DEBUT_JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] DEBUT_PNG = { (byte) 0x89, 'P', 'N', 'G' };

    private static final SecureRandom ALEA = new SecureRandom();

    private final Path dossier;

    public Photos(Path dossier) throws IOException {
        this.dossier = dossier.toAbsolutePath().normalize();
        Files.createDirectories(this.dossier);
        JOURNAL.info("Photos enregistrées dans {}", this.dossier);
    }

    /** Vrai si les premiers octets sont ceux d'un JPEG ou d'un PNG. */
    public static boolean estUneImage(byte[] octets) {
        return commencePar(octets, DEBUT_JPEG) || commencePar(octets, DEBUT_PNG);
    }

    /**
     * Enregistre la photo et renvoie son nom de fichier.
     *
     * @throws PhotoRefusee si le contenu n'est pas une image exploitable
     */
    public String enregistrer(byte[] octets) {
        if (octets == null || octets.length == 0) {
            throw new PhotoRefusee("Photo vide.");
        }
        if (!estUneImage(octets)) {
            throw new PhotoRefusee("Seules les photos JPEG et PNG sont acceptées.");
        }

        BufferedImage image = decoder(octets);
        BufferedImage reduite = reduire(image);
        String nom = HexFormat.of().formatHex(tirerAuSort()) + ".jpg";

        try (OutputStream sortie = Files.newOutputStream(dossier.resolve(nom))) {
            ecrireJpeg(reduite, sortie);
        } catch (IOException e) {
            throw new PhotoRefusee("La photo n'a pas pu être enregistrée.");
        }
        return nom;
    }

    /** Chemin du fichier, ou null si le nom n'a pas la forme attendue ou n'existe pas. */
    public Path chemin(String nom) {
        if (nom == null || !NOM_VALIDE.matcher(nom).matches()) return null;
        Path fichier = dossier.resolve(nom).normalize();
        // Ceinture et bretelles : le nom est déjà contraint, on vérifie quand même qu'on ne sort pas
        if (!fichier.startsWith(dossier) || !Files.isRegularFile(fichier)) return null;
        return fichier;
    }

    /** Efface une photo remplacée. Un échec n'est pas bloquant : la nouvelle photo est déjà en place. */
    public void supprimer(String nom) {
        Path fichier = chemin(nom);
        if (fichier == null) return;
        try {
            Files.deleteIfExists(fichier);
        } catch (IOException e) {
            JOURNAL.warn("Ancienne photo non supprimée : {}", nom, e);
        }
    }

    // ---------- interne ----------

    private static BufferedImage decoder(byte[] octets) {
        try (ImageInputStream flux = ImageIO.createImageInputStream(new ByteArrayInputStream(octets))) {
            Iterator<ImageReader> lecteurs = ImageIO.getImageReaders(flux);
            if (!lecteurs.hasNext()) {
                throw new PhotoRefusee("Ce fichier n'est pas une photo lisible.");
            }
            ImageReader lecteur = lecteurs.next();
            try {
                lecteur.setInput(flux);
                // Les dimensions se lisent dans l'en-tête, sans décoder l'image entière
                long pixels = (long) lecteur.getWidth(0) * lecteur.getHeight(0);
                if (pixels > PIXELS_MAX) {
                    throw new PhotoRefusee("Photo trop grande. Prenez-la en qualité moyenne.");
                }
                BufferedImage image = lecteur.read(0);
                if (image == null) {
                    throw new PhotoRefusee("Ce fichier n'est pas une photo lisible.");
                }
                return image;
            } finally {
                lecteur.dispose();
            }
        } catch (IOException e) {
            throw new PhotoRefusee("Ce fichier n'est pas une photo lisible.");
        }
    }

    private static BufferedImage reduire(BufferedImage source) {
        int largeur = source.getWidth();
        int hauteur = source.getHeight();
        double facteur = Math.min(1.0, (double) COTE_MAX / Math.max(largeur, hauteur));
        int nouvelleLargeur = Math.max(1, (int) Math.round(largeur * facteur));
        int nouvelleHauteur = Math.max(1, (int) Math.round(hauteur * facteur));

        // Fond blanc : le JPEG ne gère pas la transparence, un PNG transparent virerait au noir
        BufferedImage cible = new BufferedImage(nouvelleLargeur, nouvelleHauteur, BufferedImage.TYPE_INT_RGB);
        Graphics2D dessin = cible.createGraphics();
        try {
            dessin.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            dessin.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            dessin.setColor(Color.WHITE);
            dessin.fillRect(0, 0, nouvelleLargeur, nouvelleHauteur);
            dessin.drawImage(source, 0, 0, nouvelleLargeur, nouvelleHauteur, null);
        } finally {
            dessin.dispose();
        }
        return cible;
    }

    private static void ecrireJpeg(BufferedImage image, OutputStream sortie) throws IOException {
        Iterator<ImageWriter> ecrivains = ImageIO.getImageWritersByFormatName("jpeg");
        if (!ecrivains.hasNext()) {
            throw new IOException("Aucun encodeur JPEG disponible");
        }
        ImageWriter ecrivain = ecrivains.next();
        try (ImageOutputStream flux = ImageIO.createImageOutputStream(sortie)) {
            ecrivain.setOutput(flux);
            ImageWriteParam parametres = ecrivain.getDefaultWriteParam();
            if (parametres.canWriteCompressed()) {
                parametres.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parametres.setCompressionQuality(QUALITE);
            }
            // Pas de métadonnées : c'est ce qui efface l'EXIF, donc le GPS
            ecrivain.write(null, new IIOImage(image, null, null), parametres);
        } finally {
            ecrivain.dispose();
        }
    }

    private static byte[] tirerAuSort() {
        byte[] octets = new byte[16];
        ALEA.nextBytes(octets);
        return octets;
    }

    private static boolean commencePar(byte[] octets, byte[] debut) {
        if (octets == null || octets.length < debut.length) return false;
        for (int i = 0; i < debut.length; i++) {
            if (octets[i] != debut[i]) return false;
        }
        return true;
    }
}
