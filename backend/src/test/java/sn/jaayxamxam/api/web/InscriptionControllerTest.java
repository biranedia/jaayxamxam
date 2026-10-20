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
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.domain.Metier;
import sn.jaayxamxam.api.domain.Zone;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;
import sn.jaayxamxam.api.repository.MetierRepository;
import sn.jaayxamxam.api.repository.ZoneRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InscriptionController.class)
// AdminGarde est un filtre : @WebMvcTest le charge dans TOUTES les tranches web, même celles
// qui n'ont rien à voir avec l'administration. Il lui faut donc son magasin de sessions, qui est
// un simple @Component et n'est pas chargé par la tranche. Sans cet import, le contexte ne
// démarre pas et tous les tests web tombent d'un coup.
@Import(Sessions.class)
@TestPropertySource(properties = "app.limites.creations-par-heure=1000")
class InscriptionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ArtisanRepository artisans;

    @MockitoBean
    private LivreurRepository livreurs;

    @MockitoBean
    private MetierRepository metiers;

    @MockitoBean
    private ZoneRepository zones;

    private void referentielsConnus() {
        Metier metier = new Metier();
        metier.setId(1L);
        metier.setNom("Plombier");
        metier.setIcone("plombier");
        metier.setFamille("batiment");
        metier.setActif(true);
        when(metiers.findById(1L)).thenReturn(Optional.of(metier));

        Zone zone = new Zone();
        zone.setId(2L);
        zone.setVille("Dakar");
        zone.setQuartier("Médina");
        when(zones.findById(2L)).thenReturn(Optional.of(zone));

        when(artisans.existsByTelephone(anyString())).thenReturn(false);
        when(livreurs.existsByTelephone(anyString())).thenReturn(false);
        when(artisans.save(any())).thenAnswer(appel -> {
            Artisan a = appel.getArgument(0);
            a.setId(10L);
            return a;
        });
        when(livreurs.save(any())).thenAnswer(appel -> {
            Livreur l = appel.getArgument(0);
            l.setId(20L);
            return l;
        });
    }

    private static final String ARTISAN = """
            {"nom":"Moussa Ndiaye","telephone":"77 000 00 01","metierId":1,"zoneId":2,
             "description":"Fuites et chauffe-eau","anneesExperience":8,"tarifIndicatif":5000}
            """;

    private static final String LIVREUR = """
            {"nom":"Pape Sow","telephone":"77 100 00 01","vehicule":"moto","chargeMaxKg":30}
            """;

    @Test
    @DisplayName("L'artisan inscrit reçoit un code de gestion et attend la validation")
    void inscrireArtisan() throws Exception {
        referentielsConnus();

        mvc.perform(post("/api/inscriptions/artisan").contentType(MediaType.APPLICATION_JSON).content(ARTISAN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.codeGestion").value(org.hamcrest.Matchers.matchesPattern("[CAL][A-Z2-9]{8}")));
    }

    @Test
    @DisplayName("Un profil inscrit n'apparaît pas dans la recherche avant validation")
    void profilInactifALInscription() throws Exception {
        referentielsConnus();
        var capture = org.mockito.ArgumentCaptor.forClass(Artisan.class);

        mvc.perform(post("/api/inscriptions/artisan").contentType(MediaType.APPLICATION_JSON).content(ARTISAN))
                .andExpect(status().isCreated());

        verify(artisans).save(capture.capture());
        assertThat(capture.getValue().isActif()).isFalse();
        assertThat(capture.getValue().isVerifie()).isFalse();
        assertThat(capture.getValue().getTelephone()).isEqualTo("770000001");
    }

    @Test
    @DisplayName("Un numéro déjà inscrit est refusé, artisan comme livreur")
    void numeroDejaPris() throws Exception {
        referentielsConnus();
        when(livreurs.existsByTelephone("770000001")).thenReturn(true);

        mvc.perform(post("/api/inscriptions/artisan").contentType(MediaType.APPLICATION_JSON).content(ARTISAN))
                .andExpect(status().isConflict());
        verify(artisans, never()).save(any());
    }

    @Test
    @DisplayName("Un numéro invalide est refusé avant tout enregistrement")
    void numeroInvalide() throws Exception {
        referentielsConnus();
        String corps = ARTISAN.replace("77 000 00 01", "123");

        mvc.perform(post("/api/inscriptions/artisan").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest());
        verify(artisans, never()).save(any());
    }

    @Test
    @DisplayName("Un métier désactivé ne peut pas servir à s'inscrire")
    void metierDesactive() throws Exception {
        referentielsConnus();
        Metier retire = new Metier();
        retire.setId(1L);
        retire.setNom("Livreur");
        retire.setFamille("services");
        retire.setActif(false);
        when(metiers.findById(1L)).thenReturn(Optional.of(retire));

        mvc.perform(post("/api/inscriptions/artisan").contentType(MediaType.APPLICATION_JSON).content(ARTISAN))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Le livreur sans prix annoncés reprend le tarif indicatif de son véhicule")
    void livreurPrixParDefaut() throws Exception {
        referentielsConnus();
        var capture = org.mockito.ArgumentCaptor.forClass(Livreur.class);

        mvc.perform(post("/api/inscriptions/livreur").contentType(MediaType.APPLICATION_JSON).content(LIVREUR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));

        verify(livreurs).save(capture.capture());
        assertThat(capture.getValue().getPrixParKm()).isEqualTo(300);
        assertThat(capture.getValue().getPrixMinimum()).isEqualTo(1000);
        assertThat(capture.getValue().isActif()).isFalse();
    }

    @Test
    @DisplayName("Un véhicule inventé est refusé")
    void vehiculeInconnu() throws Exception {
        referentielsConnus();
        String corps = LIVREUR.replace("moto", "helicoptere");

        mvc.perform(post("/api/inscriptions/livreur").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest());
    }
}
