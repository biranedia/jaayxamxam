import { Component, computed, effect, inject, input, linkedSignal, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, Artisan, Metier, Zone } from '../../api.service';
import { Contacts } from '../../contacts';
import { Geo, estDisponible, formatDistance } from '../../geo';
import { FAMILLES, Glyphe, teinte } from '../../glyphe';
import { etoiles, fcfa, initiales, sansAccent, urlPhoto } from '../../utils';

@Component({
  selector: 'app-recherche',
  imports: [RouterLink, Glyphe],
  templateUrl: './recherche.html'
})
export class Recherche {
  private readonly api = inject(ApiService);
  protected readonly contacts = inject(Contacts);
  protected readonly geo = inject(Geo);

  // Lus dans l'URL (?famille=batiment&q=plombier) : l'accueil envoie ici déjà filtré,
  // et le lien reste partageable.
  readonly famille = input<string | undefined>();
  readonly q = input<string | undefined>();

  protected readonly familles = FAMILLES;
  protected readonly metiers = signal<Metier[]>([]);
  protected readonly zones = signal<Zone[]>([]);
  protected readonly artisans = signal<Artisan[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);

  // linkedSignal : l'URL donne la valeur de départ, un clic de l'utilisateur la remplace,
  // et un nouveau paramètre d'URL reprend la main. Un signal simple ne ferait que le premier.
  protected readonly familleChoisie = linkedSignal(() => this.famille());
  protected readonly recherche = linkedSignal(() => this.q() ?? '');
  protected readonly metierId = signal<number | undefined>(undefined);
  protected readonly zoneId = signal<number | undefined>(undefined);

  // Outils de présentation utilisés par le gabarit
  protected readonly urlPhoto = urlPhoto;
  protected readonly formatDistance = formatDistance;
  protected readonly estDisponible = estDisponible;
  protected readonly initiales = initiales;
  protected readonly etoiles = etoiles;
  protected readonly fcfa = fcfa;
  protected readonly teinte = teinte;

  // Quatre cartes grises pendant le chargement : @for a besoin d'un tableau à parcourir
  protected readonly squelettes = [0, 1, 2, 3];

  // Métiers proposés : ceux de la famille choisie, ou ceux qui correspondent à la recherche.
  // Avec 25 métiers, tout afficher d'un coup ne serait plus lisible.
  protected readonly metiersProposes = computed(() => {
    const texte = sansAccent(this.recherche().trim());
    if (texte) {
      return this.metiers().filter(m => sansAccent(m.nom).includes(texte));
    }
    const f = this.familleChoisie();
    return f ? this.metiers().filter(m => m.famille === f) : [];
  });

  // Le métier choisi reste visible même s'il sort de la liste proposée
  protected readonly metierChoisi = computed(
    () => this.metiers().find(m => m.id === this.metierId())
  );

  protected readonly zoneChoisie = computed(
    () => this.zones().find(z => z.id === this.zoneId())
  );

  /** Vrai dès qu'un filtre est actif : sert à proposer la remise à zéro. */
  protected readonly filtre = computed(
    () => !!this.familleChoisie() || !!this.recherche().trim()
      || this.metierId() !== undefined || this.zoneId() !== undefined
  );

  // Les plus proches d'abord dès qu'on connaît la position du visiteur
  protected readonly resultats = computed(() => {
    const liste = [...this.artisans()];
    if (this.geo.position()) {
      liste.sort((a, b) => (a.distanceM ?? Infinity) - (b.distanceM ?? Infinity));
    }
    return liste;
  });

  protected readonly nombreEnLigne = computed(
    () => this.resultats().filter(a => estDisponible(a.derniereActivite)).length
  );

  constructor() {
    this.api.metiers().subscribe({ next: m => this.metiers.set(m), error: () => this.echec() });
    this.api.zones().subscribe({ next: z => this.zones.set(z), error: () => this.echec() });

    // Relance la recherche à chaque changement de filtre ou de position
    effect(() => {
      const metier = this.metierId();
      const zone = this.zoneId();
      const ou = this.geo.position();
      this.chargement.set(true);
      this.erreur.set(null);
      this.api.artisans(metier, zone, ou).subscribe({
        next: liste => { this.artisans.set(liste); this.chargement.set(false); },
        error: () => this.echec()
      });
    });
  }

  protected choisirFamille(cle?: string): void {
    this.familleChoisie.set(this.familleChoisie() === cle ? undefined : cle);
    this.recherche.set('');
    this.metierId.set(undefined);
  }

  protected chercher(texte: string): void {
    this.recherche.set(texte);
    if (texte.trim()) this.familleChoisie.set(undefined);
  }

  protected choisirMetier(id?: number): void {
    this.metierId.set(this.metierId() === id ? undefined : id);
  }

  protected toutVoir(): void {
    this.familleChoisie.set(undefined);
    this.recherche.set('');
    this.metierId.set(undefined);
    this.zoneId.set(undefined);
  }

  protected choisirZone(valeur: string): void {
    this.zoneId.set(valeur ? Number(valeur) : undefined);
  }

  private echec(): void {
    this.chargement.set(false);
    this.erreur.set("Impossible de joindre le serveur. Vérifiez que l'API est démarrée.");
  }
}
