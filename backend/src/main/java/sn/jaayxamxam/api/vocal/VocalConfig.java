package sn.jaayxamxam.api.vocal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

// Branche le dossier configuré sur la classe Vocaux, qui reste utilisable sans Spring
@Configuration
public class VocalConfig {

    @Bean
    public Vocaux vocaux(@Value("${app.vocaux.dossier:./donnees/vocaux}") String dossier) throws IOException {
        return new Vocaux(Path.of(dossier));
    }
}
