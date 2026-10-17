package sn.jaayxamxam.api.web;

import sn.jaayxamxam.api.domain.Livreur;

import java.time.LocalDateTime;

// Pas de téléphone ici non plus : il passe par /api/contacts
public record LivreurDto(
        Long id,
        String nom,
        String vehicule,
        int chargeMaxKg,
        int prixParKm,
        int prixMinimum,
        boolean verifie,
        String photo,
        Double latitude,        // arrondie à ~110 m (voir Positions)
        Double longitude,
        LocalDateTime derniereActivite,
        Integer distanceM,
        Double noteMoyenne,
        long nombreAvis) {

    public static LivreurDto depuis(Livreur l, Integer distanceM, Double noteMoyenne, long nombreAvis) {
        return new LivreurDto(
                l.getId(), l.getNom(), l.getVehicule(),
                l.getChargeMaxKg(), l.getPrixParKm(), l.getPrixMinimum(), l.isVerifie(), l.getPhoto(),
                Positions.flouter(l.getLatitude()), Positions.flouter(l.getLongitude()),
                l.getDerniereActivite(),
                distanceM, noteMoyenne, nombreAvis);
    }
}
