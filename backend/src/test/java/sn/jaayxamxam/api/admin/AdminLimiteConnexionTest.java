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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Essayer des mots de passe doit devenir inutile très vite.
 *
 * <p>Classe à part parce que le compteur est porté par le contrôleur : mélanger ce test aux
 * autres épuiserait leurs tentatives et les ferait échouer pour une mauvaise raison.
 */
@WebMvcTest({AdminController.class, AdminAuthController.class})
// AdminGarde est un filtre : @WebMvcTest le charge dans TOUTES les tranches web, même celles
// qui n'ont rien à voir avec l'administration. Il lui faut donc son magasin de sessions, qui est
// un simple @Component et n'est pas chargé par la tranche. Sans cet import, le contexte ne
// démarre pas et tous les tests web tombent d'un coup.
@Import(Sessions.class)
@TestPropertySource(properties = {
        "app.admin.hash=pbkdf2$600000$iaiQ18Hem+D4OztLLx97gQ$W/6jeafvgZp3FppLsxuu70567+4aRCFCELsQFCXfeU4",
        "app.limites.admin-par-heure=1000",
        "app.limites.connexions-admin-par-heure=3"
})
class AdminLimiteConnexionTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean private ArtisanRepository artisans;
    @MockitoBean private LivreurRepository livreurs;
    @MockitoBean private AvisRepository avis;
    @MockitoBean private JournalAdmin journal;

    @Test
    @DisplayName("bloque après le nombre d'essais autorisé, même avec le bon mot de passe ensuite")
    void bloqueApresTropDEssais() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mvc.perform(post("/api/admin/connexion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"motDePasse\":\"essai " + i + "\"}"))
                    .andExpect(status().isUnauthorized());
        }

        // Le quatrième essai ne compare même plus : il est refusé d'emblée
        mvc.perform(post("/api/admin/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motDePasse\":\"essai 4\"}"))
                .andExpect(status().isTooManyRequests());

        // Et le bon mot de passe ne passe pas non plus : la porte est fermée pour l'heure
        mvc.perform(post("/api/admin/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motDePasse\":\"atelier medina plombier 2026\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
