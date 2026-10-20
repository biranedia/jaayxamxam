package sn.jaayxamxam.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import sn.jaayxamxam.api.admin.Sessions;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Metier;
import sn.jaayxamxam.api.domain.Zone;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.AvisRepository;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtisanController.class)
// AdminGarde est un filtre : @WebMvcTest le charge dans TOUTES les tranches web, même celles
// qui n'ont rien à voir avec l'administration. Il lui faut donc son magasin de sessions, qui est
// un simple @Component et n'est pas chargé par la tranche. Sans cet import, le contexte ne
// démarre pas et tous les tests web tombent d'un coup.
@Import(Sessions.class)
class ArtisanControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ArtisanRepository artisans;

    @MockitoBean
    private AvisRepository avis;

    private static Artisan artisan(long id, String nom, double lat, double lon) {
        Metier metier = new Metier();
        metier.setId(1L);
        metier.setNom("Plombier");
        metier.setIcone("plombier");
        metier.setFamille("batiment");

        Zone zone = new Zone();
        zone.setId(2L);
        zone.setVille("Dakar");
        zone.setQuartier("Médina");

        Artisan a = new Artisan();
        a.setId(id);
        a.setNom(nom);
        a.setTelephone("77000000" + id);
        a.setAnneesExperience(8);
        a.setVerifie(true);
        a.setMetier(metier);
        a.setZone(zone);
        a.setLatitude(lat);
        a.setLongitude(lon);
        return a;
    }

    @Test
    @DisplayName("Avec la position du visiteur, le plus proche arrive en premier")
    void triParProximite() throws Exception {
        Artisan loin = artisan(1, "Loin", 14.78, -17.39);
        Artisan proche = artisan(2, "Proche", 14.6818, -17.4523);
        when(artisans.rechercher(any(), any())).thenReturn(List.of(loin, proche));
        when(avis.statsArtisans(any())).thenReturn(List.of());

        mvc.perform(get("/api/artisans").param("lat", "14.68").param("lon", "-17.45"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Proche"))
                .andExpect(jsonPath("$[1].nom").value("Loin"))
                .andExpect(jsonPath("$[0].distanceM").isNumber());
    }

    @Test
    @DisplayName("La position publiée est arrondie : elle ne désigne pas une maison")
    void positionFloutee() throws Exception {
        when(artisans.rechercher(any(), any())).thenReturn(List.of(artisan(1, "Moussa", 14.681783, -17.452341)));
        when(avis.statsArtisans(any())).thenReturn(List.of());

        mvc.perform(get("/api/artisans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latitude").value(14.682))
                .andExpect(jsonPath("$[0].longitude").value(-17.452));
    }

    @Test
    @DisplayName("Sans position du visiteur, aucune distance n'est inventée")
    void sansPosition() throws Exception {
        when(artisans.rechercher(any(), any())).thenReturn(List.of(artisan(1, "Moussa", 14.68, -17.45)));
        when(avis.statsArtisans(any())).thenReturn(List.of());

        mvc.perform(get("/api/artisans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distanceM").value(nullValue()));
    }

    @Test
    @DisplayName("La note moyenne et le nombre d'avis viennent d'une seule requête groupée")
    void notesGroupees() throws Exception {
        when(artisans.rechercher(any(), any())).thenReturn(List.of(artisan(1, "Moussa", 14.68, -17.45)));
        when(avis.statsArtisans(any())).thenReturn(List.<Object[]>of(new Object[] { 1L, 4.8, 12L }));

        mvc.perform(get("/api/artisans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].noteMoyenne").value(4.8))
                .andExpect(jsonPath("$[0].nombreAvis").value(12));
    }

    @Test
    @DisplayName("Un artisan inconnu donne 404, pas une page vide")
    void ficheInconnue() throws Exception {
        when(artisans.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/artisans/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un artisan désactivé n'est plus consultable")
    void ficheDesactivee() throws Exception {
        Artisan a = artisan(5, "Parti", 14.68, -17.45);
        a.setActif(false);
        when(artisans.findById(5L)).thenReturn(Optional.of(a));

        mvc.perform(get("/api/artisans/5")).andExpect(status().isNotFound());
    }
}
