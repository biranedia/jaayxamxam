import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, Suivi as SuiviDto } from '../../api.service';
import { Geo, distanceMetres, estEnLigne, formatDistance, minutesTrajet } from '../../geo';
import { Glyphe, teinte } from '../../glyphe';
import { fcfa, initiales, lienWhatsapp, messageErreur, urlPhoto } from '../../utils';

// Les étapes dans l'ordre, pour une demande d'artisan comme pour une course
const ETAPES = [
  { cle: 'NOUVELLE', nom: 'Envoyée' },
  { cle: 'ACCEPTEE', nom: 'Acceptée' },
  { cle: 'EN_ROUTE', nom: 'En route' },
  { cle: 'TERMINEE', nom: 'Terminée' }
];

@Component({
  selector: 'app-suivi',
  imports: [RouterLink, DatePipe, Glyphe],
  templateUrl: './suivi.html'
})
export class Suivi {
  private readonly api = inject(ApiService);
  protected readonly geo = inject(Geo);

  // Prérempli quand on arrive juste après avoir envoyé une demande ou une course
  readonly code = input<string>();

  protected readonly etapes = ETAPES;
  protected readonly saisie = signal('');
  protected readonly resultat = signal<SuiviDto | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly enCours = signal(false);

  protected readonly lienWhatsapp = lienWhatsapp;
  protected readonly urlPhoto = urlPhoto;
  protected readonly formatDistance = formatDistance;
  protected readonly minutesTrajet = minutesTrajet;
  protected readonly estEnLigne = estEnLigne;
  protected readonly initiales = initiales;
  protected readonly fcfa = fcfa;
  protected readonly teinte = teinte;

  // Position de l'étape actuelle dans la suite d'étapes
  protected readonly etapeActuelle = computed(() => {
    const statut = this.resultat()?.statut;
    const index = ETAPES.findIndex(e => e.cle === statut);
    return index === -1 ? 0 : index;
  });

  // Distance entre le visiteur et l'intervenant, quand les deux positions sont connues
  protected readonly distanceIntervenant = computed(() => {
    const moi = this.geo.position();
    const lui = this.resultat()?.intervenant;
    if (!moi || lui?.latitude == null || lui.longitude == null) return null;
    return distanceMetres(moi, { lat: lui.latitude, lon: lui.longitude });
  });

  constructor() {
    effect(() => {
      const c = this.code();
      if (c) {
        this.saisie.set(c);
        this.chercher(c);
      }
    });
  }

  protected soumettre(valeur: string, evenement: Event): void {
    evenement.preventDefault();
    this.saisie.set(valeur);
    this.chercher(valeur);
  }

  protected rafraichir(): void {
    if (this.saisie()) this.chercher(this.saisie());
  }

  private chercher(code: string): void {
    const propre = code.trim().toUpperCase();
    if (!propre) return;
    this.enCours.set(true);
    this.erreur.set(null);
    this.api.suivi(propre).subscribe({
      next: r => { this.resultat.set(r); this.enCours.set(false); },
      error: e => {
        this.resultat.set(null);
        this.erreur.set(e.status === 404 ? 'Aucune demande avec ce code.' : messageErreur(e.status));
        this.enCours.set(false);
      }
    });
  }
}
