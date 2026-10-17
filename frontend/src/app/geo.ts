import { Injectable, signal } from '@angular/core';

export interface Point { lat: number; lon: number; }

export type EtatGeo = 'inconnue' | 'recherche' | 'ok' | 'refusee' | 'indisponible';

// Distance à vol d'oiseau entre deux points, en mètres (formule de haversine)
export function distanceMetres(a: Point, b: Point): number {
  const R = 6371000;
  const rad = (d: number) => (d * Math.PI) / 180;
  const dLat = rad(b.lat - a.lat);
  const dLon = rad(b.lon - a.lon);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLon / 2) ** 2;
  return Math.round(2 * R * Math.asin(Math.sqrt(h)));
}

// « 850 m » en dessous d'un kilomètre, « 2,4 km » au-dessus
export function formatDistance(metres: number): string {
  if (metres < 1000) return `${Math.round(metres / 10) * 10} m`;
  return `${(metres / 1000).toFixed(1).replace('.', ',')} km`;
}

// Un travailleur est « en ligne » si son téléphone a envoyé sa position récemment
export function estEnLigne(derniereActivite?: string, minutes = 10): boolean {
  if (!derniereActivite) return false;
  const ecart = Date.now() - new Date(derniereActivite).getTime();
  return ecart >= 0 && ecart < minutes * 60_000;
}

// Durée de trajet approximative en ville : 18 km/h de moyenne en moto à Dakar
export function minutesTrajet(metres: number): number {
  return Math.max(3, Math.round(metres / 300));
}

@Injectable({ providedIn: 'root' })
export class Geo {
  readonly position = signal<Point | null>(null);
  readonly etat = signal<EtatGeo>('inconnue');

  private surveillance?: number;

  // Demande la position une fois ; le navigateur affiche sa propre autorisation
  demander(): void {
    if (!navigator.geolocation) {
      this.etat.set('indisponible');
      return;
    }
    this.etat.set('recherche');
    navigator.geolocation.getCurrentPosition(
      p => {
        this.position.set({ lat: p.coords.latitude, lon: p.coords.longitude });
        this.etat.set('ok');
      },
      e => this.etat.set(e.code === e.PERMISSION_DENIED ? 'refusee' : 'indisponible'),
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 60_000 }
    );
  }

  // Suivi continu : utilisé par le travailleur qui se met « disponible »
  suivre(auChangement: (p: Point) => void): void {
    if (!navigator.geolocation || this.surveillance !== undefined) return;
    this.surveillance = navigator.geolocation.watchPosition(
      p => {
        const point = { lat: p.coords.latitude, lon: p.coords.longitude };
        this.position.set(point);
        this.etat.set('ok');
        auChangement(point);
      },
      () => this.etat.set('indisponible'),
      { enableHighAccuracy: true, maximumAge: 30_000 }
    );
  }

  arreterSuivi(): void {
    if (this.surveillance !== undefined) {
      navigator.geolocation.clearWatch(this.surveillance);
      this.surveillance = undefined;
    }
  }
}
