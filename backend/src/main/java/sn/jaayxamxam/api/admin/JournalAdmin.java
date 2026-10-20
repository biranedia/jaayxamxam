package sn.jaayxamxam.api.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Journal des actions d'administration.
 *
 * <p>Pourquoi cette table existe : tu valides l'identité de personnes réelles et tu rends leurs
 * numéros visibles. Le jour où quelqu'un conteste — « je n'ai jamais été refusé », « qui a masqué
 * mon avis » —, seule une trace horodatée répond. Elle te protège autant qu'elle protège eux.
 *
 * <p>Les connexions refusées y figurent aussi : une série de refus à trois heures du matin est le
 * seul signe que quelqu'un essaie d'entrer.
 *
 * <p>L'écriture se fait dans sa propre transaction ({@code REQUIRES_NEW}) : si l'action échoue et
 * que sa transaction est annulée, la trace de la tentative doit rester. Et un échec d'écriture du
 * journal ne doit jamais faire échouer l'action elle-même — d'où le filet autour.
 */
@Component
public class JournalAdmin {

    private static final Logger LOG = LoggerFactory.getLogger(JournalAdmin.class);

    private final ActionAdminRepository actions;

    public JournalAdmin(ActionAdminRepository actions) {
        this.actions = actions;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String adresse, String action, String cible, String detail) {
        try {
            ActionAdmin ligne = new ActionAdmin();
            ligne.setAdresse(tronquer(adresse, 64));
            ligne.setAction(tronquer(action, 48));
            ligne.setCible(tronquer(cible, 64));
            ligne.setDetail(tronquer(detail, 255));
            ligne.setFaiteLe(LocalDateTime.now());
            actions.save(ligne);
        } catch (RuntimeException e) {
            // Le journal est une sécurité, pas une dépendance : on ne casse pas l'action pour lui
            LOG.error("Échec d'écriture du journal d'administration ({})", action, e);
        }
    }

    @Transactional(readOnly = true)
    public List<ActionAdmin> dernieres() {
        return actions.findTop200ByOrderByFaiteLeDesc();
    }

    private static String tronquer(String texte, int maximum) {
        if (texte == null) return null;
        return texte.length() <= maximum ? texte : texte.substring(0, maximum);
    }
}
