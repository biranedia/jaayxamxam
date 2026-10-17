import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, Livreur } from '../../api.service';
import { Contacts } from '../../contacts';
import { Geo, estEnLigne, formatDistance, minutesTrajet } from '../../geo';
import { Glyphe, teinte } from '../../glyphe';
import { etoiles, fcfa, initiales, urlPhoto } from '../../utils';

// Les quatre types de véhicule proposés, du plus léger au plus lourd
const VEHICULES = [
  { cle: 'moto', nom: 'Moto', detail: 'Colis, plis, repas' },
  { cle: 'tricycle', nom: 'Tricycle', detail: 'Courses, petits meubles' },
  { cle: 'voiture', nom: 'Voiture', detail: 'Bagages, personnes' },
  { cle: 'camionnette', nom: 'Camionnette', detail: 'Déménagement, matériaux' }
];

@Component({
  selector: 'app-yonni',
  imports: [RouterLink, Glyphe],
  templateUrl: './yonni.html'
})
export class Yonni {
  private readonly api = inject(ApiService);
  protected readonly contacts = inject(Contacts);
  protected readonly geo = inject(Geo);

  protected readonly vehicules = VEHICULES;
  protected readonly vehicule = signal<string | undefined>(undefined);
  protected readonly livreurs = signal<Livreur[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);

  protected readonly urlPhoto = urlPhoto;
  protected readonly formatDistance = formatDistance;
  protected readonly minutesTrajet = minutesTrajet;
  protected readonly estEnLigne = estEnLigne;
  protected readonly initiales = initiales;
  protected readonly etoiles = etoiles;
  protected readonly fcfa = fcfa;
  protected readonly teinte = teinte;

  // Quatre cartes grises pendant le chargement : @for a besoin d'un tableau
  protected readonly squelettes = [0, 1, 2, 3];

  protected readonly resultats = computed(() => {
    const liste = [...this.livreurs()];
    if (this.geo.position()) {
      liste.sort((a, b) => (a.distanceM ?? Infinity) - (b.distanceM ?? Infinity));
    }
    return liste;
  });

  protected readonly nombreEnLigne = computed(
    () => this.resultats().filter(l => estEnLigne(l.derniereActivite)).length
  );

  constructor() {
    effect(() => {
      const v = this.vehicule();
      const ou = this.geo.position();
      this.chargement.set(true);
      this.erreur.set(null);
      this.api.livreurs(v, ou).subscribe({
        next: liste => { this.livreurs.set(liste); this.chargement.set(false); },
        error: () => {
          this.chargement.set(false);
          this.erreur.set("Impossible de joindre le serveur. Vérifiez que l'API est démarrée.");
        }
      });
    });
  }

  protected choisirVehicule(cle?: string): void {
    this.vehicule.set(this.vehicule() === cle ? undefined : cle);
  }
}
