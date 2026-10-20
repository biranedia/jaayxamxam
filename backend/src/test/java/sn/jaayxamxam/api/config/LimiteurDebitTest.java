package sn.jaayxamxam.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import sn.jaayxamxam.api.admin.Sessions;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.jaayxamxam.api.repository.CourseRepository;
import sn.jaayxamxam.api.repository.DemandeRepository;
import sn.jaayxamxam.api.web.SuiviController;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Essayer des codes de suivi au hasard doit devenir vite inutile
@WebMvcTest(SuiviController.class)
// AdminGarde est un filtre : @WebMvcTest le charge dans TOUTES les tranches web, même celles
// qui n'ont rien à voir avec l'administration. Il lui faut donc son magasin de sessions, qui est
// un simple @Component et n'est pas chargé par la tranche. Sans cet import, le contexte ne
// démarre pas et tous les tests web tombent d'un coup.
@Import(Sessions.class)
@TestPropertySource(properties = "app.limites.suivis-par-heure=3")
class LimiteurDebitTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DemandeRepository demandes;

    @MockitoBean
    private CourseRepository courses;

    @Test
    @DisplayName("Au-delà de la limite horaire, l'API répond 429 au lieu de chercher")
    void bloqueApresLaLimite() throws Exception {
        when(demandes.findByCodeSuivi(any())).thenReturn(Optional.empty());
        when(courses.findByCodeSuivi(any())).thenReturn(Optional.empty());

        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/suivi/CODE" + i + "XXX")).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/suivi/ENCOREUN")).andExpect(status().isTooManyRequests());
    }
}
