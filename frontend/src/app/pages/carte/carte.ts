import { AfterViewInit, Component, ElementRef, OnDestroy, computed, effect, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { ApiService, Artisan, Livreur } from '../../api.service';
import { Geo, distanceMetres, estEnLigne, formatDistance } from '../../geo';
import { Glyphe, teinte } from '../../glyphe';
import { echapper, initiales, urlPhoto } from '../../utils';

const DAKAR: L.LatLngTuple = [14.7167, -17.4677];

// Une ligne de la liste, qu'il s'agisse d'un artisan ou d'un livreur
interface Proche {
  id: number;
  lien: unknown[] | null;
  nom: string;
  sousTitre: string;
  cle: string;       // dessin
  famille: string;   // couleur
  photo?: string;
  lat: number;
  lon: number;
  distanceM?: number;
  enLigne: boolean;
}

@Component({
  selector: 'app-carte',
  imports: [RouterLink, Glyphe],
  templateUrl: './carte.html'
})
export class Carte implements AfterViewInit, OnDestroy {
  private readonly api = inject(ApiService);
  protected readonly geo = inject(Geo);

  private readonly conteneur = viewChild.required<ElementRef<HTMLElement>>('carte');
  private carte?: L.Map;
  private couche?: L.LayerGroup;

  protected readonly artisans = signal<Artisan[]>([]);
  protected readonly livreurs = signal<Livreur[]>([]);
  protected readonly tuilesAbsentes = signal(false);
  protected readonly formatDistance = formatDistance;
  protected readonly teinte = teinte;
  protected readonly initiales = initiales;
  protected readonly urlPhoto = urlPhoto;

  // Tout le monde sur la même liste, trié par distance quand on a la position
  protected readonly proches = computed<Proche[]>(() => {
    const moi = this.geo.position();
    const avecDistance = (lat: number, lon: number) =>
      moi ? distanceMetres(moi, { lat, lon }) : undefined;

    const liste: Proche[] = [
      ...this.artisans()
        .filter(a => a.latitude != null && a.longitude != null)
        .map(a => ({
          id: a.id,
          lien: ['/artisan', a.id],
          nom: a.nom,
          sousTitre: `${a.metier} · ${a.quartier}`,
          cle: a.metierIcone ?? 'defaut',
          famille: a.metierFamille ?? 'services',
          photo: a.photo,
          lat: a.latitude!,
          lon: a.longitude!,
          distanceM: a.distanceM ?? avecDistance(a.latitude!, a.longitude!),
          enLigne: estEnLigne(a.derniereActivite)
        })),
      ...this.livreurs()
        .filter(l => l.latitude != null && l.longitude != null)
        .map(l => ({
          id: l.id,
          lien: null,
          nom: l.nom,
          sousTitre: `Yonni la · ${l.vehicule}`,
          cle: l.vehicule,
          famille: l.vehicule,
          photo: l.photo,
          lat: l.latitude!,
          lon: l.longitude!,
          distanceM: l.distanceM ?? avecDistance(l.latitude!, l.longitude!),
          enLigne: estEnLigne(l.derniereActivite)
        }))
    ];
    return liste.sort((a, b) => (a.distanceM ?? Infinity) - (b.distanceM ?? Infinity));
  });

  constructor() {
    effect(() => {
      const ou = this.geo.position();
      this.api.artisans(undefined, undefined, ou).subscribe({ next: a => this.artisans.set(a), error: () => {} });
      this.api.livreurs(undefined, ou).subscribe({ next: l => this.livreurs.set(l), error: () => {} });
    });

    // Redessine les marqueurs à chaque nouvelle donnée
    effect(() => {
      const points = this.proches();
      const moi = this.geo.position();
      if (this.carte) this.dessiner(points, moi);
    });
  }

  ngAfterViewInit(): void {
    // L'attribution OpenStreetMap est reprise dans le bandeau au-dessus de la carte,
    // sinon elle se cache derrière la feuille de résultats.
    this.carte = L.map(this.conteneur().nativeElement, { zoomControl: false, attributionControl: false })
      .setView(DAKAR, 13);
    L.control.zoom({ position: 'topright' }).addTo(this.carte);

    const tuiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 });
    // Réseau faible : on garde un fond quadrillé et les marqueurs restent lisibles
    tuiles.on('tileerror', () => this.tuilesAbsentes.set(true));
    tuiles.addTo(this.carte);

    this.couche = L.layerGroup().addTo(this.carte);
    this.dessiner(this.proches(), this.geo.position());
  }

  ngOnDestroy(): void {
    this.carte?.remove();
  }

  protected centrerSurMoi(): void {
    const moi = this.geo.position();
    if (moi) this.carte?.setView([moi.lat, moi.lon], 15);
    else this.geo.demander();
  }

  private dessiner(points: Proche[], moi: { lat: number; lon: number } | null): void {
    if (!this.couche) return;
    this.couche.clearLayers();

    for (const p of points) {
      // Leaflet écrit ce HTML tel quel : les noms viennent de la base, donc on les échappe.
      // Sans cela, un artisan nommé « <img onerror=...> » ferait exécuter son code ici.
      const marqueur = L.divIcon({
        className: '',
        html: `<div class="marqueur" style="background:${teinte(p.famille)};opacity:${p.enLigne ? 1 : .55}">
                 <strong style="font:700 12px Archivo,sans-serif">${echapper(initiales(p.nom))}</strong>
               </div>`,
        iconSize: [34, 34],
        iconAnchor: [17, 34]
      });

      // La bulle est construite en DOM : le texte n'est jamais interprété comme du HTML
      const bulle = document.createElement('div');
      const titre = document.createElement('strong');
      titre.textContent = p.nom;
      const detail = document.createElement('div');
      detail.textContent = p.sousTitre;
      bulle.append(titre, detail);

      L.marker([p.lat, p.lon], { icon: marqueur, title: p.nom })
        .bindPopup(bulle)
        .addTo(this.couche);
    }

    if (moi) {
      const pastille = L.divIcon({
        className: '',
        html: '<div class="marqueur moi"></div>',
        iconSize: [34, 34],
        iconAnchor: [17, 17]
      });
      L.marker([moi.lat, moi.lon], { icon: pastille, title: 'Vous êtes ici' }).addTo(this.couche);
      this.carte?.setView([moi.lat, moi.lon], 14);
    }
  }
}
