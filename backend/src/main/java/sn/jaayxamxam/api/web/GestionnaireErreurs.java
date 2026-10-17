package sn.jaayxamxam.api.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import sn.jaayxamxam.api.photo.PhotoRefusee;

import java.util.Map;

/**
 * Réponses d'erreur uniformes : toujours {"message": "..."} et jamais de pile d'appels.
 *
 * <p>Une pile d'appels renvoyée au navigateur dit à un attaquant quelles bibliothèques tournent et
 * dans quelles versions. Les détails vont dans les journaux du serveur, le visiteur reçoit une
 * phrase utilisable.
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger JOURNAL = LoggerFactory.getLogger(GestionnaireErreurs.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> statut(ResponseStatusException e) {
        String message = e.getReason() == null ? "Requête refusée." : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", message));
    }

    @ExceptionHandler(PhotoRefusee.class)
    public ResponseEntity<Map<String, String>> photo(PhotoRefusee e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        // Le détail champ par champ reste côté serveur : le formulaire sait déjà quoi dire
        JOURNAL.debug("Formulaire invalide : {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", "Les informations saisies sont invalides."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> fichierTropGros(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "Photo trop lourde. Prenez-la en qualité moyenne."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> imprevu(Exception e) {
        JOURNAL.error("Erreur inattendue", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Une erreur est survenue. Réessayez dans un moment."));
    }
}
