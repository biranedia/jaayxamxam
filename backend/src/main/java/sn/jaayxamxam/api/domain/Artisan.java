package sn.jaayxamxam.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Artisan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String telephone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "annees_experience", nullable = false)
    private int anneesExperience;

    // Tarif indicatif en FCFA
    @Column(name = "tarif_indicatif")
    private Integer tarifIndicatif;

    // Badge "Vérifié" (CNI contrôlée manuellement)
    @Column(nullable = false)
    private boolean verifie;

    @Column(nullable = false)
    private boolean actif = true;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "metier_id")
    private Metier metier;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    // Dernière position envoyée par le téléphone de l'artisan
    private Double latitude;
    private Double longitude;

    // Sert à dire « en ligne » : position envoyée il y a moins de dix minutes
    @Column(name = "derniere_activite")
    private LocalDateTime derniereActivite;


    // Inscription écartée par l'administration : gardée en base, invisible, réversible
    @Column(nullable = false)
    private boolean refuse;

    // Nom du fichier de la photo de profil, null tant qu'il n'y en a pas
    @Column(length = 64)
    private String photo;

    // Secret remis au travailleur à l'inscription, en attendant l'authentification par téléphone
    @Column(name = "code_gestion", unique = true, updatable = false)
    private String codeGestion;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe = LocalDateTime.now();
}
