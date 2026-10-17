package sn.jaayxamxam.api.photo;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Envoi et lecture des photos de profil.
 *
 * <p>Pour déposer une photo, il faut le code de gestion reçu à l'inscription : c'est ce qui prouve
 * qu'on est bien le propriétaire du profil, en attendant la vérification du téléphone. Le code passe
 * dans le formulaire et non dans l'adresse, pour ne pas se retrouver dans les journaux du serveur
 * ni dans l'historique du navigateur.
 */
@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final Photos photos;
    private final ArtisanRepository artisans;
    private final LivreurRepository livreurs;

    public PhotoController(Photos photos, ArtisanRepository artisans, LivreurRepository livreurs) {
        this.photos = photos;
        this.artisans = artisans;
        this.livreurs = livreurs;
    }

    @PostMapping("/artisan")
    @Transactional
    public PhotoResponse photoArtisan(@RequestParam("code") String code,
                                      @RequestParam("fichier") MultipartFile fichier) {
        Artisan a = artisans.findByCodeGestion(nettoyer(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code de gestion inconnu"));

        String ancienne = a.getPhoto();
        a.setPhoto(photos.enregistrer(octets(fichier)));
        artisans.save(a);
        photos.supprimer(ancienne);
        return new PhotoResponse(a.getPhoto());
    }

    @PostMapping("/livreur")
    @Transactional
    public PhotoResponse photoLivreur(@RequestParam("code") String code,
                                      @RequestParam("fichier") MultipartFile fichier) {
        Livreur l = livreurs.findByCodeGestion(nettoyer(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code de gestion inconnu"));

        String ancienne = l.getPhoto();
        l.setPhoto(photos.enregistrer(octets(fichier)));
        livreurs.save(l);
        photos.supprimer(ancienne);
        return new PhotoResponse(l.getPhoto());
    }

    /**
     * Les noms sont tirés au hasard et ne sont jamais réutilisés : une photo peut donc être gardée
     * très longtemps par le navigateur. Changer de photo change le nom, donc l'adresse.
     */
    @GetMapping("/{nom}")
    public ResponseEntity<Resource> lire(@PathVariable String nom) {
        Path fichier = photos.chemin(nom);
        if (fichier == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo introuvable");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(new FileSystemResource(fichier));
    }

    private static byte[] octets(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new PhotoRefusee("Aucune photo reçue.");
        }
        try {
            return fichier.getBytes();
        } catch (IOException e) {
            throw new PhotoRefusee("La photo n'a pas pu être lue.");
        }
    }

    private static String nettoyer(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    public record PhotoResponse(String photo) {
    }
}
