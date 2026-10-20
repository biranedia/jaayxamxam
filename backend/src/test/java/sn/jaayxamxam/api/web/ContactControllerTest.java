package sn.jaayxamxam.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import sn.jaayxamxam.api.admin.Sessions;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
// AdminGarde est un filtre : @WebMvcTest le charge dans TOUTES les tranches web, même celles
// qui n'ont rien à voir avec l'administration. Il lui faut donc son magasin de sessions, qui est
// un simple @Component et n'est pas chargé par la tranche. Sans cet import, le contexte ne
// démarre pas et tous les tests web tombent d'un coup.
@Import(Sessions.class)
@TestPropertySource(properties = "app.limites.contacts-par-heure=1000")
class ContactControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ArtisanRepository artisans;

    @MockitoBean
    private LivreurRepository livreurs;

    @Test
    @DisplayName("Le numéro d'un artisan visible se demande un par un")
    void numeroArtisan() throws Exception {
        Artisan a = new Artisan();
        a.setId(1L);
        a.setNom("Moussa");
        a.setTelephone("770000001");
        a.setActif(true);
        when(artisans.findById(1L)).thenReturn(Optional.of(a));

        mvc.perform(get("/api/contacts/artisan/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephone").value("770000001"))
                // Rien d'autre ne sort par cette porte
                .andExpect(jsonPath("$.nom").doesNotExist());
    }

    @Test
    @DisplayName("Le numéro d'un profil non validé n'est pas donné")
    void profilInactif() throws Exception {
        Artisan a = new Artisan();
        a.setId(2L);
        a.setTelephone("770000002");
        a.setActif(false);
        when(artisans.findById(2L)).thenReturn(Optional.of(a));

        mvc.perform(get("/api/contacts/artisan/2")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un identifiant inconnu donne 404")
    void inconnu() throws Exception {
        when(artisans.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/contacts/artisan/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Même porte pour les livreurs")
    void numeroLivreur() throws Exception {
        Livreur l = new Livreur();
        l.setId(3L);
        l.setTelephone("771000001");
        l.setActif(true);
        when(livreurs.findById(3L)).thenReturn(Optional.of(l));

        mvc.perform(get("/api/contacts/livreur/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephone").value("771000001"));
    }
}
