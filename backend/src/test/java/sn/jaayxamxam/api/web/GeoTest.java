package sn.jaayxamxam.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Les mêmes calculs tournent côté navigateur : les deux doivent donner le même prix
class GeoTest {

    private static final double MEDINA_LAT = 14.68;
    private static final double MEDINA_LON = -17.45;
    private static final double GOLF_SUD_LAT = 14.78;
    private static final double GOLF_SUD_LON = -17.39;

    @Test
    @DisplayName("Médina → Golf Sud fait environ 12,8 km à vol d'oiseau")
    void distanceEntreDeuxQuartiers() {
        assertThat(Geo.distance(MEDINA_LAT, MEDINA_LON, GOLF_SUD_LAT, GOLF_SUD_LON))
                .isBetween(12_000, 13_500);
    }

    @Test
    @DisplayName("La distance d'un point à lui-même est nulle")
    void distanceNulle() {
        assertThat(Geo.distance(MEDINA_LAT, MEDINA_LON, MEDINA_LAT, MEDINA_LON)).isZero();
    }

    @Test
    @DisplayName("Sans coordonnée, pas de distance plutôt qu'une distance fausse")
    void distanceInconnue() {
        assertThat(Geo.distance(null, MEDINA_LON, GOLF_SUD_LAT, GOLF_SUD_LON)).isNull();
        assertThat(Geo.distance(MEDINA_LAT, MEDINA_LON, null, null)).isNull();
    }

    @Test
    @DisplayName("Le trajet par les rues majore la distance de 30 %")
    void distanceRoute() {
        int route = Geo.distanceRoute(MEDINA_LAT, MEDINA_LON, GOLF_SUD_LAT, GOLF_SUD_LON);
        int volOiseau = Geo.distance(MEDINA_LAT, MEDINA_LON, GOLF_SUD_LAT, GOLF_SUD_LON);
        assertThat(route).isEqualTo(Math.round(volOiseau * 1.3f));
    }

    @Test
    @DisplayName("16,7 km en moto : 5 000 FCFA, comme à l'écran")
    void prixMoto() {
        assertThat(Geo.prix(16_713, 300, 1000)).isEqualTo(5000);
    }

    @Test
    @DisplayName("16,7 km en camionnette : 13 400 FCFA")
    void prixCamionnette() {
        assertThat(Geo.prix(16_713, 800, 5000)).isEqualTo(13_400);
    }

    @Test
    @DisplayName("Une course très courte est facturée au minimum du livreur")
    void prixMinimum() {
        assertThat(Geo.prix(600, 300, 1000)).isEqualTo(1000);
        assertThat(Geo.prix(0, 300, 1000)).isEqualTo(1000);
    }

    @Test
    @DisplayName("Le prix tombe toujours sur une centaine de francs")
    void prixArrondi() {
        for (int metres = 500; metres < 30_000; metres += 137) {
            assertThat(Geo.prix(metres, 300, 1000) % 100).isZero();
        }
    }
}
