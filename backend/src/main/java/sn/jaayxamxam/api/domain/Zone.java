package sn.jaayxamxam.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ville;

    @Column(nullable = false)
    private String quartier;

    // Point central du quartier, pour estimer le prix d'une course
    private Double latitude;
    private Double longitude;
}
