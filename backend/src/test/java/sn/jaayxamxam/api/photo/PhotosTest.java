package sn.jaayxamxam.api.photo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Une photo envoyée par un inconnu est une pièce jointe hostile jusqu'à preuve du contraire.
 * Ces tests vérifient chacune des quatre précautions décrites dans {@link Photos}.
 */
class PhotosTest {

    @TempDir
    Path dossier;

    private Photos photos;

    @BeforeEach
    void creer() throws IOException {
        photos = new Photos(dossier);
    }

    // ---------- ce qui doit être refusé ----------

    @Test
    @DisplayName("Un fichier texte renommé en .jpg est refusé sur ses octets, pas sur son nom")
    void refuseUnTexte() {
        byte[] texte = "Ceci n'est pas une photo.".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> photos.enregistrer(texte)).isInstanceOf(PhotoRefusee.class);
    }

    @Test
    @DisplayName("Des octets de début de JPEG suivis de n'importe quoi ne passent pas le décodage")
    void refuseUnHybride() {
        byte[] hybride = new byte[1000];
        hybride[0] = (byte) 0xFF;
        hybride[1] = (byte) 0xD8;
        hybride[2] = (byte) 0xFF;
        assertThat(Photos.estUneImage(hybride)).isTrue();   // les octets magiques ne suffisent pas
        assertThatThrownBy(() -> photos.enregistrer(hybride)).isInstanceOf(PhotoRefusee.class);
    }

    @Test
    @DisplayName("Une image qui s'annonce en 25 000 × 25 000 est refusée sans jamais être décodée")
    void refuseUneBombeDeDecompression() {
        // L'en-tête suffit : décoder reviendrait à demander plusieurs gigaoctets de mémoire
        byte[] bombe = pngAnnoncant(25_000, 25_000);

        long debut = System.currentTimeMillis();
        assertThatThrownBy(() -> photos.enregistrer(bombe)).isInstanceOf(PhotoRefusee.class);
        assertThat(System.currentTimeMillis() - debut).isLessThan(2000);
    }

    @Test
    @DisplayName("Un envoi vide est refusé")
    void refuseLeVide() {
        assertThatThrownBy(() -> photos.enregistrer(new byte[0])).isInstanceOf(PhotoRefusee.class);
        assertThatThrownBy(() -> photos.enregistrer(null)).isInstanceOf(PhotoRefusee.class);
    }

    // ---------- ce qui doit être accepté ----------

    @Test
    @DisplayName("Une grande photo est réduite à 640 px et pèse beaucoup moins lourd")
    void reduitEtAllege() throws IOException {
        byte[] envoyee = jpeg(2400, 1600);

        String nom = photos.enregistrer(envoyee);
        Path fichier = dossier.resolve(nom);
        BufferedImage ecrite = ImageIO.read(fichier.toFile());

        assertThat(ecrite.getWidth()).isEqualTo(640);
        assertThat(ecrite.getHeight()).isEqualTo(427);
        assertThat(Files.size(fichier)).isLessThan(envoyee.length);
    }

    @Test
    @DisplayName("Le fichier écrit ne contient aucun bloc EXIF : ni appareil, ni position GPS")
    void effaceLesMetadonnees() throws IOException {
        String nom = photos.enregistrer(jpeg(800, 600));
        byte[] ecrit = Files.readAllBytes(dossier.resolve(nom));

        // « Exif » marque le début du bloc de métadonnées d'un JPEG
        assertThat(new String(ecrit, StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
    }

    @Test
    @DisplayName("Un PNG transparent devient blanc, pas noir")
    void gereLaTransparence() throws IOException {
        BufferedImage transparent = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(transparent, "png", sortie);

        String nom = photos.enregistrer(sortie.toByteArray());
        BufferedImage ecrite = ImageIO.read(dossier.resolve(nom).toFile());

        assertThat(ecrite.getRGB(100, 100) & 0xFFFFFF).isEqualTo(0xFFFFFF);
    }

    @Test
    @DisplayName("Deux envois de la même photo donnent deux noms différents")
    void nomsTiresAuHasard() throws IOException {
        byte[] photo = jpeg(300, 300);
        String premier = photos.enregistrer(photo);
        String second = photos.enregistrer(photo);

        assertThat(premier).matches("[0-9a-f]{32}\\.jpg").isNotEqualTo(second);
    }

    // ---------- lecture ----------

    @Test
    @DisplayName("Aucun nom ne permet de sortir du dossier des photos")
    void refuseLesCheminsPieges() {
        assertThat(photos.chemin("../../etc/passwd")).isNull();
        assertThat(photos.chemin("/etc/passwd")).isNull();
        assertThat(photos.chemin("..\\..\\windows\\win.ini")).isNull();
        assertThat(photos.chemin("photo.jpg")).isNull();
        assertThat(photos.chemin("0123456789abcdef0123456789abcdef.png")).isNull();
        assertThat(photos.chemin(null)).isNull();
    }

    @Test
    @DisplayName("Un nom bien formé mais inexistant ne renvoie pas de chemin")
    void refuseUnFichierAbsent() {
        assertThat(photos.chemin("0123456789abcdef0123456789abcdef.jpg")).isNull();
    }

    @Test
    @DisplayName("La photo remplacée est effacée, et une suppression piégée ne touche rien")
    void supprime() throws IOException {
        String nom = photos.enregistrer(jpeg(200, 200));
        assertThat(photos.chemin(nom)).isNotNull();

        photos.supprimer(nom);
        assertThat(photos.chemin(nom)).isNull();

        photos.supprimer("../../etc/passwd");   // ne doit rien faire, et surtout pas échouer
    }

    // ---------- fabrication des pièces d'épreuve ----------

    private static byte[] jpeg(int largeur, int hauteur) throws IOException {
        BufferedImage image = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
        Graphics2D dessin = image.createGraphics();
        dessin.setColor(new Color(30, 90, 160));
        dessin.fillRect(0, 0, largeur, hauteur);
        dessin.setColor(new Color(245, 178, 22));
        dessin.fillOval(largeur / 4, hauteur / 4, largeur / 2, hauteur / 2);
        dessin.dispose();

        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", sortie);
        return sortie.toByteArray();
    }

    /**
     * Un PNG dont l'en-tête annonce des dimensions énormes, sans les pixels qui vont avec.
     * C'est exactement la forme d'une bombe de décompression : quelques octets sur le disque,
     * des gigaoctets une fois décodée.
     */
    private static byte[] pngAnnoncant(int largeur, int hauteur) {
        byte[] signature = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
        byte[] ihdr = new byte[25];
        int i = 0;
        ihdr[i++] = 'I'; ihdr[i++] = 'H'; ihdr[i++] = 'D'; ihdr[i++] = 'R';
        i = ecrireEntier(ihdr, i, largeur);
        i = ecrireEntier(ihdr, i, hauteur);
        ihdr[i++] = 8;      // profondeur
        ihdr[i++] = 0;      // niveaux de gris
        ihdr[i++] = 0;      // compression
        ihdr[i++] = 0;      // filtrage
        ihdr[i] = 0;        // entrelacement

        CRC32 crc = new CRC32();
        crc.update(ihdr, 0, 17);

        byte[] fichier = new byte[signature.length + 4 + 17 + 4];
        System.arraycopy(signature, 0, fichier, 0, signature.length);
        int position = signature.length;
        position = ecrireEntier(fichier, position, 13);                  // longueur du bloc
        System.arraycopy(ihdr, 0, fichier, position, 17);
        position += 17;
        ecrireEntier(fichier, position, (int) crc.getValue());
        return fichier;
    }

    private static int ecrireEntier(byte[] cible, int position, int valeur) {
        cible[position] = (byte) (valeur >>> 24);
        cible[position + 1] = (byte) (valeur >>> 16);
        cible[position + 2] = (byte) (valeur >>> 8);
        cible[position + 3] = (byte) valeur;
        return position + 4;
    }
}
