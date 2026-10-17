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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@TestPropertySource(properties = {
        "app.admin.jeton=jeton-de-test-bien-long",
        "app.limites.admin-par-heure=1000"
})
class AdminGardeTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ArtisanRepository artisans;

    @MockitoBean
    private LivreurRepository livreurs;

    @MockitoBean
    private AvisRepository avis;

    @Test
    @DisplayName("Sans en-tête, l'administration est fermée")
    void sansEntete() throws Exception {
        mvc.perform(get("/api/admin/attente")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un jeton presque juste reste refusé")
    void mauvaisJeton() throws Exception {
        mvc.perform(get("/api/admin/attente").header("X-Admin-Token", "jeton-de-test-bien-lon"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Avec le bon jeton, la file d'attente s'ouvre")
    void bonJeton() throws Exception {
        when(artisans.findByActifFalseAndRefuseFalseOrderByCreeLeAsc()).thenReturn(List.of());
        when(livreurs.findByActifFalseAndRefuseFalseOrderByCreeLeAsc()).thenReturn(List.of());
        when(artisans.countByActifTrue()).thenReturn(4L);
        when(livreurs.countByActifTrue()).thenReturn(2L);

        mvc.perform(get("/api/admin/attente").header("X-Admin-Token", "jeton-de-test-bien-long"))
                .andExpect(status().isOk());
    }
}
