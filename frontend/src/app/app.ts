import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { Glyphe } from './glyphe';
import { Geo } from './geo';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Glyphe],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  readonly geo = inject(Geo);
  private readonly router = inject(Router);

  /**
   * Le back-office n'affiche ni la marque ni la navigation publique.
   *
   * <p>Deux raisons. La première est de discrétion : une barre « M'inscrire » au-dessus d'un
   * écran d'administration annonce à qui tombe sur l'adresse qu'il y a quelque chose à forcer.
   * La seconde est d'usage : ce sont deux mondes différents, et mélanger les deux navigations
   * fait perdre du temps à celui qui travaille.
   */
  protected readonly ecranNu = signal(false);

  /** Une seule liste pour les deux navigations : la barre du bas et celle de l'en-tête. */
  protected readonly liens = [
    { chemin: '/', nom: 'Accueil', glyphe: 'maison' },
    { chemin: '/artisans', nom: 'Artisans', glyphe: 'loupe' },
    { chemin: '/yonni', nom: 'Yonni la', glyphe: 'moto' },
    { chemin: '/carte', nom: 'Carte', glyphe: 'carte' },
    { chemin: '/suivi', nom: 'Suivi', glyphe: 'liste' }
  ];

  constructor() {
    // Sans cela, on change de page et on arrive au milieu : le navigateur garde le défilement
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(e => {
        window.scrollTo({ top: 0 });
        this.ecranNu.set((e as NavigationEnd).urlAfterRedirects.startsWith('/admin'));
      });

    // Premier affichage : l'événement de navigation n'est pas encore passé
    this.ecranNu.set(this.router.url.startsWith('/admin'));
  }

  localiser(): void {
    this.geo.demander();
  }

  libelleLieu(): string {
    switch (this.geo.etat()) {
      case 'ok': return 'Autour de moi';
      case 'recherche': return 'Localisation…';
      case 'refusee': return 'Position refusée';
      case 'indisponible': return 'Position indisponible';
      default: return 'Me localiser';
    }
  }
}
