package sn.jaayxamxam.api.domain;

import java.util.Set;

// Véhicules acceptés. Les valeurs sont en minuscules : mêmes mots en base, dans l'API et à l'écran.
public final class Vehicule {

    public static final String MOTO = "moto";
    public static final String TRICYCLE = "tricycle";
    public static final String VOITURE = "voiture";
    public static final String CAMIONNETTE = "camionnette";

    private static final Set<String> TOUS = Set.of(MOTO, TRICYCLE, VOITURE, CAMIONNETTE);

    private Vehicule() {
    }

    public static boolean existe(String vehicule) {
        return vehicule != null && TOUS.contains(vehicule);
    }
}
