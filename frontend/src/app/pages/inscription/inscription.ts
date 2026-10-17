import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { ApiService, InscriptionReponse, Metier, Zone } from '../../api.service';
import { FAMILLES, Glyphe, teinte } from '../../glyphe';
import { fcfa, messageErreur, telephoneValide } from '../../utils';

// Les mêmes véhicules que dans Yonni la, avec leur charge habituelle
const VEHICULES = [
  { cle: 'moto', nom: 'Moto', charge: 30 },
  { cle: 'tricycle', nom: 'Tricycle', charge: 250 },
  { cle: 'voiture', nom: 'Voiture', charge: 150 },
  { cle: 'camionnette', nom: 'Camionnette', charge: 1200 }
];

type TypeTravailleur = 'artisan' | 'livreur';

@Component({
  selector: 'app-inscription',
  imports: [ReactiveFormsModule, RouterLink, Glyphe],
  templateUrl: './inscription.html'
})
export class Inscription {
  private readonly api = inject(ApiService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly familles = FAMILLES;
  protected readonly vehicules = VEHICULES;
  protected readonly metiers = toSignal(this.api.metiers(), { initialValue: [] as Metier[] });
  protected readonly zones = toSignal(this.api.zones(), { initialValue: [] as Zone[] });

  // Rien n'est choisi au départ : l'écran demande d'abord « vous faites quoi ? »
  protected readonly choix = signal<TypeTravailleur | null>(null);
  protected readonly reponse = signal<InscriptionReponse | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly enCours = signal(false);

  protected readonly fcfa = fcfa;
  protected readonly teinte = teinte;

  // Deuxième temps de l'inscription : la photo, une fois le code obtenu
  protected readonly photoApercu = signal<string | null>(null);
  protected readonly photoEnvoyee = signal(false);
  protected readonly photoErreur = signal<string | null>(null);
  protected readonly photoEnCours = signal(false);
  private photoFichier: File | null = null;

  protected readonly formArtisan = this.fb.group({
    nom: ['', [Validators.required, Validators.maxLength(120)]],
    telephone: ['', [Validators.required, telephoneValide]],
    metierId: ['', Validators.required],
    zoneId: ['', Validators.required],
    anneesExperience: [1, [Validators.required, Validators.min(0), Validators.max(70)]],
    tarifIndicatif: ['', Validators.min(0)],
    description: ['', Validators.maxLength(500)]
  });

  protected readonly formLivreur = this.fb.group({
    nom: ['', [Validators.required, Validators.maxLength(120)]],
    telephone: ['', [Validators.required, telephoneValide]],
    vehicule: ['moto', Validators.required],
    chargeMaxKg: [30, [Validators.required, Validators.min(1), Validators.max(5000)]],
    prixParKm: ['', Validators.min(0)],
    prixMinimum: ['', Validators.min(0)]
  });

  /**
   * Contrôle rapide avant l'envoi : le serveur refait tout, mais autant le dire tout de suite
   * plutôt que de faire monter cinq mégaoctets en 3G pour rien.
   */
  protected choisirPhoto(evenement: Event): void {
    const fichier = (evenement.target as HTMLInputElement).files?.[0] ?? null;
    this.photoErreur.set(null);
    this.libererApercu();

    if (!fichier) {
      this.photoFichier = null;
      return;
    }
    if (fichier.type !== 'image/jpeg' && fichier.type !== 'image/png') {
      this.photoErreur.set('Choisissez une photo JPEG ou PNG.');
      this.photoFichier = null;
      return;
    }
    if (fichier.size > 5 * 1024 * 1024) {
      this.photoErreur.set('Photo trop lourde (5 Mo maximum). Prenez-la en qualité moyenne.');
      this.photoFichier = null;
      return;
    }
    this.photoFichier = fichier;
    this.photoApercu.set(URL.createObjectURL(fichier));
  }

  protected envoyerPhoto(): void {
    const code = this.reponse()?.codeGestion;
    const type = this.choix();
    if (!this.photoFichier || !code || !type) return;

    this.photoEnCours.set(true);
    this.photoErreur.set(null);
    this.api.envoyerPhoto(type, code, this.photoFichier).subscribe({
      next: () => {
        this.photoEnvoyee.set(true);
        this.photoEnCours.set(false);
        this.libererApercu();
      },
      error: e => {
        this.photoErreur.set(e.error?.message ?? messageErreur(e.status));
        this.photoEnCours.set(false);
      }
    });
  }

  private libererApercu(): void {
    const ancien = this.photoApercu();
    if (ancien) URL.revokeObjectURL(ancien);
    this.photoApercu.set(null);
  }

  protected choisir(type: TypeTravailleur): void {
    this.choix.set(type);
    this.erreur.set(null);
  }

  protected revenir(): void {
    this.choix.set(null);
    this.erreur.set(null);
  }

  // Changer de véhicule propose la charge habituelle de ce véhicule
  protected choisirVehicule(cle: string): void {
    this.formLivreur.controls.vehicule.setValue(cle);
    const v = VEHICULES.find(x => x.cle === cle);
    if (v) this.formLivreur.controls.chargeMaxKg.setValue(v.charge);
  }

  protected envoyerArtisan(): void {
    if (this.formArtisan.invalid) {
      this.formArtisan.markAllAsTouched();
      return;
    }
    const v = this.formArtisan.getRawValue();
    this.envoyer(this.api.inscrireArtisan({
      nom: v.nom.trim(),
      telephone: v.telephone,
      metierId: Number(v.metierId),
      zoneId: Number(v.zoneId),
      anneesExperience: Number(v.anneesExperience),
      tarifIndicatif: v.tarifIndicatif ? Number(v.tarifIndicatif) : undefined,
      description: v.description.trim() || undefined
    }));
  }

  protected envoyerLivreur(): void {
    if (this.formLivreur.invalid) {
      this.formLivreur.markAllAsTouched();
      return;
    }
    const v = this.formLivreur.getRawValue();
    this.envoyer(this.api.inscrireLivreur({
      nom: v.nom.trim(),
      telephone: v.telephone,
      vehicule: v.vehicule,
      chargeMaxKg: Number(v.chargeMaxKg),
      prixParKm: v.prixParKm ? Number(v.prixParKm) : undefined,
      prixMinimum: v.prixMinimum ? Number(v.prixMinimum) : undefined
    }));
  }

  private envoyer(appel: Observable<InscriptionReponse>): void {
    this.enCours.set(true);
    this.erreur.set(null);
    appel.subscribe({
      next: r => { this.reponse.set(r); this.enCours.set(false); },
      error: e => {
        this.erreur.set(e.status === 409
          ? 'Ce numéro est déjà inscrit. Appelez-nous si ce profil est le vôtre.'
          : messageErreur(e.status));
        this.enCours.set(false);
      }
    });
  }
}
