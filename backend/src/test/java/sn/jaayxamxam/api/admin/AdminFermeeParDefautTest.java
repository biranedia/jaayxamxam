package sn.jaayxamxam.api.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.AvisRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Aucun jeton configuré : c'est l'état d'un serveur fraîchement déployé
@WebMvcTest(AdminController.class)
@TestPropertySource(properties = "app.limites.admin-par-heure=1000")
class AdminFermeeParDefautTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ArtisanRepository artisans;

    @MockitoBean
    private LivreurRepository livreurs;

    @MockitoBean
    private AvisRepository avis;

    @Test
    @DisplayName("Sans ADMIN_TOKEN, l'administration est injoignable quoi qu'on envoie")
    void fermeParDefaut() throws Exception {
        // Pas d'« admin/admin » : un serveur mal configuré n'expose rien du tout
        mvc.perform(get("/api/admin/attente")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/attente").header("X-Admin-Token", ""))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/attente").header("X-Admin-Token", "n'importe quoi"))
                .andExpect(status().isForbidden());
    }
}
