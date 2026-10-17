package sn.jaayxamxam.api.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

// Branche le dossier configuré sur la classe Photos, qui reste utilisable sans Spring
@Configuration
public class PhotoConfig {

    @Bean
    public Photos photos(@Value("${app.photos.dossier:./donnees/photos}") String dossier) throws IOException {
        return new Photos(Path.of(dossier));
    }
}
