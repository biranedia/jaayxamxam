package sn.jaayxamxam.api.web;

public final class Telephone {

    private Telephone() {
    }

    // Retourne le numéro à 9 chiffres (sans +221, espaces ni tirets), ou null s'il est invalide
    public static String normaliser(String brut) {
        if (brut == null) {
            return null;
        }
        String chiffres = brut.replaceAll("\\D", "");
        if (chiffres.length() == 12 && chiffres.startsWith("221")) {
            chiffres = chiffres.substring(3);
        }
        return chiffres.matches("\\d{9}") ? chiffres : null;
    }
}
