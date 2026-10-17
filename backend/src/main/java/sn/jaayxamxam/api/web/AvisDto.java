package sn.jaayxamxam.api.web;

import sn.jaayxamxam.api.domain.Avis;

import java.time.LocalDateTime;

public record AvisDto(int note, String commentaire, LocalDateTime creeLe) {

    public static AvisDto depuis(Avis a) {
        return new AvisDto(a.getNote(), a.getCommentaire(), a.getCreeLe());
    }
}
