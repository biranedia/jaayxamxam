package sn.jaayxamxam.api.vocal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ce qui entre ici vient de l'extérieur : un fichier déposé par n'importe qui sur Internet.
 * Chaque test correspond à une façon concrète d'essayer de faire passer autre chose.
 */
class VocauxTest {

    @TempDir
    Path dossier;

    private Vocaux vocaux;

    @BeforeEach
    void preparer() throws IOException {
        vocaux = new Vocaux(dossier);
    }

    /** Fabrique un fichier d'une taille donnée commençant par les octets d'en-tête voulus. */
    private static byte[] avec(byte[] debut, int taille) {
        byte[] b = new byte[taille];
        new Random(7).nextBytes(b);
        System.arraycopy(debut, 0, b, 0, debut.length);
        return b;
    }

    private static byte[] webm() { return avec(new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3 }, 50_000); }
    private static byte[] ogg()  { return avec(new byte[] { 'O', 'g', 'g', 'S' }, 50_000); }

    private static byte[] mp4() {
        byte[] b = new byte[50_000];
        new Random(3).nextBytes(b);
        // Un MP4 commence par la taille de la première boîte, puis « ftyp »
        b[0] = 0; b[1] = 0; b[2] = 0; b[3] = 0x20;
        b[4] = 'f'; b[5] = 't'; b[6] = 'y'; b[7] = 'p';
        return b;
    }

    @Test
    @DisplayName("reconnaît les trois contenants que produisent les navigateurs")
    void reconnaitLesFormats() {
        assertThat(Vocaux.extension(webm())).isEqualTo("webm");   // Chrome, Android
        assertThat(Vocaux.extension(ogg())).isEqualTo("ogg");     // Firefox
        assertThat(Vocaux.extension(mp4())).isEqualTo("m4a");     // Safari, iOS
    }

    /**
     * On ne regarde ni le nom du fichier ni le type annoncé : les deux viennent du client et se
     * falsifient. Seuls les premiers octets décident.
     */
    @Test
    @DisplayName("refuse tout ce qui n'est pas un de ces contenants, quel que soit le nom donné")
    void refuseLeReste() {
        assertThat(Vocaux.extension("ceci est du texte et pas du son".getBytes())).isNull();
        assertThat(Vocaux.extension(avec(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }, 50_000)))
                .as("une image JPEG").isNull();
        assertThat(Vocaux.extension(avec(new byte[] { 'P', 'K', 3, 4 }, 50_000)))
                .as("une archive ZIP").isNull();
        assertThat(Vocaux.extension(avec(new byte[] { 0x7F, 'E', 'L', 'F' }, 50_000)))
                .as("un exécutable").isNull();
        assertThat(Vocaux.extension(new byte[] { 1, 2, 3 })).as("trop court").isNull();
        assertThat(Vocaux.extension(null)).isNull();
    }

    @Test
    @DisplayName("enregistre sous un nom tiré au hasard, jamais deux fois le même")
    void enregistre() throws IOException {
        String nom = vocaux.enregistrer(webm());

        assertThat(nom).matches("[0-9a-f]{32}\\.webm");
        assertThat(dossier.resolve(nom)).exists();
        assertThat(Files.size(dossier.resolve(nom))).isEqualTo(50_000);
        assertThat(vocaux.enregistrer(webm())).isNotEqualTo(nom);
    }

    /** Un fichier à moitié écrit ne doit jamais être servi : d'où l'écriture puis le déplacement. */
    @Test
    @DisplayName("ne laisse aucun fichier temporaire derrière lui")
    void pasDeTemporaire() throws IOException {
        vocaux.enregistrer(ogg());
        try (var liste = Files.list(dossier)) {
            assertThat(liste.map(Path::toString)).noneMatch(n -> n.endsWith(".part"));
        }
    }

    @Test
    @DisplayName("refuse le vide, le trop gros, le trop court et le mauvais format")
    void refuseAlEnregistrement() {
        assertThatThrownBy(() -> vocaux.enregistrer(new byte[0]))
                .isInstanceOf(VocalRefuse.class);
        assertThatThrownBy(() -> vocaux.enregistrer(null))
                .isInstanceOf(VocalRefuse.class);
        assertThatThrownBy(() -> vocaux.enregistrer(
                avec(new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3 }, 1_600_000)))
                .isInstanceOf(VocalRefuse.class)
                .hasMessageContaining("trop long");
        assertThatThrownBy(() -> vocaux.enregistrer(
                avec(new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3 }, 500)))
                .isInstanceOf(VocalRefuse.class)
                .hasMessageContaining("trop court");
        assertThatThrownBy(() -> vocaux.enregistrer(avec(new byte[] { 'P', 'K', 3, 4 }, 50_000)))
                .isInstanceOf(VocalRefuse.class);
    }

    /**
     * Le test qui compte : sans contrôle du nom, « ../application.yml » serait un chemin
     * acceptable et le serveur livrerait sa propre configuration.
     */
    @Test
    @DisplayName("ne sert jamais un fichier hors de son dossier")
    void traverseeDeRepertoire() {
        assertThat(vocaux.chemin("../application.yml")).isNull();
        assertThat(vocaux.chemin("../../etc/passwd")).isNull();
        assertThat(vocaux.chemin("/etc/passwd")).isNull();
        assertThat(vocaux.chemin("..%2Fsecret.webm")).isNull();
        assertThat(vocaux.chemin("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ.webm")).as("hors hexadécimal").isNull();
        assertThat(vocaux.chemin("court.webm")).isNull();
        assertThat(vocaux.chemin(null)).isNull();
        assertThat(vocaux.chemin("00000000000000000000000000000000.exe")).as("autre extension").isNull();
        assertThat(vocaux.chemin("00000000000000000000000000000000.webm")).as("bien formé mais absent").isNull();
    }

    @Test
    @DisplayName("sert un type MIME déduit de l'extension, jamais de ce que le client annonce")
    void typeMime() {
        assertThat(Vocaux.typeMime("a.webm")).isEqualTo("audio/webm");
        assertThat(Vocaux.typeMime("a.ogg")).isEqualTo("audio/ogg");
        assertThat(Vocaux.typeMime("a.m4a")).isEqualTo("audio/mp4");
        assertThat(Vocaux.typeMime("a.html")).isEqualTo("application/octet-stream");
        assertThat(Vocaux.typeMime(null)).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("supprime un vocal, et ignore une demande de suppression hors dossier")
    void suppression() {
        String nom = vocaux.enregistrer(webm());
        vocaux.supprimer(nom);
        assertThat(dossier.resolve(nom)).doesNotExist();

        // Ne doit ni lever ni toucher quoi que ce soit en dehors
        vocaux.supprimer("../application.yml");
        vocaux.supprimer(null);
    }
}
