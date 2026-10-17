package sn.jaayxamxam.api.photo;

/** Le fichier envoyé n'est pas une photo exploitable. Le message est montré à l'utilisateur. */
public class PhotoRefusee extends RuntimeException {

    public PhotoRefusee(String message) {
        super(message);
    }
}
