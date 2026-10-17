package sn.jaayxamxam.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Metier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    // Clé du dessin de l'outil, côté frontend
    private String icone;

    // batiment, maison, mecanique, beaute ou services : donne la couleur du panneau
    @Column(nullable = false)
    private String famille;

    // Un métier retiré de la recherche sans être supprimé (profils déjà rattachés)
    @Column(nullable = false)
    private boolean actif = true;
}
