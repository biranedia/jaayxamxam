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
import sn.jaayxamxam.api.domain.Demande;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.DemandeRepository;
import sn.jaayxamxam.api.repository.MetierRepository;
import sn.jaayxamxam.api.repository.ZoneRepository;

@RestController
@RequestMapping("/api/demandes")
public class DemandeController {

    private final DemandeRepository demandes;
    private final MetierRepository metiers;
    private final ZoneRepository zones;
    private final ArtisanRepository artisans;

    public DemandeController(DemandeRepository demandes, MetierRepository metiers,
                             ZoneRepository zones, ArtisanRepository artisans) {
        this.demandes = demandes;
        this.metiers = metiers;
        this.zones = zones;
        this.artisans = artisans;
    }

    // POST /api/demandes : crée une demande de service (aucun compte requis pour l'instant)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DemandeResponse creer(@Valid @RequestBody DemandeRequest req) {
        String telephone = Telephone.normaliser(req.clientTelephone());
        if (telephone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numéro de téléphone invalide");
        }

        // Écrit ou parlé, il faut l'un des deux : sans cela l'artisan reçoit une demande vide
        String texte = req.description() == null ? "" : req.description().trim();
        boolean parle = req.vocal() != null && !req.vocal().isBlank();
        if (texte.isEmpty() && !parle) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Décrivez le travail, en écrivant ou en enregistrant un message.");
        }

        Demande d = new Demande();
        // Un code déjà pris ferait échouer l'insertion sur la contrainte d'unicité :
        // une lecture indexée coûte moins cher qu'une erreur 500 inexplicable
        d.setCodeSuivi(Codes.libre(CodeSuivi.DEMANDE, c -> demandes.findByCodeSuivi(c).isPresent()));
        d.setClientNom(req.clientNom().trim());
        d.setClientTelephone(telephone);
        d.setDescription(texte.isEmpty() ? null : texte);
        d.setVocal(parle ? req.vocal() : null);
        d.setUrgente(req.urgente());
        d.setLatitude(req.latitude());
        d.setLongitude(req.longitude());
        d.setMetier(metiers.findById(req.metierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Métier inconnu")));
        d.setZone(zones.findById(req.zoneId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quartier inconnu")));

        // Artisan contacté : facultatif, mais il doit exister et être actif
        if (req.artisanId() != null) {
            Artisan a = artisans.findById(req.artisanId())
                    .filter(Artisan::isActif)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artisan introuvable"));
            d.setArtisan(a);
        }

        Demande enregistree = demandes.save(d);
        return new DemandeResponse(enregistree.getId(), enregistree.getCodeSuivi(), enregistree.getStatut().name());
    }
}
