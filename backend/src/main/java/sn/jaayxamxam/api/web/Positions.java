package sn.jaayxamxam.api.web;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Les positions publiées sont volontairement imprécises.
 *
 * <p>Un artisan partage sa position pour être trouvé, pas pour que n'importe qui sache où il dort.
 * Trois décimales valent environ 110 mètres : assez pour trier par proximité et afficher une
 * distance utile, trop grossier pour désigner une maison. La position exacte reste en base et sert
 * au calcul des distances côté serveur ; elle n'est donnée qu'au client qui suit un travail en cours.
 */
public final class Positions {

    private static final int DECIMALES = 3;

    private Positions() {
    }

    public static Double flouter(Double coordonnee) {
        if (coordonnee == null) return null;
        return BigDecimal.valueOf(coordonnee).setScale(DECIMALES, RoundingMode.HALF_UP).doubleValue();
    }
}
