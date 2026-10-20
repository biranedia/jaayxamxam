package sn.jaayxamxam.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_nom", nullable = false)
    private String clientNom;

    @Column(name = "client_telephone", nullable = false)
    private String clientTelephone;

    // Facultative depuis les vocaux : un client qui enregistre n'a rien à écrire.
    // La base vérifie qu'au moins l'un des deux est présent (contrainte demande_a_une_description).
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Nom du fichier audio déposé par le client, ou null s'il a seulement écrit. */
    @Column(length = 64)
    private String vocal;

    @Column(nullable = false)
    private boolean urgente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemande statut = StatutDemande.NOUVELLE;

    // Secret court remis au client pour suivre sa demande et laisser un avis
    @Column(name = "code_suivi", nullable = false, unique = true, updatable = false)
    private String codeSuivi = CodeSuivi.generer(CodeSuivi.DEMANDE);

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "metier_id")
    private Metier metier;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    // Artisan contacté (facultatif)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artisan_id")
    private Artisan artisan;

    // Position du client au moment de l'envoi, si elle a été autorisée
    private Double latitude;
    private Double longitude;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe = LocalDateTime.now();
}
