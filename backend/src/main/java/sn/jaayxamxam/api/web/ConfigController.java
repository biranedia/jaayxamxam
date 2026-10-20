package sn.jaayxamxam.api.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Les quelques réglages dont l'interface a besoin et qui changent selon l'installation.
 *
 * <p>Pour l'instant un seul : le numéro WhatsApp de la plateforme, vers lequel les travailleurs
 * envoient leur code pour faire confirmer leur numéro. Le mettre ici plutôt que dans le code du
 * frontend évite d'avoir à recompiler l'application pour en changer.
 *
 * <p>Rien de secret ne doit transiter par cet appel : il est public et sans limitation.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final String whatsapp;

    public ConfigController(@Value("${app.whatsapp.verification:}") String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public record ConfigResponse(String whatsappVerification) {
    }

    @GetMapping
    public ConfigResponse config() {
        // Chaîne vide quand rien n'est configuré : l'interface cache alors le bouton
        return new ConfigResponse(whatsapp == null ? "" : whatsapp.trim());
    }
}
