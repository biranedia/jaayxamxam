package sn.jaayxamxam.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Une course « Yonni la » : d'un quartier à un autre
@Entity
@Getter
@Setter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_nom", nullable = false)
    private String clientNom;

    @Column(name = "client_telephone", nullable = false)
    private String clientTelephone;

    @Column(nullable = false)
    private String vehicule;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "depart_zone_id")
    private Zone depart;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_zone_id")
    private Zone destination;

    // Point de repère donné par le client (« en face de la pharmacie »)
    private String repere;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "distance_m", nullable = false)
    private int distanceM;

    // Prix indicatif en FCFA, à confirmer avec le livreur
    @Column(name = "prix_estime", nullable = false)
    private int prixEstime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCourse statut = StatutCourse.NOUVELLE;

    @Column(name = "code_suivi", nullable = false, unique = true, updatable = false)
    private String codeSuivi = CodeSuivi.generer(CodeSuivi.COURSE);

    // Livreur choisi, ou null tant que personne n'a pris la course
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livreur_id")
    private Livreur livreur;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe = LocalDateTime.now();
}
