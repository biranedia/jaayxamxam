package sn.jaayxamxam.api.admin;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.AvisRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Les façons concrètes d'entrer dans le back-office sans y être invité.
 *
 * <p>Chaque test correspond à une attaque réelle : deviner le mot de passe, fabriquer un cookie,
 * faire cliquer sur un lien piégé depuis un autre site, réutiliser une session fermée.
 *
 * <p>L'empreinte ci-dessous a été produite par {@link MotDePasse#hacher} pour la phrase
 * {@code atelier medina plombier 2026}. Elle est fixe pour que les tests soient reproductibles ;
 * c'est une phrase de test, elle n'ouvre rien d'autre que ces tests.
 */
@WebMvcTest({AdminController.class, AdminAuthController.class})
// AdminGarde est un filtre : @WebMvcTest le charge dans TOUTES les tranches web, même celles
// qui n'ont rien à voir avec l'administration. Il lui faut donc son magasin de sessions, qui est
// un simple @Component et n'est pas chargé par la tranche. Sans cet import, le contexte ne
// démarre pas et tous les tests web tombent d'un coup.
@Import(Sessions.class)
@TestPropertySource(properties = {
        "app.admin.hash=pbkdf2$600000$iaiQ18Hem+D4OztLLx97gQ$W/6jeafvgZp3FppLsxuu70567+4aRCFCELsQFCXfeU4",
        "app.admin.cookie-securise=true",
        "app.limites.admin-par-heure=1000",
        "app.limites.connexions-admin-par-heure=1000"
})
class AdminSecuriteTest {

    private static final String PHRASE = "atelier medina plombier 2026";

    @Autowired
    private MockMvc mvc;

    @MockitoBean private ArtisanRepository artisans;
    @MockitoBean private LivreurRepository livreurs;
    @MockitoBean private AvisRepository avis;
    @MockitoBean private JournalAdmin journal;

    private static String corps(String motDePasse) {
        return "{\"motDePasse\":\"" + motDePasse + "\"}";
    }

    /**
     * Se connecte et renvoie le cookie de session.
     *
     * <p>On relit l'en-tête {@code Set-Cookie} brut plutôt que de passer par
     * {@code getCookie()} : le cookie est posé par un en-tête, et on ne veut pas que ces tests
     * dépendent de la façon dont la réponse simulée le réinterprète.
     */
    private Cookie connexion() throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/connexion")
                        .contentType(MediaType.APPLICATION_JSON).content(corps(PHRASE)))
                .andExpect(status().isOk())
                .andReturn();
        return new Cookie(AdminGarde.COOKIE, valeurDuCookie(enTeteCookie(r)));
    }

    /** L'en-tête Set-Cookie de la réponse, qui doit toujours être présent après une connexion. */
    private static String enTeteCookie(MvcResult r) {
        String entete = r.getResponse().getHeader("Set-Cookie");
        assertThat(entete).as("en-tête Set-Cookie").isNotNull();
        return entete;
    }

    /** Extrait « admin_session=XXX » la valeur XXX, sans dépendre d'une bibliothèque. */
    private static String valeurDuCookie(String entete) {
        String debut = AdminGarde.COOKIE + "=";
        int i = entete.indexOf(debut);
        assertThat(i).as("cookie %s présent", AdminGarde.COOKIE).isNotNegative();
        String reste = entete.substring(i + debut.length());
        int fin = reste.indexOf(';');
        return fin < 0 ? reste : reste.substring(0, fin);
    }

    // ---------- Sans session ----------

    @Test
    @DisplayName("la file d'attente est inaccessible sans session")
    void sansSession() throws Exception {
        mvc.perform(get("/api/admin/attente")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un cookie fabriqué à la main ne vaut rien")
    void cookieInvente() throws Exception {
        mvc.perform(get("/api/admin/attente")
                        .cookie(new Cookie(AdminGarde.COOKIE, "jeton-fabrique-a-la-main")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un mauvais mot de passe est refusé et ne pose aucun cookie")
    void mauvaisMotDePasse() throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/connexion")
                        .contentType(MediaType.APPLICATION_JSON).content(corps("pas le bon")))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertThat(r.getResponse().getHeader("Set-Cookie")).isNull();
    }

    @Test
    @DisplayName("sans session, /moi répond simplement « pas connecté »")
    void moiSansSession() throws Exception {
        mvc.perform(get("/api/admin/moi"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.connecte").value(false));
    }

    // ---------- Avec session ----------

    /**
     * Le cookie doit être HttpOnly : sans cela, une faille d'injection n'importe où dans
     * l'application permettrait de lire la session et de se faire passer pour l'administrateur.
     */
    @Test
    @DisplayName("le cookie de session est HttpOnly, Secure, SameSite=Strict et limité à /api/admin")
    void proprietesDuCookie() throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/connexion")
                        .contentType(MediaType.APPLICATION_JSON).content(corps(PHRASE)))
                .andExpect(status().isOk())
                .andReturn();

        String entete = enTeteCookie(r);
        assertThat(entete).as("HttpOnly").contains("HttpOnly");
        assertThat(entete).as("Secure").contains("Secure");
        assertThat(entete).as("SameSite").contains("SameSite=Strict");
        assertThat(entete).as("portée").contains("Path=/api/admin");
        // Le jeton ne doit jamais apparaître dans le corps de la réponse
        assertThat(r.getResponse().getContentAsString()).doesNotContain(valeurDuCookie(entete));
    }

    @Test
    @DisplayName("avec une session valable, la file d'attente s'ouvre")
    void avecSession() throws Exception {
        when(artisans.findByActifFalseAndRefuseFalseOrderByCreeLeAsc()).thenReturn(List.of());
        when(livreurs.findByActifFalseAndRefuseFalseOrderByCreeLeAsc()).thenReturn(List.of());

        mvc.perform(get("/api/admin/attente").cookie(connexion()))
                .andExpect(status().isOk());
    }

    /**
     * Le test CSRF. Un cookie part tout seul avec chaque requête, y compris celle qu'un site
     * malveillant déclenche depuis un onglet ouvert à côté. L'en-tête exigé ne peut être posé
     * que par du JavaScript de même origine : un formulaire croisé en est incapable.
     */
    @Test
    @DisplayName("une action sans l'en-tête anti-CSRF est refusée, même avec une session valable")
    void csrfSansEntete() throws Exception {
        mvc.perform(post("/api/admin/artisan/1/valider").cookie(connexion()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("la même action passe avec l'en-tête")
    void csrfAvecEntete() throws Exception {
        sn.jaayxamxam.api.domain.Artisan a = new sn.jaayxamxam.api.domain.Artisan();
        a.setId(1L);
        a.setNom("Moussa Ndiaye");
        a.setTelephone("770000001");
        when(artisans.findById(1L)).thenReturn(java.util.Optional.of(a));
        when(artisans.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        mvc.perform(post("/api/admin/artisan/1/valider")
                        .cookie(connexion())
                        .header(AdminGarde.ENTETE_CSRF, "1"))
                .andExpect(status().isOk());
    }

    /** Une lecture ne change rien : inutile d'exiger l'en-tête, et cela simplifie l'interface. */
    @Test
    @DisplayName("une lecture n'exige pas l'en-tête anti-CSRF")
    void lectureSansEntete() throws Exception {
        when(artisans.findByActifFalseAndRefuseFalseOrderByCreeLeAsc()).thenReturn(List.of());
        when(livreurs.findByActifFalseAndRefuseFalseOrderByCreeLeAsc()).thenReturn(List.of());

        mvc.perform(get("/api/admin/attente").cookie(connexion()))
                .andExpect(status().isOk());
    }

    // ---------- Déconnexion ----------

    /** La déconnexion doit être réelle côté serveur, pas seulement un cookie effacé. */
    @Test
    @DisplayName("après déconnexion, l'ancien cookie ne rouvre plus rien")
    void deconnexion() throws Exception {
        Cookie session = connexion();

        MvcResult sortie = mvc.perform(post("/api/admin/deconnexion")
                        .cookie(session)
                        .header(AdminGarde.ENTETE_CSRF, "1"))
                .andExpect(status().isOk())
                .andReturn();

        // Le navigateur reçoit un cookie vidé et expiré sur-le-champ
        String efface = enTeteCookie(sortie);
        assertThat(valeurDuCookie(efface)).isEmpty();
        assertThat(efface).contains("Max-Age=0");

        // Et surtout : le jeton d'origine ne vaut plus rien, même s'il a été recopié
        mvc.perform(get("/api/admin/attente").cookie(session))
                .andExpect(status().isUnauthorized());
    }
}
