package sn.jaayxamxam.api.vocal;

/**
 * Enregistrement refusé pour une raison que l'on peut dire au client.
 *
 * <p>Le message est montré tel quel, donc il reste compréhensible et ne révèle rien du serveur.
 */
public class VocalRefuse extends RuntimeException {

    public VocalRefuse(String message) {
        super(message);
    }
}
