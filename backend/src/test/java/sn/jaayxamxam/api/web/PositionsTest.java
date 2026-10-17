package sn.jaayxamxam.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// La position publiée ne doit jamais désigner une maison
class PositionsTest {

    @Test
    @DisplayName("Trois décimales, soit environ cent mètres")
    void arrondit() {
        assertThat(Positions.flouter(14.681783)).isEqualTo(14.682);
        assertThat(Positions.flouter(-17.452341)).isEqualTo(-17.452);
    }

    @Test
    @DisplayName("Une position absente le reste")
    void laissePasserNull() {
        assertThat(Positions.flouter(null)).isNull();
    }

    @Test
    @DisplayName("L'écart introduit ne dépasse jamais cent mètres environ")
    void ecartBorne() {
        double exact = 14.6817836;
        double floutee = Positions.flouter(exact);
        int ecart = Geo.distance(exact, -17.4523, floutee, -17.4523);
        assertThat(ecart).isLessThan(120);
    }

    @Test
    @DisplayName("Deux adresses voisines se confondent une fois floutées")
    void voisinsIndistinguables() {
        // Deux maisons à une trentaine de mètres l'une de l'autre
        assertThat(Positions.flouter(14.68201)).isEqualTo(Positions.flouter(14.68223));
    }
}
