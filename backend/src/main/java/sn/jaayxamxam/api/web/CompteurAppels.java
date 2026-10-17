package sn.jaayxamxam.api.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compte les appels par clé (une adresse IP) sur une fenêtre de temps glissante.
 *
 * <p>Volontairement simple : tout est en mémoire. Cela protège d'un visiteur isolé qui spamme ou qui
 * essaie des codes de suivi au hasard, pas d'une attaque répartie sur beaucoup de machines, et le
 * compteur repart à zéro à chaque redémarrage. Avec plusieurs instances, chacune a le sien : il
 * faudra passer par Redis le jour où l'on tourne à plus d'un serveur.
 */
public class CompteurAppels {

    private static final int MENAGE_TOUS_LES = 1000;

    private static final class Fenetre {
        long debutMs;
        int nombre;
    }

    private final Map<String, Fenetre> fenetres = new ConcurrentHashMap<>();
    private int appelsDepuisMenage;

    /** Renvoie vrai si l'appel est autorisé, et le compte. */
    public synchronized boolean autoriser(String cle, int maximum, long fenetreMs, long maintenantMs) {
        menageSiNecessaire(fenetreMs, maintenantMs);

        // La fenetre demarre a l'instant du premier appel. Sans cela elle partirait de
        // l'instant zero (1970), et le premier appel suivant serait vu comme hors fenetre.
        Fenetre f = fenetres.computeIfAbsent(cle, k -> {
            Fenetre nouvelle = new Fenetre();
            nouvelle.debutMs = maintenantMs;
            return nouvelle;
        });
        if (maintenantMs - f.debutMs >= fenetreMs) {
            f.debutMs = maintenantMs;
            f.nombre = 0;
        }
        if (f.nombre >= maximum) return false;
        f.nombre++;
        return true;
    }

    /** Nombre d'appels déjà comptés pour cette clé dans la fenêtre en cours. */
    public synchronized int compte(String cle, long fenetreMs, long maintenantMs) {
        Fenetre f = fenetres.get(cle);
        if (f == null || maintenantMs - f.debutMs >= fenetreMs) return 0;
        return f.nombre;
    }

    // Sans ménage, la carte grossirait indéfiniment avec les adresses vues une seule fois
    private void menageSiNecessaire(long fenetreMs, long maintenantMs) {
        if (++appelsDepuisMenage < MENAGE_TOUS_LES) return;
        appelsDepuisMenage = 0;
        fenetres.entrySet().removeIf(e -> maintenantMs - e.getValue().debutMs >= fenetreMs * 2);
    }
}
