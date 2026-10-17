package sn.jaayxamxam.api.web;

import sn.jaayxamxam.api.domain.Vehicule;

import java.util.Map;

// Tarifs indicatifs utilisés quand le client n'a pas encore choisi de livreur.
// Dès qu'un livreur est choisi, ce sont ses propres prix qui s'appliquent.
public final class Tarifs {

    public record Tarif(int parKm, int minimum) {
    }

    private static final Map<String, Tarif> PAR_VEHICULE = Map.of(
            Vehicule.MOTO, new Tarif(300, 1000),
            Vehicule.TRICYCLE, new Tarif(400, 1500),
            Vehicule.VOITURE, new Tarif(500, 2000),
            Vehicule.CAMIONNETTE, new Tarif(800, 5000));

    private Tarifs() {
    }

    public static Tarif pour(String vehicule) {
        return PAR_VEHICULE.getOrDefault(vehicule, PAR_VEHICULE.get(Vehicule.MOTO));
    }
}
