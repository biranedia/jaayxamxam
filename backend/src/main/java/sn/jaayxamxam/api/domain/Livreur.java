package sn.jaayxamxam.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Livreur ou transporteur du module « Yonni la »
@Entity
@Getter
@Setter
public class Livreur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String telephone;

    // moto, tricycle, voiture ou camionnette (voir Vehicule)
    @Column(nullable = false)
    private String vehicule;

    @Column(name = "charge_max_kg", nullable = false)
    private int chargeMaxKg = 30;

    @Column(name = "prix_par_km", nullable = false)
    private int prixParKm;

    @Column(name = "prix_minimum", nullable = false)
    private int prixMinimum;

    @Column(nullable = false)
    private boolean verifie;

    @Column(nullable = false)
    private boolean actif = true;

    private Double latitude;
    private Double longitude;

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
