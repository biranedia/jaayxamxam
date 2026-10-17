import { Component, effect, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, DemandeReponse, Metier, Zone } from '../../api.service';
import { messageErreur, telephoneValide } from '../../utils';

@Component({
  selector: 'app-demande',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './demande.html'
})
export class FormulaireDemande {
  private readonly api = inject(ApiService);
  private readonly fb = inject(NonNullableFormBuilder);

  // Préremplis depuis l'URL quand on arrive depuis la fiche d'un artisan
  readonly artisanId = input<string>();
  readonly metierId = input<string>();
  readonly zoneId = input<string>();

  protected readonly metiers = signal<Metier[]>([]);
  protected readonly zones = signal<Zone[]>([]);
  protected readonly reponse = signal<DemandeReponse | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly enCours = signal(false);

  protected readonly form = this.fb.group({
    clientNom: ['', [Validators.required, Validators.maxLength(120)]],
    clientTelephone: ['', [Validators.required, telephoneValide]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    urgente: [false],
    metierId: ['', Validators.required],
    zoneId: ['', Validators.required]
  });

  constructor() {
    this.api.metiers().subscribe({ next: m => this.metiers.set(m), error: e => this.erreur.set(messageErreur(e.status)) });
    this.api.zones().subscribe({ next: z => this.zones.set(z), error: e => this.erreur.set(messageErreur(e.status)) });

    effect(() => {
      const metier = this.metierId();
      const zone = this.zoneId();
      if (metier) this.form.controls.metierId.setValue(metier);
      if (zone) this.form.controls.zoneId.setValue(zone);
    });
  }

  protected envoyer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const artisan = this.artisanId();
    this.enCours.set(true);
    this.erreur.set(null);
    this.api.creerDemande({
      clientNom: v.clientNom.trim(),
      clientTelephone: v.clientTelephone,
      description: v.description.trim(),
      urgente: v.urgente,
      metierId: Number(v.metierId),
      zoneId: Number(v.zoneId),
      artisanId: artisan ? Number(artisan) : undefined
    }).subscribe({
      next: r => {
        this.reponse.set(r);
        this.enCours.set(false);
      },
      error: e => {
        this.erreur.set(messageErreur(e.status));
        this.enCours.set(false);
      }
    });
  }
}
