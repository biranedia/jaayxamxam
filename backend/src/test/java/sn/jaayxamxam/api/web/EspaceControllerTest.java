package sn.jaayxamxam.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import sn.jaayxamxam.api.admin.Sessions;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.jaayxamxam.api.domain.Artisan;
import sn.jaayxamxam.api.domain.Demande;
import sn.jaayxamxam.api.domain.Livreur;
import sn.jaayxamxam.api.domain.Metier;
import sn.jaayxamxam.api.domain.StatutDemande;
import sn.jaayxamxam.api.domain.Zone;
import sn.jaayxamxam.api.repository.ArtisanRepository;
import sn.jaayxamxam.api.repository.CourseRepository;
import sn.jaayxamxam.api.repository.DemandeRepository;
import sn.jaayxamxam.api.repository.LivreurRepository;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EspaceController.class)
// AdminGarde est un filtre : @WebMvcTest le charge dans TOUTES les tranches web, même celles
// qui n'ont rien à voir avec l'administration. Il lui faut donc son magasin de sessions, qui est
// un simple @Component et n'est pas chargé par la tranche. Sans cet import, le contexte ne
// démarre pas et tous les tests web tombent d'un coup.
@Import(Sessions.class)
class EspaceControllerTest {

    private static final String CODE_ARTISAN = "A7KM2QXP4";
    private static final String CODE_LIVREUR = "L3BN8RTW6";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ArtisanRepository artisans;

    @MockitoBean
    private LivreurRepository livreurs;

    @MockitoBean
    private DemandeRepository demandes;

    @MockitoBean
    private CourseRepository courses;

    private static Artisan artisan(boolean actif) {
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
        a.setActif(actif);
        a.setCodeGestion(CODE_ARTISAN);
        return a;
    }

    private static Livreur livreur() {
        Livreur l = new Livreur();
        l.setId(5L);
        l.setNom("Ibrahima Sarr");
        l.setTelephone("770000009");
        l.setVehicule("moto");
        l.setActif(true);
        l.setCodeGestion(CODE_LIVREUR);
        return l;
    }

    @Test
    @DisplayName("ouvre l'espace d'un artisan avec son code de gestion")
    void ouvreEspaceArtisan() throws Exception {
        when(artisans.findByCodeGestion(CODE_ARTISAN)).thenReturn(Optional.of(artisan(true)));
        when(demandes.findByArtisanIdOrderByCreeLeDesc(1L)).thenReturn(List.of());

        mvc.perform(post("/api/espace").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"" + CODE_ARTISAN + "\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("artisan"))
                .andExpect(jsonPath("$.nom").value("Moussa Ndiaye"))
                .andExpect(jsonPath("$.etat").value("VALIDE"))
                // Son propre numéro lui est rendu : c'est le sien
                .andExpect(jsonPath("$.telephone").value("770000001"));
    }

    @Test
    @DisplayName("accepte un code en minuscules, avec espaces et tirets")
    void codeTolerant() throws Exception {
        when(artisans.findByCodeGestion(CODE_ARTISAN)).thenReturn(Optional.of(artisan(true)));
        when(demandes.findByArtisanIdOrderByCreeLeDesc(1L)).thenReturn(List.of());

        mvc.perform(post("/api/espace").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\" a7km-2qxp4 \"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("artisan"));
    }

    @Test
    @DisplayName("refuse un code inconnu sans dire lequel existe")
    void codeInconnu() throws Exception {
        when(artisans.findByCodeGestion(any())).thenReturn(Optional.empty());
        when(livreurs.findByCodeGestion(any())).thenReturn(Optional.empty());

        mvc.perform(post("/api/espace").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"ZZZZZZZZZ\"}")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Code inconnu. Vérifiez celui reçu à l'inscription."));
    }

    @Test
    @DisplayName("traite un code mal formé comme un code inconnu")
    void codeMalForme() throws Exception {
        mvc.perform(post("/api/espace").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABC\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("enregistre la position et marque le travailleur comme actif à l'instant")
    void envoiePosition() throws Exception {
        Artisan a = artisan(true);
        when(artisans.findByCodeGestion(CODE_ARTISAN)).thenReturn(Optional.of(a));
        when(artisans.save(any(Artisan.class))).thenAnswer(i -> i.getArgument(0));
        when(demandes.findByArtisanIdOrderByCreeLeDesc(1L)).thenReturn(List.of());

        mvc.perform(post("/api/espace/position").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"" + CODE_ARTISAN
                                + "\",\"latitude\":14.6812,\"longitude\":-17.4521}")))
                .andExpect(status().isOk())
                // La position que le travailleur voit est la sienne, exacte et non floutée
                .andExpect(jsonPath("$.latitude").value(14.6812))
                .andExpect(jsonPath("$.derniereActivite").isNotEmpty());
    }

    @Test
    @DisplayName("interdit de se déclarer disponible tant que le profil n'est pas validé")
    void profilEnAttenteNePeutPasSeDeclarer() throws Exception {
        when(artisans.findByCodeGestion(CODE_ARTISAN)).thenReturn(Optional.of(artisan(false)));

        mvc.perform(post("/api/espace/position").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"" + CODE_ARTISAN
                                + "\",\"latitude\":14.6812,\"longitude\":-17.4521}")))
                .andExpect(status().isForbidden());

        verify(artisans, never()).save(any(Artisan.class));
    }

    @Test
    @DisplayName("refuse une latitude hors des bornes terrestres")
    void positionAberrante() throws Exception {
        mvc.perform(post("/api/espace/position").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"" + CODE_ARTISAN
                                + "\",\"latitude\":200,\"longitude\":-17.4521}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("le passage hors ligne vide la dernière activité mais garde la position")
    void passeHorsLigne() throws Exception {
        Artisan a = artisan(true);
        a.setLatitude(14.6812);
        a.setLongitude(-17.4521);
        when(artisans.findByCodeGestion(CODE_ARTISAN)).thenReturn(Optional.of(a));
        when(artisans.save(any(Artisan.class))).thenAnswer(i -> i.getArgument(0));
        when(demandes.findByArtisanIdOrderByCreeLeDesc(1L)).thenReturn(List.of());

        mvc.perform(post("/api/espace/hors-ligne").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"" + CODE_ARTISAN + "\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.derniereActivite").doesNotExist())
                .andExpect(jsonPath("$.latitude").value(14.6812));
    }

    @Test
    @DisplayName("un artisan voit les demandes qui lui sont adressées")
    void listeSesDemandes() throws Exception {
        Artisan a = artisan(true);

        Zone zone = new Zone();
        zone.setVille("Dakar");
        zone.setQuartier("Médina");

        Demande d = new Demande();
        d.setId(42L);
        d.setClientNom("Awa Diop");
        d.setClientTelephone("770000055");
        d.setDescription("Fuite sous l'évier");
        d.setUrgente(true);
        d.setStatut(StatutDemande.NOUVELLE);
        d.setZone(zone);
        d.setArtisan(a);

        when(artisans.findByCodeGestion(CODE_ARTISAN)).thenReturn(Optional.of(a));
        when(demandes.findByArtisanIdOrderByCreeLeDesc(1L)).thenReturn(List.of(d));

        mvc.perform(post("/api/espace").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"" + CODE_ARTISAN + "\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travaux[0].id").value(42))
                .andExpect(jsonPath("$.travaux[0].clientNom").value("Awa Diop"))
                .andExpect(jsonPath("$.travaux[0].urgente").value(true))
                .andExpect(jsonPath("$.travaux[0].lieu").value("Médina, Dakar"));
    }

    @Test
    @DisplayName("accepte une demande qui lui appartient")
    void accepteSaDemande() throws Exception {
        Artisan a = artisan(true);

        Demande d = new Demande();
        d.setId(42L);
        d.setArtisan(a);
        d.setStatut(StatutDemande.NOUVELLE);

        when(artisans.findByCodeGestion(CODE_ARTISAN)).thenReturn(Optional.of(a));
        when(demandes.findById(42L)).thenReturn(Optional.of(d));
        when(demandes.save(any(Demande.class))).thenAnswer(i -> i.getArgument(0));
        when(demandes.findByArtisanIdOrderByCreeLeDesc(1L)).thenReturn(List.of(d));

        mvc.perform(post("/api/espace/travaux/42/statut").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"" + CODE_ARTISAN + "\",\"statut\":\"ACCEPTEE\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travaux[0].statut").value("ACCEPTEE"));
    }

    /**
     * Le test qui compte vraiment : sans la vérification du propriétaire, n'importe quel
     * travailleur pourrait accepter ou refuser le travail d'un autre.
     */
    @Test
    @DisplayName("ne touche pas à la demande d'un autre artisan")
    void refuseLaDemandeDUnAutre() throws Exception {
        Artisan moi = artisan(true);

        Artisan autre = new Artisan();
        autre.setId(99L);

        Demande sienne = new Demande();
        sienne.setId(42L);
        sienne.setArtisan(autre);
        sienne.setStatut(StatutDemande.NOUVELLE);

        when(artisans.findByCodeGestion(CODE_ARTISAN)).thenReturn(Optional.of(moi));
        when(demandes.findById(42L)).thenReturn(Optional.of(sienne));

        mvc.perform(post("/api/espace/travaux/42/statut").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"" + CODE_ARTISAN + "\",\"statut\":\"ACCEPTEE\"}")))
                .andExpect(status().isNotFound());

        verify(demandes, never()).save(any(Demande.class));
    }

    @Test
    @DisplayName("refuse un état de demande inventé")
    void statutInvente() throws Exception {
        Artisan a = artisan(true);
        Demande d = new Demande();
        d.setId(42L);
        d.setArtisan(a);

        when(artisans.findByCodeGestion(CODE_ARTISAN)).thenReturn(Optional.of(a));
        when(demandes.findById(anyLong())).thenReturn(Optional.of(d));

        mvc.perform(post("/api/espace/travaux/42/statut").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"" + CODE_ARTISAN + "\",\"statut\":\"PAYEE\"}")))
                .andExpect(status().isBadRequest());

        verify(demandes, never()).save(any(Demande.class));
    }

    @Test
    @DisplayName("ouvre aussi l'espace d'un livreur, avec son véhicule et ses tarifs")
    void ouvreEspaceLivreur() throws Exception {
        when(artisans.findByCodeGestion(CODE_LIVREUR)).thenReturn(Optional.empty());
        when(livreurs.findByCodeGestion(CODE_LIVREUR)).thenReturn(Optional.of(livreur()));
        when(courses.findByLivreurIdOrderByCreeLeDesc(5L)).thenReturn(List.of());

        mvc.perform(post("/api/espace").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"code\":\"" + CODE_LIVREUR + "\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("livreur"))
                .andExpect(jsonPath("$.vehicule").value("moto"))
                .andExpect(jsonPath("$.metier").doesNotExist());
    }
}
