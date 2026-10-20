package sn.jaayxamxam.api.admin;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessions d'administration, gardées en mémoire du serveur.
 *
 * <p>Le jeton remis au navigateur est tiré au hasard sur 256 bits : il ne contient aucune
 * information et ne se devine pas. C'est le serveur qui décide de sa validité, donc la
 * déconnexion est <em>réelle</em> — contrairement à un jeton signé de type JWT, qu'on ne peut pas
 * révoquer avant son expiration sans tenir malgré tout une liste côté serveur.
 *
 * <p>Deux durées, volontairement :
 * <ul>
 *   <li><strong>8 heures au maximum</strong> depuis la connexion, quoi qu'il arrive. Une journée de
 *       travail. Passé ce délai il faut retaper le mot de passe.
 *   <li><strong>45 minutes d'inactivité.</strong> Un navigateur laissé ouvert dans un cybercafé ne
 *       reste pas une porte d'entrée toute la journée.
 * </ul>
 *
 * <p>En mémoire : tout redémarrage déconnecte. C'est acceptable pour une personne seule, et cela
 * évite de stocker des jetons en base. Le jour où le serveur tournera en plusieurs instances, il
 * faudra passer par Redis — c'est la même limite que pour la limitation de débit.
 *
 * <p>Ce qui est conservé n'est pas le jeton mais son empreinte SHA-256 : si un vidage mémoire ou
 * une trace venait à fuiter, elle ne contiendrait aucun jeton réutilisable.
 */
@Component
public class Sessions {

    private static final Duration DUREE_MAXIMALE = Duration.ofHours(8);
    private static final Duration INACTIVITE_MAXIMALE = Duration.ofMinutes(45);
    private static final int OCTETS_JETON = 32;              // 256 bits
    private static final int MENAGE_TOUS_LES = 50;

    private static final SecureRandom ALEA = new SecureRandom();

    private static final class Session {
        final Instant debut;
        volatile Instant derniereUtilisation;
        final String adresse;

        Session(Instant maintenant, String adresse) {
            this.debut = maintenant;
            this.derniereUtilisation = maintenant;
            this.adresse = adresse;
        }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private int appelsDepuisMenage;

    /** Ouvre une session et renvoie le jeton à remettre au navigateur. Il n'est donné qu'ici. */
    public String ouvrir(String adresse, Instant maintenant) {
        byte[] brut = new byte[OCTETS_JETON];
        ALEA.nextBytes(brut);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(brut);
        sessions.put(empreinte(jeton), new Session(maintenant, adresse));
        menageSiNecessaire(maintenant);
        return jeton;
    }

    /**
     * Valide un jeton et repousse l'échéance d'inactivité.
     *
     * <p>Renvoie l'adresse d'ouverture quand la session est valable, vide sinon. Une session
     * expirée est supprimée au passage plutôt que laissée à traîner.
     */
    public Optional<String> valider(String jeton, Instant maintenant) {
        if (jeton == null || jeton.isBlank()) return Optional.empty();

        String cle = empreinte(jeton);
        Session s = sessions.get(cle);
        if (s == null) return Optional.empty();

        if (maintenant.isAfter(s.debut.plus(DUREE_MAXIMALE))
                || maintenant.isAfter(s.derniereUtilisation.plus(INACTIVITE_MAXIMALE))) {
            sessions.remove(cle);
            return Optional.empty();
        }

        s.derniereUtilisation = maintenant;
        return Optional.of(s.adresse);
    }

    /** Ferme une session. Après cet appel, le jeton ne vaut plus rien, immédiatement. */
    public void fermer(String jeton) {
        if (jeton != null && !jeton.isBlank()) sessions.remove(empreinte(jeton));
    }

    /** Ferme toutes les sessions : utile si l'on soupçonne une fuite. */
    public void fermerToutes() {
        sessions.clear();
    }

    /** Nombre de sessions encore ouvertes, affiché dans le back-office. */
    public int ouvertes() {
        return sessions.size();
    }

    /** Durée d'inactivité tolérée, pour informer l'interface. */
    public static Duration inactiviteMaximale() {
        return INACTIVITE_MAXIMALE;
    }

    /**
     * On range l'empreinte du jeton, jamais le jeton lui-même. SHA-256 sans sel suffit ici :
     * le jeton fait déjà 256 bits aléatoires, il n'y a rien à deviner par dictionnaire.
     */
    private static String empreinte(String jeton) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(MotDePasse.octets(jeton));
            return Base64.getEncoder().encodeToString(h);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    // Sans ménage, les sessions expirées s'accumuleraient indéfiniment en mémoire
    private synchronized void menageSiNecessaire(Instant maintenant) {
        if (++appelsDepuisMenage < MENAGE_TOUS_LES) return;
        appelsDepuisMenage = 0;
        sessions.values().removeIf(s ->
                maintenant.isAfter(s.debut.plus(DUREE_MAXIMALE))
                        || maintenant.isAfter(s.derniereUtilisation.plus(INACTIVITE_MAXIMALE)));
    }
}
