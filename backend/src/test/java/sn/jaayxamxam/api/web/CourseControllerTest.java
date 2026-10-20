package sn.jaayxamxam.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import sn.jaayxamxam.api.admin.Sessions;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.jaayxamxam.api.domain.Course;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.domain.Zone;
import sn.jaayxamxam.api.repository.CourseRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;
import sn.jaayxamxam.api.repository.ZoneRepository;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
// AdminGarde est un filtre : @WebMvcTest le charge dans TOUTES les tranches web, même celles
// qui n'ont rien à voir avec l'administration. Il lui faut donc son magasin de sessions, qui est
// un simple @Component et n'est pas chargé par la tranche. Sans cet import, le contexte ne
// démarre pas et tous les tests web tombent d'un coup.
@Import(Sessions.class)
// La limitation de débit a son propre test : ici elle ne doit pas gêner
@TestPropertySource(properties = "app.limites.creations-par-heure=1000")
class CourseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CourseRepository courses;

    @MockitoBean
    private ZoneRepository zones;

    @MockitoBean
    private LivreurRepository livreurs;

    private static Zone zone(long id, String quartier, Double lat, Double lon) {
        Zone z = new Zone();
        z.setId(id);
        z.setVille("Dakar");
        z.setQuartier(quartier);
        z.setLatitude(lat);
        z.setLongitude(lon);
        return z;
    }

    private void quartiersConnus() {
        when(zones.findById(2L)).thenReturn(Optional.of(zone(2, "Médina", 14.68, -17.45)));
        when(zones.findById(5L)).thenReturn(Optional.of(zone(5, "Golf Sud", 14.78, -17.39)));
        when(courses.save(any())).thenAnswer(appel -> {
            Course c = appel.getArgument(0);
            c.setId(1L);
            return c;
        });
    }

    private static String corps(String vehicule, Long livreurId) {
        String livreur = livreurId == null ? "" : ",\"livreurId\":" + livreurId;
        return """
                {"clientNom":"Awa Diop","clientTelephone":"77 000 00 09","vehicule":"%s",
                 "departZoneId":2,"destinationZoneId":5,"precision":"En face de la pharmacie"%s}
                """.formatted(vehicule, livreur);
    }

    @Test
    @DisplayName("Médina → Golf Sud en moto : 16,7 km et 5 000 FCFA, comme à l'écran")
    void prixAnnonce() throws Exception {
        quartiersConnus();

        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(corps("moto", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.distanceM").value(16_713))
                .andExpect(jsonPath("$.prixEstime").value(5000))
                .andExpect(jsonPath("$.statut").value("NOUVELLE"))
                .andExpect(jsonPath("$.codeSuivi").value(org.hamcrest.Matchers.matchesPattern("[CAL][A-Z2-9]{8}")));
    }

    @Test
    @DisplayName("Le même trajet en camionnette coûte plus cher")
    void tarifParVehicule() throws Exception {
        quartiersConnus();

        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(corps("camionnette", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixEstime").value(13_400));
    }

    @Test
    @DisplayName("Les prix du livreur choisi l'emportent sur le tarif indicatif")
    void prixDuLivreur() throws Exception {
        quartiersConnus();
        Livreur l = new Livreur();
        l.setId(7L);
        l.setNom("Pape Sow");
        l.setTelephone("771000001");
        l.setVehicule("moto");
        l.setPrixParKm(500);
        l.setPrixMinimum(2000);
        when(livreurs.findById(7L)).thenReturn(Optional.of(l));

        // 16,713 km × 500 FCFA = 8 356, arrondi à 8 400
        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(corps("moto", 7L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixEstime").value(8400));
    }

    @Test
    @DisplayName("Un livreur qui conduit un autre véhicule est refusé")
    void vehiculeIncoherent() throws Exception {
        quartiersConnus();
        Livreur l = new Livreur();
        l.setId(7L);
        l.setVehicule("camionnette");
        l.setPrixParKm(800);
        l.setPrixMinimum(5000);
        when(livreurs.findById(7L)).thenReturn(Optional.of(l));

        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(corps("moto", 7L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un véhicule inventé est refusé")
    void vehiculeInconnu() throws Exception {
        quartiersConnus();

        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(corps("helicoptere", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un numéro de téléphone invalide est refusé")
    void telephoneInvalide() throws Exception {
        quartiersConnus();
        String corps = """
                {"clientNom":"Awa Diop","clientTelephone":"123","vehicule":"moto",
                 "departZoneId":2,"destinationZoneId":5}
                """;

        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un quartier sans position ne peut pas donner de prix")
    void quartierSansPosition() throws Exception {
        when(zones.findById(2L)).thenReturn(Optional.of(zone(2, "Médina", null, null)));
        when(zones.findById(5L)).thenReturn(Optional.of(zone(5, "Golf Sud", 14.78, -17.39)));

        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(corps("moto", null)))
                .andExpect(status().isBadRequest());
    }
}
