import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { ApiService, CourseReponse, Zone } from '../../api.service';
import { distanceMetres, formatDistance, minutesTrajet } from '../../geo';
import { Glyphe } from '../../glyphe';
import { fcfa, messageErreur, telephoneValide } from '../../utils';

// Tarifs indicatifs par véhicule ; le serveur applique ceux du livreur choisi
const TARIFS: Record<string, { parKm: number; minimum: number; nom: string }> = {
  moto: { parKm: 300, minimum: 1000, nom: 'Moto' },
  tricycle: { parKm: 400, minimum: 1500, nom: 'Tricycle' },
  voiture: { parKm: 500, minimum: 2000, nom: 'Voiture' },
  camionnette: { parKm: 800, minimum: 5000, nom: 'Camionnette' }
};

@Component({
  selector: 'app-course',
  imports: [ReactiveFormsModule, RouterLink, Glyphe],
  templateUrl: './course.html'
})
export class FormulaireCourse {
  private readonly api = inject(ApiService);
  private readonly fb = inject(NonNullableFormBuilder);

  // Préremplis quand on arrive depuis la fiche d'un livreur
  readonly livreurId = input<string>();
  readonly vehicule = input<string>();

  protected readonly tarifs = TARIFS;
  protected readonly cles = Object.keys(TARIFS);
  protected readonly zones = toSignal(this.api.zones(), { initialValue: [] as Zone[] });
  protected readonly reponse = signal<CourseReponse | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly enCours = signal(false);
  protected readonly fcfa = fcfa;
  protected readonly formatDistance = formatDistance;
  protected readonly minutesTrajet = minutesTrajet;

  protected readonly form = this.fb.group({
    clientNom: ['', [Validators.required, Validators.maxLength(120)]],
    clientTelephone: ['', [Validators.required, telephoneValide]],
    vehicule: ['moto', Validators.required],
    departZoneId: ['', Validators.required],
    destinationZoneId: ['', Validators.required],
    precision: ['', Validators.maxLength(200)],
    description: ['', Validators.maxLength(500)]
  });

  // Valeurs du formulaire suivies en direct pour l'estimation du prix
  private readonly valeurs = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  // Estimation à vol d'oiseau entre les deux quartiers, majorée de 30 % pour les rues
  protected readonly estimation = computed(() => {
    const v = this.valeurs();
    const depart = this.zones().find(z => String(z.id) === String(v.departZoneId));
    const arrivee = this.zones().find(z => String(z.id) === String(v.destinationZoneId));
    if (!depart?.latitude || !arrivee?.latitude) return null;

    const metres = Math.round(
      distanceMetres(
        { lat: depart.latitude, lon: depart.longitude! },
        { lat: arrivee.latitude, lon: arrivee.longitude! }
      ) * 1.3
    );
    const tarif = TARIFS[v.vehicule ?? 'moto'] ?? TARIFS['moto'];
    const prix = Math.max(tarif.minimum, Math.round((metres / 1000) * tarif.parKm / 100) * 100);
    return { metres, prix };
  });

  constructor() {
    effect(() => {
      const v = this.vehicule();
      if (v && TARIFS[v]) this.form.controls.vehicule.setValue(v);
    });
  }

  protected envoyer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const livreur = this.livreurId();
    this.enCours.set(true);
    this.erreur.set(null);
    this.api.creerCourse({
      clientNom: v.clientNom.trim(),
      clientTelephone: v.clientTelephone,
      vehicule: v.vehicule,
      departZoneId: Number(v.departZoneId),
      destinationZoneId: Number(v.destinationZoneId),
      precision: v.precision.trim() || undefined,
      description: v.description.trim() || undefined,
      livreurId: livreur ? Number(livreur) : undefined
    }).subscribe({
      next: r => { this.reponse.set(r); this.enCours.set(false); },
      error: e => { this.erreur.set(messageErreur(e.status)); this.enCours.set(false); }
    });
  }
}
