package sn.jaayxamxam.api.web;

// Calculs de distance, identiques à ceux du frontend
public final class Geo {

    private static final double RAYON_TERRE_M = 6_371_000;

    // Les rues ne sont pas droites : on majore la distance à vol d'oiseau de 30 %
    private static final double DETOUR = 1.3;

    private Geo() {
    }

    // Distance à vol d'oiseau en mètres (haversine), ou null si une coordonnée manque
    public static Integer distance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return (int) Math.round(2 * RAYON_TERRE_M * Math.asin(Math.sqrt(h)));
    }

    // Distance routière estimée entre deux quartiers
    public static int distanceRoute(double lat1, double lon1, double lat2, double lon2) {
        Integer volOiseau = distance(lat1, lon1, lat2, lon2);
        return (int) Math.round(volOiseau * DETOUR);
    }

    // Prix arrondi à la centaine de francs, jamais en dessous du minimum du livreur
    public static int prix(int metres, int prixParKm, int prixMinimum) {
        int calcule = (int) Math.round(metres / 1000.0 * prixParKm / 100) * 100;
        return Math.max(prixMinimum, calcule);
    }
}
