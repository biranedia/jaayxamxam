package sn.jaayxamxam.api.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.AvisRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le défaut le plus courant d'un back-office : un déploiement qui oublie la configuration et
 * laisse la porte ouverte. Ici, l'oubli ferme tout — il n'existe aucun mot de passe par défaut.
 */
@WebMvcTest({AdminController.class, AdminAuthController.class})
// AdminGarde est un filtre : @WebMvcTest le charge dans TOUTES les tranches web, même celles
// qui n'ont rien à voir avec l'administration. Il lui faut donc son magasin de sessions, qui est
// un simple @Component et n'est pas chargé par la tranche. Sans cet import, le contexte ne
// démarre pas et tous les tests web tombent d'un coup.
@Import(Sessions.class)
@TestPropertySource(properties = {
        "app.admin.hash=",                      // volontairement vide
        "app.limites.admin-par-heure=1000",
        "app.limites.connexions-admin-par-heure=1000"
})
class AdminFermeeParDefautTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean private ArtisanRepository artisans;
    @MockitoBean private LivreurRepository livreurs;
    @MockitoBean private AvisRepository avis;
    @MockitoBean private JournalAdmin journal;

    @Test
    @DisplayName("sans mot de passe configuré, la lecture est refusée")
    void lectureRefusee() throws Exception {
        mvc.perform(get("/api/admin/attente")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sans mot de passe configuré, on ne peut même pas tenter de se connecter")
    void connexionRefusee() throws Exception {
        mvc.perform(post("/api/admin/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motDePasse\":\"n'importe quoi\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aucune chaîne vide ne sert de mot de passe")
    void motDePasseVideRefuse() throws Exception {
        mvc.perform(post("/api/admin/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motDePasse\":\"\"}"))
                .andExpect(status().isForbidden());
    }
}
