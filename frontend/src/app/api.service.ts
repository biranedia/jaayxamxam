import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Point } from './geo';

export interface Metier { id: number; nom: string; icone?: string; famille: string; }
export interface Zone { id: number; ville: string; quartier: string; latitude?: number; longitude?: number; }

export interface Artisan {
  id: number;
  nom: string;
  // Pas de téléphone ici : il se demande profil par profil (voir Contacts)
  description?: string;
  anneesExperience: number;
  tarifIndicatif?: number;
  verifie: boolean;
  metierId: number;
  metier: string;
  metierIcone?: string;
  metierFamille?: string;
  photo?: string;          // nom du fichier, à afficher via /api/photos/{nom}
  zoneId: number;
  ville: string;
  quartier: string;
  // Position et présence
  latitude?: number;
  longitude?: number;
  derniereActivite?: string;   // horodatage du dernier envoi de position
  distanceM?: number;          // calculé par le serveur quand lat/lon sont fournis
  noteMoyenne?: number | null;
  nombreAvis?: number;
}

export interface Livreur {
  id: number;
  nom: string;
  vehicule: 'moto' | 'voiture' | 'camionnette' | 'tricycle';
  chargeMaxKg: number;
  photo?: string;
  prixParKm: number;          // FCFA
  prixMinimum: number;        // FCFA
  verifie: boolean;
  latitude?: number;
  longitude?: number;
  derniereActivite?: string;
  distanceM?: number;
  noteMoyenne?: number | null;
  nombreAvis?: number;
}

export interface Avis { note: number; commentaire?: string; creeLe: string; }

export interface ArtisanDetail {
  artisan: Artisan;
  noteMoyenne: number | null;  // null tant qu'il n'y a aucun avis
  nombreAvis: number;
  avis: Avis[];
}

export interface DemandeRequete {
  clientNom: string;
  clientTelephone: string;
  description: string;
  urgente: boolean;
  metierId: number;
  zoneId: number;
  artisanId?: number;
  latitude?: number;
  longitude?: number;
}

export interface DemandeReponse { id: number; codeSuivi: string; statut: string; }
export interface AvisRequete { codeSuivi: string; note: number; commentaire?: string; }

export interface CourseRequete {
  clientNom: string;
  clientTelephone: string;
  vehicule: string;
  departZoneId: number;
  destinationZoneId: number;
  precision?: string;          // « en face de la pharmacie », repère donné par le client
  description?: string;        // ce qu'il y a à transporter
  livreurId?: number;
}

export interface CourseReponse {
  id: number;
  codeSuivi: string;
  statut: string;
  prixEstime: number;
  distanceM: number;
}

// Ce que le client voit avec son code de suivi, pour une demande comme pour une course
export interface Intervenant {
  nom: string;
  telephone: string;
  cle: string;                 // icône du métier, ou véhicule : donne le dessin
  famille: string;             // famille du métier, ou véhicule : donne la couleur
  photo?: string;
  latitude?: number;
  longitude?: number;
  derniereActivite?: string;
}

export interface Suivi {
  type: 'demande' | 'course';
  codeSuivi: string;
  statut: string;
  creeLe: string;
  resume: string;
  prixEstime?: number;
  intervenant?: Intervenant;
}

// Ce qu'un travailleur remplit pour proposer ses services
export interface InscriptionArtisan {
  nom: string;
  telephone: string;
  metierId: number;
  zoneId: number;
  description?: string;
  anneesExperience: number;
  tarifIndicatif?: number;
}

export interface InscriptionLivreur {
  nom: string;
  telephone: string;
  vehicule: string;
  chargeMaxKg: number;
  photo?: string;
  prixParKm?: number;
  prixMinimum?: number;
}

// Le profil attend d'être validé ; le code de gestion est à conserver
export interface InscriptionReponse { id: number; codeGestion: string; statut: string; }

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  metiers(): Observable<Metier[]> {
    return this.http.get<Metier[]>('/api/metiers');
  }

  zones(): Observable<Zone[]> {
    return this.http.get<Zone[]>('/api/zones');
  }

  // Tous les filtres sont facultatifs ; avec une position, le serveur renvoie la distance
  artisans(metierId?: number, zoneId?: number, ou?: Point | null): Observable<Artisan[]> {
    return this.http.get<Artisan[]>('/api/artisans', { params: this.filtres(metierId, zoneId, ou) });
  }

  artisan(id: number): Observable<ArtisanDetail> {
    return this.http.get<ArtisanDetail>(`/api/artisans/${id}`);
  }

  livreurs(vehicule?: string, ou?: Point | null): Observable<Livreur[]> {
    let params = new HttpParams();
    if (vehicule) params = params.set('vehicule', vehicule);
    if (ou) params = params.set('lat', ou.lat).set('lon', ou.lon);
    return this.http.get<Livreur[]>('/api/livreurs', { params });
  }

  creerDemande(demande: DemandeRequete): Observable<DemandeReponse> {
    return this.http.post<DemandeReponse>('/api/demandes', demande);
  }

  creerAvis(avis: AvisRequete): Observable<Avis> {
    return this.http.post<Avis>('/api/avis', avis);
  }

  creerCourse(course: CourseRequete): Observable<CourseReponse> {
    return this.http.post<CourseReponse>('/api/courses', course);
  }

  inscrireArtisan(inscription: InscriptionArtisan): Observable<InscriptionReponse> {
    return this.http.post<InscriptionReponse>('/api/inscriptions/artisan', inscription);
  }

  inscrireLivreur(inscription: InscriptionLivreur): Observable<InscriptionReponse> {
    return this.http.post<InscriptionReponse>('/api/inscriptions/livreur', inscription);
  }

  // Un numéro à la fois : le serveur compte et journalise ces appels
  contact(type: 'artisan' | 'livreur', id: number): Observable<{ telephone: string }> {
    return this.http.get<{ telephone: string }>(`/api/contacts/${type}/${id}`);
  }

  envoyerPhoto(type: 'artisan' | 'livreur', code: string, fichier: File): Observable<{ photo: string }> {
    const corps = new FormData();
    corps.append('code', code);
    corps.append('fichier', fichier);
    return this.http.post<{ photo: string }>(`/api/photos/${type}`, corps);
  }

  suivi(code: string): Observable<Suivi> {
    return this.http.get<Suivi>(`/api/suivi/${encodeURIComponent(code)}`);
  }

  private filtres(metierId?: number, zoneId?: number, ou?: Point | null): HttpParams {
    let params = new HttpParams();
    if (metierId) params = params.set('metierId', metierId);
    if (zoneId) params = params.set('zoneId', zoneId);
    if (ou) params = params.set('lat', ou.lat).set('lon', ou.lon);
    return params;
  }
}
