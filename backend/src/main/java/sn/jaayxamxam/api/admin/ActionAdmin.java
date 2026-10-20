package sn.jaayxamxam.api.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Une ligne du journal d'administration. Rien n'y est jamais modifié ni supprimé. */
@Entity
@Table(name = "action_admin")
@Getter
@Setter
public class ActionAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Adresse d'où l'action a été faite. Une seule personne, mais plusieurs endroits possibles. */
    @Column(length = 64)
    private String adresse;

    /** VALIDER_ARTISAN, REFUSER_LIVREUR, CONNEXION_REFUSEE… */
    @Column(nullable = false, length = 48)
    private String action;

    /** Sur qui ou sur quoi : « artisan:12 », « avis:7 ». Nul pour les connexions. */
    @Column(length = 64)
    private String cible;

    /** Précision libre, jamais de donnée sensible ni de mot de passe. */
    @Column(length = 255)
    private String detail;

    @Column(name = "faite_le", nullable = false, updatable = false)
    private LocalDateTime faiteLe = LocalDateTime.now();
}
