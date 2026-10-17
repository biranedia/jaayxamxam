import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { Accueil } from './accueil';

const ilYa = (min: number) => new Date(Date.now() - min * 60_000).toISOString();

const metiers = [
  { id: 1, nom: 'Plombier', icone: 'plombier', famille: 'batiment' },
  { id: 3, nom: 'Maçon', icone: 'macon', famille: 'batiment' },
  { id: 12, nom: 'Ménage', icone: 'menage', famille: 'maison' }
];

const artisans = [
  { id: 1, nom: 'Démo Plombier', anneesExperience: 8, verifie: true, metierId: 1,
    metier: 'Plombier', zoneId: 2, ville: 'Dakar', quartier: 'Médina', derniereActivite: ilYa(2) },
  { id: 2, nom: 'Démo Maçon', anneesExperience: 5, verifie: false, metierId: 3,
    metier: 'Maçon', zoneId: 3, ville: 'Guédiawaye', quartier: 'Golf Sud', derniereActivite: ilYa(600) }
];

const livreurs = [
  { id: 1, nom: 'Démo Moto', vehicule: 'moto', chargeMaxKg: 20, prixParKm: 250,
    prixMinimum: 1000, verifie: true, derniereActivite: ilYa(1) }
];

describe('Accueil', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Accueil],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Répond aux trois appels de comptage que la page lance à sa création. */
  async function servir(fixture: { whenStable(): Promise<unknown> }): Promise<void> {
    await fixture.whenStable();
    http.expectOne(r => r.url === '/api/metiers').flush(metiers);
    http.expectOne(r => r.url === '/api/livreurs').flush(livreurs);
    http.expectOne(r => r.url === '/api/artisans').flush(artisans);
    await fixture.whenStable();
  }

  it('affiche les chiffres réels de la plateforme', async () => {
    const fixture = TestBed.createComponent(Accueil);
    await servir(fixture);

    const chiffres = [...(fixture.nativeElement as HTMLElement).querySelectorAll('.chiffre')]
      .map(c => c.textContent?.trim());
    // artisans, livreurs, métiers, en ligne (1 artisan récent + 1 livreur récent)
    expect(chiffres).toEqual(['2', '1', '3', '2']);
  });

  it('compte les métiers de chaque famille sur sa tuile', async () => {
    const fixture = TestBed.createComponent(Accueil);
    await servir(fixture);

    const tuiles = (fixture.nativeElement as HTMLElement).querySelectorAll('.tuile');
    // 5 familles + la tuile « Tout voir »
    expect(tuiles.length).toBe(6);
    expect(tuiles[0].textContent).toContain('2 métiers');  // bâtiment
    expect(tuiles[1].textContent).toContain('1 métier');   // maison
  });

  it('envoie la saisie vers la recherche dans le paramètre q', async () => {
    const fixture = TestBed.createComponent(Accueil);
    await servir(fixture);

    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const champ = (fixture.nativeElement as HTMLElement)
      .querySelector('.hero-recherche input') as HTMLInputElement;
    champ.value = '  plombier  ';
    champ.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    (fixture.nativeElement as HTMLElement)
      .querySelector('.hero-recherche')!.dispatchEvent(new Event('submit'));

    expect(navigate).toHaveBeenCalledWith(['/artisans'], { queryParams: { q: 'plombier' } });
  });

  it('mène aux métiers filtrés par famille', async () => {
    const fixture = TestBed.createComponent(Accueil);
    await servir(fixture);

    const premiere = (fixture.nativeElement as HTMLElement)
      .querySelector('.tuile') as HTMLAnchorElement;
    expect(premiere.getAttribute('href')).toBe('/artisans?famille=batiment');
  });

  it('ne casse pas quand le serveur est injoignable', async () => {
    const fixture = TestBed.createComponent(Accueil);
    await fixture.whenStable();

    http.expectOne(r => r.url === '/api/metiers').error(new ProgressEvent('echec'));
    http.expectOne(r => r.url === '/api/livreurs').error(new ProgressEvent('echec'));
    http.expectOne(r => r.url === '/api/artisans').error(new ProgressEvent('echec'));
    await fixture.whenStable();

    // Les compteurs retombent sur zéro, la page reste affichée
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.hero-titre')).toBeTruthy();
    expect([...el.querySelectorAll('.chiffre')].map(c => c.textContent?.trim()))
      .toEqual(['0', '0', '0', '0']);
  });
});
