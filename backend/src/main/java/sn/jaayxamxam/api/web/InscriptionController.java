package sn.jaayxamxam.api.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.CodeSuivi;
import sn.jaayxamxam.api.domain.Codes;
import sn.jaayxamxam.api.domain.CodeSuivi;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.domain.Metier;
import sn.jaayxamxam.api.domain.Vehicule;
import sn.jaayxamxam.api.domain.Zone;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;
import sn.jaayxamxam.api.repository.MetierRepository;
import sn.jaayxamxam.api.repository.ZoneRepository;

/**
 * Inscription des travailleurs : artisans d'un côté, livreurs Yonni la de l'autre.
 *
 * <p>Le profil créé est <strong>inactif</strong> : il n'apparaît nulle part tant que quelqu'un ne
 * l'a pas validé. C'est volontaire — sans vérification du téléphone, n'importe qui pourrait inscrire
 * n'importe quel numéro, y compris celui d'un concurrent.
 */
@RestController
@RequestMapping("/api/inscriptions")
public class InscriptionController {

    private static final String EN_ATTENTE = "EN_ATTENTE";

    private final ArtisanRepository artisans;
    private final LivreurRepository livreurs;
    private final MetierRepository metiers;
    private final ZoneRepository zones;

    public InscriptionController(ArtisanRepository artisans, LivreurRepository livreurs,
                                 MetierRepository metiers, ZoneRepository zones) {
        this.artisans = artisans;
        this.livreurs = livreurs;
        this.metiers = metiers;
        this.zones = zones;
    }

    @PostMapping("/artisan")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public InscriptionResponse artisan(@Valid @RequestBody InscriptionArtisanRequest req) {
        String telephone = telephoneLibre(req.telephone());

        Metier metier = metiers.findById(req.metierId())
                .filter(Metier::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Métier inconnu"));
        Zone zone = zones.findById(req.zoneId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quartier inconnu"));

        Artisan a = new Artisan();
        a.setNom(req.nom().trim());
        a.setTelephone(telephone);
        a.setDescription(texte(req.description()));
        a.setAnneesExperience(req.anneesExperience());
        a.setTarifIndicatif(req.tarifIndicatif());
        a.setMetier(metier);
        a.setZone(zone);
        a.setActif(false);      // en attente de validation
        a.setVerifie(false);    // le badge se gagne après contrôle de la pièce d'identité
        a.setCodeGestion(CodeSuivi.generer(CodeSuivi.ARTISAN));

        a.setCodeGestion(Codes.libre(CodeSuivi.ARTISAN, c -> artisans.findByCodeGestion(c).isPresent()));
        Artisan cree = artisans.save(a);
        return new InscriptionResponse(cree.getId(), cree.getCodeGestion(), EN_ATTENTE);
    }

    @PostMapping("/livreur")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public InscriptionResponse livreur(@Valid @RequestBody InscriptionLivreurRequest req) {
        String telephone = telephoneLibre(req.telephone());

        if (!Vehicule.existe(req.vehicule())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Véhicule inconnu");
        }

        // Sans prix annoncés, on part du tarif indicatif du véhicule
        Tarifs.Tarif defaut = Tarifs.pour(req.vehicule());

        Livreur l = new Livreur();
        l.setNom(req.nom().trim());
        l.setTelephone(telephone);
        l.setVehicule(req.vehicule());
        l.setChargeMaxKg(req.chargeMaxKg());
        l.setPrixParKm(req.prixParKm() == null ? defaut.parKm() : req.prixParKm());
        l.setPrixMinimum(req.prixMinimum() == null ? defaut.minimum() : req.prixMinimum());
        l.setActif(false);
        l.setVerifie(false);
        l.setCodeGestion(CodeSuivi.generer(CodeSuivi.LIVREUR));

        l.setCodeGestion(Codes.libre(CodeSuivi.LIVREUR, c -> livreurs.findByCodeGestion(c).isPresent()));
        Livreur cree = livreurs.save(l);
        return new InscriptionResponse(cree.getId(), cree.getCodeGestion(), EN_ATTENTE);
    }

    /**
     * Un numéro ne porte qu'un seul profil, artisan ou livreur. C'est aussi ce qui empêche
     * d'inscrire cent fois la même personne, en attendant la vérification par SMS.
     */
    private String telephoneLibre(String saisie) {
        String telephone = Telephone.normaliser(saisie);
        if (telephone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numéro de téléphone invalide");
        }
        if (artisans.existsByTelephone(telephone) || livreurs.existsByTelephone(telephone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro est déjà inscrit");
        }
        return telephone;
    }

    private static String texte(String valeur) {
        if (valeur == null) return null;
        String propre = valeur.trim();
        return propre.isEmpty() ? null : propre;
    }
}
