package sn.jaayxamxam.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TelephoneTest {

    @ParameterizedTest
    @DisplayName("Les écritures courantes d'un numéro sénégalais donnent le même résultat")
    @ValueSource(strings = {
            "770000001", "77 000 00 01", "77-000-00-01", "+221 77 000 00 01", "221770000001", "(77) 000 00 01"
    })
    void normalise(String saisie) {
        assertThat(Telephone.normaliser(saisie)).isEqualTo("770000001");
    }

    @ParameterizedTest
    @DisplayName("Ce qui n'est pas un numéro à neuf chiffres est refusé")
    @ValueSource(strings = { "7700", "77000000012345", "abc", "00000000000000" })
    void refuse(String saisie) {
        assertThat(Telephone.normaliser(saisie)).isNull();
    }

    @ParameterizedTest
    @DisplayName("Vide ou absent est refusé, pas accepté comme numéro vide")
    @NullAndEmptySource
    void refuseVide(String saisie) {
        assertThat(Telephone.normaliser(saisie)).isNull();
    }
}
