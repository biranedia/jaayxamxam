package sn.jaayxamxam.api.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Moyenne et nombre d'avis par identifiant, construits depuis une requête groupée
public record Stats(Double moyenne, long nombre) {

    public static final Stats AUCUN = new Stats(null, 0);

    // Chaque ligne vaut [id, moyenne, nombre]
    public static Map<Long, Stats> depuis(List<Object[]> lignes) {
        Map<Long, Stats> parId = new HashMap<>();
        for (Object[] ligne : lignes) {
            parId.put((Long) ligne[0], new Stats((Double) ligne[1], (Long) ligne[2]));
        }
        return parId;
    }
}
