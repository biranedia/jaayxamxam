package sn.jaayxamxam.api.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Avis;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.AvisRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Administration : valider les inscriptions, donner le badge, masquer un avis.
 *
 * <p>Protégé par {@link AdminGarde}. Rien ici n'est destructeur : un refus garde le profil en base,
 * un avis masqué reste lisible en base. Tout est réversible, parce qu'une erreur de validation à
 * 23 h ne doit pas effacer le gagne-pain de quelqu'un.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger JOURNAL = LoggerFactory.getLogger(AdminController.class);

    private final ArtisanRepository artisans;
    private final LivreurRepository livreurs;
    private final AvisRepository avis;

    public AdminController(ArtisanRepository artisans, LivreurRepository livreurs, AvisRepository avis) {
        this.artisans = artisans;
        this.livreurs = livreurs;
        this.avis = avis;
    }

    public record EnAttente(Long id, String type, String nom, String telephone, String detail,
                            String photo, LocalDateTime creeLe) {
    }

    public record Resume(int artisansEnAttente, int livreursEnAttente,
                         long artisansActifs, long livreursActifs, List<EnAttente> file) {
    }

    /** La file d'attente, les plus anciens d'abord : c'est l'écran de travail. */
    @GetMapping("/attente")
    @Transactional(readOnly = true)
    public Resume attente() {
        List<EnAttente> file = new java.util.ArrayList<>();
        List<Artisan> a = artisans.findByActifFalseAndRefuseFalseOrderByCreeLeAsc();
        List<Livreur> l = livreurs.findByActifFalseAndRefuseFalseOrderByCreeLeAsc();

        a.forEach(x -> file.add(new EnAttente(x.getId(), "artisan", x.getNom(), x.getTelephone(),
                x.getMetier().getNom() + " · " + x.getZone().getQuartier(), x.getPhoto(), x.getCreeLe())));
        l.forEach(x -> file.add(new EnAttente(x.getId(), "livreur", x.getNom(), x.getTelephone(),
                x.getVehicule() + " · jusqu'à " + x.getChargeMaxKg() + " kg", x.getPhoto(), x.getCreeLe())));
        file.sort(java.util.Comparator.comparing(EnAttente::creeLe));

        return new Resume(a.size(), l.size(),
                artisans.countByActifTrue(), livreurs.countByActifTrue(), file);
    }

    @PostMapping("/artisan/{id}/valider")
    @Transactional
    public EnAttente validerArtisan(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean valeur) {
        Artisan a = artisan(id);
        a.setActif(valeur);
        if (valeur) a.setRefuse(false);
        JOURNAL.info("Artisan {} : actif = {}", id, valeur);
        return resume(artisans.save(a));
    }

    @PostMapping("/artisan/{id}/verifier")
    @Transactional
    public EnAttente verifierArtisan(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean valeur) {
        Artisan a = artisan(id);
        a.setVerifie(valeur);
        JOURNAL.info("Artisan {} : vérifié = {}", id, valeur);
        return resume(artisans.save(a));
    }

    @PostMapping("/artisan/{id}/refuser")
    @Transactional
    public EnAttente refuserArtisan(@PathVariable Long id) {
        Artisan a = artisan(id);
        a.setRefuse(true);
        a.setActif(false);
        JOURNAL.info("Artisan {} : refusé", id);
        return resume(artisans.save(a));
    }

    @PostMapping("/livreur/{id}/valider")
    @Transactional
    public EnAttente validerLivreur(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean valeur) {
        Livreur l = livreur(id);
        l.setActif(valeur);
        if (valeur) l.setRefuse(false);
        JOURNAL.info("Livreur {} : actif = {}", id, valeur);
        return resume(livreurs.save(l));
    }

    @PostMapping("/livreur/{id}/verifier")
    @Transactional
    public EnAttente verifierLivreur(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean valeur) {
        Livreur l = livreur(id);
        l.setVerifie(valeur);
        JOURNAL.info("Livreur {} : vérifié = {}", id, valeur);
        return resume(livreurs.save(l));
    }

    @PostMapping("/livreur/{id}/refuser")
    @Transactional
    public EnAttente refuserLivreur(@PathVariable Long id) {
        Livreur l = livreur(id);
        l.setRefuse(true);
        l.setActif(false);
        JOURNAL.info("Livreur {} : refusé", id);
        return resume(livreurs.save(l));
    }

    /** Masquer un avis plutôt que l'effacer : on peut revenir dessus. */
    @PostMapping("/avis/{id}/masquer")
    @Transactional
    public void masquerAvis(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean valeur) {
        Avis a = avis.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avis introuvable"));
        a.setMasque(valeur);
        avis.save(a);
        JOURNAL.info("Avis {} : masqué = {}", id, valeur);
    }

    private Artisan artisan(Long id) {
        return artisans.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artisan introuvable"));
    }

    private Livreur livreur(Long id) {
        return livreurs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livreur introuvable"));
    }

    private static EnAttente resume(Artisan a) {
        return new EnAttente(a.getId(), "artisan", a.getNom(), a.getTelephone(),
                a.isActif() ? "Visible" : (a.isRefuse() ? "Refusé" : "En attente"), a.getPhoto(), a.getCreeLe());
    }

    private static EnAttente resume(Livreur l) {
        return new EnAttente(l.getId(), "livreur", l.getNom(), l.getTelephone(),
                l.isActif() ? "Visible" : (l.isRefuse() ? "Refusé" : "En attente"), l.getPhoto(), l.getCreeLe());
    }
}
