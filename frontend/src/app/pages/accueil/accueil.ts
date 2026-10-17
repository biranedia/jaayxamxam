import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService, Artisan, Livreur, Metier } from '../../api.service';
import { Geo, estEnLigne } from '../../geo';
import { FAMILLES, Glyphe, teinte } from '../../glyphe';

/**
 * Page d'accueil : la première chose que voit un visiteur.
 *
 * <p>Elle doit répondre à trois questions en quelques secondes : qu'est-ce que ce site,
 * qu'est-ce que je peux y faire tout de suite, et est-ce que quelqu'un y est vraiment.
 * D'où les chiffres en direct : une plateforme vide se voit, autant l'assumer.
 */
@Component({
  selector: 'app-accueil',
  imports: [RouterLink, Glyphe],
  templateUrl: './accueil.html'
})
export class Accueil {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  protected readonly geo = inject(Geo);

  protected readonly familles = FAMILLES;
  protected readonly teinte = teinte;

  // Un dessin représentatif par famille, pour que la tuile se lise sans lire
  private static readonly GLYPHES: Record<string, string> = {
    batiment: 'macon',
    maison: 'menage',
    mecanique: 'mecanicien',
    beaute: 'tailleur',
    services: 'livre'
  };

  protected glypheFamille(cle: string): string {
    return Accueil.GLYPHES[cle] ?? 'defaut';
  }

  protected readonly metiers = signal<Metier[]>([]);
  protected readonly artisans = signal<Artisan[]>([]);
  protected readonly livreurs = signal<Livreur[]>([]);
  protected readonly chargement = signal(true);
  protected readonly recherche = signal('');

  protected readonly nombreEnLigne = computed(
    () => this.artisans().filter(a => estEnLigne(a.derniereActivite)).length
      + this.livreurs().filter(l => estEnLigne(l.derniereActivite)).length
  );

  // Combien de métiers par famille : affiché sur chaque tuile pour montrer l'étendue
  protected readonly comptesFamille = computed(() => {
    const comptes: Record<string, number> = {};
    for (const m of this.metiers()) comptes[m.famille] = (comptes[m.famille] ?? 0) + 1;
    return comptes;
  });

  constructor() {
    // Trois appels de lecture, sans filtre : ils servent uniquement aux compteurs
    this.api.metiers().subscribe({ next: m => this.metiers.set(m), error: () => this.fini() });
    this.api.livreurs().subscribe({ next: l => this.livreurs.set(l), error: () => this.fini() });
    this.api.artisans().subscribe({
      next: a => { this.artisans.set(a); this.fini(); },
      error: () => this.fini()
    });
  }

  /** Envoie la saisie vers la page de recherche, qui lit le paramètre « q » de l'URL. */
  protected lancerRecherche(evenement: Event): void {
    evenement.preventDefault();
    const texte = this.recherche().trim();
    this.router.navigate(['/artisans'], { queryParams: texte ? { q: texte } : {} });
  }

  protected localiser(): void {
    this.geo.demander();
  }

  private fini(): void {
    this.chargement.set(false);
  }
}
