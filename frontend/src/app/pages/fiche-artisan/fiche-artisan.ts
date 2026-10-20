import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, ArtisanDetail } from '../../api.service';
import { Contacts } from '../../contacts';
import { Geo, distanceMetres, estDisponible, formatDistance } from '../../geo';
import { Glyphe, teinte } from '../../glyphe';
import { etoiles, fcfa, initiales, messageErreur, urlPhoto } from '../../utils';

@Component({
  selector: 'app-fiche-artisan',
  imports: [RouterLink, ReactiveFormsModule, DatePipe, DecimalPipe, Glyphe],
  templateUrl: './fiche-artisan.html'
})
export class FicheArtisan {
  private readonly api = inject(ApiService);
  protected readonly contacts = inject(Contacts);
  private readonly fb = inject(NonNullableFormBuilder);

  private readonly geo = inject(Geo);

  // Outils de présentation utilisés par le gabarit
  protected readonly formatDistance = formatDistance;
  protected readonly estDisponible = estDisponible;
  protected readonly initiales = initiales;
  protected readonly etoiles = etoiles;
  protected readonly fcfa = fcfa;
  protected readonly teinte = teinte;

  // Identifiant lu dans l'URL (/artisan/:id)
  readonly id = input.required<string>();

  protected readonly detail = signal<ArtisanDetail | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly avisMessage = signal<string | null>(null);
  protected readonly avisErreur = signal<string | null>(null);
  protected readonly avisEnCours = signal(false);
  protected readonly urlPhoto = urlPhoto;

  // Distance calculée ici : la fiche ne reçoit pas la position du visiteur
  protected readonly distance = computed(() => {
    const moi = this.geo.position();
    const a = this.detail()?.artisan;
    if (!moi || a?.latitude == null || a.longitude == null) return null;
    return distanceMetres(moi, { lat: a.latitude, lon: a.longitude });
  });

  protected readonly formAvis = this.fb.group({
    codeSuivi: ['', Validators.required],
    note: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    commentaire: ['', Validators.maxLength(1000)]
  });

  constructor() {
    // Recharge la fiche quand l'identifiant change
    effect(() => this.charger(Number(this.id())));
  }

  protected envoyerAvis(): void {
    if (this.formAvis.invalid) {
      this.formAvis.markAllAsTouched();
      return;
    }
    const v = this.formAvis.getRawValue();
    this.avisEnCours.set(true);
    this.avisErreur.set(null);
    this.avisMessage.set(null);
    this.api.creerAvis({
      codeSuivi: v.codeSuivi.trim(),
      note: v.note,
      commentaire: v.commentaire.trim() || undefined
    }).subscribe({
      next: () => {
        this.avisMessage.set('Merci, votre avis a été publié.');
        this.formAvis.reset({ codeSuivi: '', note: 5, commentaire: '' });
        this.avisEnCours.set(false);
        this.charger(Number(this.id()));
      },
      error: e => {
        this.avisErreur.set(messageErreur(e.status));
        this.avisEnCours.set(false);
      }
    });
  }

  private charger(id: number): void {
    this.erreur.set(null);
    this.api.artisan(id).subscribe({
      next: d => this.detail.set(d),
      error: e => this.erreur.set(e.status === 404 ? 'Artisan introuvable.' : messageErreur(e.status))
    });
  }
}
