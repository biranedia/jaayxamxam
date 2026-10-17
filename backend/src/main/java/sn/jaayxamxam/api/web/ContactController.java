package sn.jaayxamxam.api.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

/**
 * Le numéro de téléphone se demande un par un.
 *
 * <p>Avant, la recherche renvoyait tout l'annuaire avec les numéros : un script récupérait tous les
 * artisans de Dakar en une requête, et nos inscrits se faisaient démarcher. Le numéro a donc quitté
 * la liste. Il s'obtient ici, un profil à la fois, en comptant les appels et en les journalisant.
 *
 * <p>Cela ne rend pas la collecte impossible — rien ne la rend impossible, un humain peut toujours
 * recopier. Cela la rend lente, visible dans les journaux, et arrêtable.
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private static final Logger JOURNAL = LoggerFactory.getLogger(ContactController.class);

    private final ArtisanRepository artisans;
    private final LivreurRepository livreurs;

    public ContactController(ArtisanRepository artisans, LivreurRepository livreurs) {
        this.artisans = artisans;
        this.livreurs = livreurs;
    }

    @GetMapping("/artisan/{id}")
    @Transactional(readOnly = true)
    public ContactResponse artisan(@PathVariable Long id) {
        Artisan a = artisans.findById(id)
                .filter(Artisan::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artisan introuvable"));
        JOURNAL.info("Numéro consulté : artisan {}", id);
        return new ContactResponse(a.getTelephone());
    }

    @GetMapping("/livreur/{id}")
    @Transactional(readOnly = true)
    public ContactResponse livreur(@PathVariable Long id) {
        Livreur l = livreurs.findById(id)
                .filter(Livreur::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livreur introuvable"));
        JOURNAL.info("Numéro consulté : livreur {}", id);
        return new ContactResponse(l.getTelephone());
    }

    public record ContactResponse(String telephone) {
    }
}
