package sn.jaayxamxam.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Demande;
import sn.jaayxamxam.api.domain.Metier;
import sn.jaayxamxam.api.domain.StatutDemande;
import sn.jaayxamxam.api.domain.Zone;
import sn.jaayxamxam.api.repository.CourseRepository;
import sn.jaayxamxam.api.repository.DemandeRepository;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuiviController.class)
class SuiviControllerTest {

    private static final double LAT_EXACTE = 14.681783;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DemandeRepository demandes;

    @MockitoBean
    private CourseRepository courses;

    private static Demande demande(StatutDemande statut) {
        Metier metier = new Metier();
        metier.setNom("Plombier");
        metier.setIcone("plombier");
        metier.setFamille("batiment");

        Zone zone = new Zone();
        zone.setVille("Dakar");
        zone.setQuartier("Médina");

        Artisan a = new Artisan();
        a.setId(1L);
        a.setNom("Moussa Ndiaye");
        a.setTelephone("770000001");
        a.setMetier(metier);
        a.setZone(zone);
        a.setLatitude(LAT_EXACTE);
        a.setLongitude(-17.452341);

        Demande d = new Demande();
        d.setId(1L);
        d.setClientNom("Awa Diop");
        d.setClientTelephone("770000009");
        d.setDescription("Fuite sous l'évier");
        d.setStatut(statut);
        d.setMetier(metier);
        d.setZone(zone);
        d.setArtisan(a);
        return d;
    }

    @Test
    @DisplayName("Le code de suivi donne l'état de la demande et l'artisan")
    void suitUneDemande() throws Exception {
        Demande d = demande(StatutDemande.ACCEPTEE);
        when(demandes.findByCodeSuivi(d.getCodeSuivi())).thenReturn(Optional.of(d));

        mvc.perform(get("/api/suivi/" + d.getCodeSuivi()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("demande"))
                .andExpect(jsonPath("$.statut").value("ACCEPTEE"))
                .andExpect(jsonPath("$.intervenant.nom").value("Moussa Ndiaye"));
    }

    @Test
    @DisplayName("Travail engagé : le client voit la position exacte de l'artisan qui arrive")
    void positionExactePendantLeTravail() throws Exception {
        Demande d = demande(StatutDemande.EN_ROUTE);
        when(demandes.findByCodeSuivi(d.getCodeSuivi())).thenReturn(Optional.of(d));

        mvc.perform(get("/api/suivi/" + d.getCodeSuivi()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervenant.latitude").value(LAT_EXACTE));
    }

    @Test
    @DisplayName("Demande pas encore acceptée : la position reste approximative")
    void positionFloutteeAvantAcceptation() throws Exception {
        Demande d = demande(StatutDemande.NOUVELLE);
        when(demandes.findByCodeSuivi(d.getCodeSuivi())).thenReturn(Optional.of(d));

        mvc.perform(get("/api/suivi/" + d.getCodeSuivi()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervenant.latitude").value(14.682));
    }

    @Test
    @DisplayName("Un code inconnu donne 404 sans indiquer s'il a existé")
    void codeInconnu() throws Exception {
        when(demandes.findByCodeSuivi(any())).thenReturn(Optional.empty());
        when(courses.findByCodeSuivi(any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/suivi/ZZZZZZZZ")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Le code est accepté en minuscules : on le recopie à la main")
    void codeEnMinuscules() throws Exception {
        Demande d = demande(StatutDemande.ACCEPTEE);
        when(demandes.findByCodeSuivi(d.getCodeSuivi())).thenReturn(Optional.of(d));

        mvc.perform(get("/api/suivi/" + d.getCodeSuivi().toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervenant.nom").value("Moussa Ndiaye"));
    }
}
