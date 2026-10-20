package sn.jaayxamxam.api.vocal;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Dépôt et lecture des messages vocaux.
 *
 * <p>Le dépôt est anonyme : le client n'a pas de compte, il enregistre avant même d'avoir envoyé
 * sa demande. Le nom rendu est ensuite joint à la demande. C'est le limiteur de débit qui empêche
 * d'en déposer des milliers, comme pour les photos.
 *
 * <p>Un vocal décrit un problème chez quelqu'un : plus privé qu'une photo de profil. D'où le nom
 * sur 128 bits aléatoires, et surtout {@code Cache-Control: private, no-store} — on ne veut pas
 * qu'un proxy partagé en garde une copie.
 */
@RestController
@RequestMapping("/api/vocaux")
public class VocalController {

    private final Vocaux vocaux;

    public VocalController(Vocaux vocaux) {
        this.vocaux = vocaux;
    }

    public record VocalResponse(String vocal) {
    }

    @PostMapping
    public VocalResponse deposer(@RequestParam("fichier") MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new VocalRefuse("Aucun enregistrement reçu.");
        }
        try {
            return new VocalResponse(vocaux.enregistrer(fichier.getBytes()));
        } catch (IOException e) {
            throw new VocalRefuse("L'enregistrement n'a pas pu être lu.");
        }
    }

    /**
     * Sert le fichier.
     *
     * <p>Le type MIME vient de l'extension que nous avons choisie d'après les octets, jamais de
     * ce que le client avait annoncé. Avec {@code nosniff} posé par EntetesSecurite, aucun
     * navigateur ne tentera d'y voir autre chose que de l'audio.
     */
    @GetMapping("/{nom}")
    public ResponseEntity<Resource> lire(@PathVariable String nom) {
        Path fichier = vocaux.chemin(nom);
        if (fichier == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enregistrement introuvable");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(Vocaux.typeMime(nom)))
                // Jamais en cache partagé : le contenu est privé même si l'adresse est indevinable
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(new FileSystemResource(fichier));
    }
}
