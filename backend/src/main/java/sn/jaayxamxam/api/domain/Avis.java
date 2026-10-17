package sn.jaayxamxam.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Avis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Exactement une des deux cibles est renseignée (contrainte ck_avis_cible)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artisan_id")
    private Artisan artisan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livreur_id")
    private Livreur livreur;

    // Une seule des deux sources, et un seul avis par demande ou par course
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id")
    private Demande demande;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // Note de 1 à 5 (contrôlée aussi par la base)
    @Column(nullable = false)
    private int note;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    // Avis masqué par la modération
    @Column(nullable = false)
    private boolean masque;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe = LocalDateTime.now();
}
