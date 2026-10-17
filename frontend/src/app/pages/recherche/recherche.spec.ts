import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Recherche } from './recherche';

const ilYa = (min: number) => new Date(Date.now() - min * 60_000).toISOString();

const metiers = [
  { id: 1, nom: 'Plombier', icone: 'plombier', famille: 'batiment' },
  { id: 3, nom: 'Maçon', icone: 'macon', famille: 'batiment' },
  { id: 12, nom: 'Ménage', icone: 'menage', famille: 'maison' },
  { id: 24, nom: 'Vigile', icone: 'bouclier', famille: 'services' }
];

const artisan = {
  id: 1, nom: 'Démo Plombier', telephone: '770000001', description: 'Fuites et WC',
  anneesExperience: 8, tarifIndicatif: 5000, verifie: true,
  metierId: 1, metier: 'Plombier', metierIcone: 'plombier', metierFamille: 'batiment',
  photo: 'a1b2c3d4e5f60718293a4b5c6d7e8f90.jpg',
  zoneId: 2, ville: 'Dakar', quartier: 'Médina',
  derniereActivite: ilYa(2), distanceM: 280, noteMoyenne: 4.8, nombreAvis: 12
};

describe('Recherche', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Recherche],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  async function ouvrir(artisans: unknown[]) {
    const fixture = TestBed.createComponent(Recherche);
    fixture.detectChanges();
    http.expectOne('/api/metiers').flush(metiers);
    http.expectOne('/api/zones').flush([{ id: 2, ville: 'Dakar', quartier: 'Médina' }]);
    http.expectOne(r => r.url === '/api/artisans').flush(artisans);
    await fixture.whenStable();
    return fixture;
  }

  const puces = (fixture: { nativeElement: unknown }) =>
    [...(fixture.nativeElement as HTMLElement).querySelectorAll('button.puce')] as HTMLButtonElement[];

  const parTexte = (fixture: { nativeElement: unknown }, texte: string) =>
    puces(fixture).find(p => p.textContent?.includes(texte)) as HTMLButtonElement;

  async function taper(fixture: { nativeElement: unknown; whenStable(): Promise<unknown> }, texte: string) {
    const champ = (fixture.nativeElement as HTMLElement).querySelector('input[type=search]') as HTMLInputElement;
    champ.value = texte;
    champ.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('affiche le panneau de l\'artisan avec son sceau « Vérifié »', async () => {
    const el = (await ouvrir([artisan])).nativeElement as HTMLElement;
    expect(el.textContent).toContain('Démo Plombier');
    expect(el.querySelector('.sceau')).not.toBeNull();
    // Le numéro n'est plus dans la liste : les boutons le demandent au clic
    expect(el.textContent).not.toContain('770000001');
    expect(el.querySelector('a[href^="tel:"]')).toBeNull();
  });

  it('affiche la photo du profil quand il y en a une', async () => {
    const el = (await ouvrir([artisan])).nativeElement as HTMLElement;
    const photo = el.querySelector('img.portrait') as HTMLImageElement;
    expect(photo).not.toBeNull();
    expect(photo.getAttribute('src'))
      .toBe('/api/photos/a1b2c3d4e5f60718293a4b5c6d7e8f90.jpg');
  });

  it('retombe sur l\'outil dessiné quand il n\'y a pas de photo', async () => {
    const sansPhoto = { ...artisan, photo: undefined };
    const el = (await ouvrir([sansPhoto])).nativeElement as HTMLElement;
    expect(el.querySelector('img.portrait')).toBeNull();
    expect(el.querySelector('.peinture app-glyphe')).not.toBeNull();
  });

  it('demande le numéro au serveur quand on touche « Appeler »', async () => {
    const fixture = await ouvrir([artisan]);
    const boutons = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')];
    (boutons.find(b => b.textContent?.includes('Appeler')) as HTMLButtonElement).click();

    const req = http.expectOne('/api/contacts/artisan/1');
    expect(req.request.method).toBe('GET');
    req.flush({ telephone: '770000001' });
  });

  it('montre la présence et la distance envoyées par le serveur', async () => {
    const el = (await ouvrir([artisan])).nativeElement as HTMLElement;
    expect(el.querySelector('.presence.en-ligne')?.textContent).toContain('En ligne');
    expect(el.querySelector('.distance')?.textContent).toContain('280 m');
  });

  it('ne montre aucun métier tant qu\'aucune famille n\'est choisie', async () => {
    const fixture = await ouvrir([]);
    // Seules les familles sont proposées : 25 métiers d'un coup seraient illisibles
    expect(parTexte(fixture, 'Plombier')).toBeUndefined();
    expect(parTexte(fixture, 'Bâtiment')).toBeDefined();
  });

  it('propose les métiers de la famille choisie, et eux seuls', async () => {
    const fixture = await ouvrir([]);
    parTexte(fixture, 'Bâtiment').click();
    await fixture.whenStable();

    expect(parTexte(fixture, 'Plombier')).toBeDefined();
    expect(parTexte(fixture, 'Maçon')).toBeDefined();
    expect(parTexte(fixture, 'Vigile')).toBeUndefined();
  });

  it('trouve un métier à la recherche, même sans les accents', async () => {
    const fixture = await ouvrir([]);
    await taper(fixture, 'menage');

    expect(parTexte(fixture, 'Ménage')).toBeDefined();
    expect(parTexte(fixture, 'Plombier')).toBeUndefined();
  });

  it('le dit quand aucun métier ne correspond', async () => {
    const fixture = await ouvrir([]);
    await taper(fixture, 'pilote');

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucun métier ne correspond');
  });

  it('relance la recherche sur le métier choisi', async () => {
    const fixture = await ouvrir([]);
    parTexte(fixture, 'Bâtiment').click();
    await fixture.whenStable();
    parTexte(fixture, 'Plombier').click();
    await fixture.whenStable();

    const req = http.expectOne(r => r.url === '/api/artisans');
    expect(req.request.params.get('metierId')).toBe('1');
    req.flush([]);
  });

  it('affiche une erreur si l\'API ne répond pas', async () => {
    const fixture = TestBed.createComponent(Recherche);
    fixture.detectChanges();
    http.expectOne('/api/metiers').flush([]);
    http.expectOne('/api/zones').flush([]);
    http.expectOne(r => r.url === '/api/artisans').error(new ProgressEvent('error'));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('.erreur')).not.toBeNull();
  });
});

// L'accueil envoie ici avec ?famille=... : si ce lien casse, les tuiles ne filtrent plus rien.
describe('Recherche — filtres venus de l\'URL', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Recherche],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('applique la famille reçue en entrée et ne propose que ses métiers', async () => {
    const fixture = TestBed.createComponent(Recherche);
    fixture.componentRef.setInput('famille', 'batiment');
    await fixture.whenStable();

    http.expectOne(r => r.url === '/api/metiers').flush(metiers);
    http.expectOne(r => r.url === '/api/zones').flush([]);
    http.expectOne(r => r.url === '/api/artisans').flush([artisan]);
    await fixture.whenStable();

    const puces = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('[aria-label="Métiers"] .puce');
    expect([...puces].map(p => p.textContent?.trim())).toEqual(['Plombier', 'Maçon']);
  });

  it('pré-remplit le champ avec le texte reçu en entrée', async () => {
    const fixture = TestBed.createComponent(Recherche);
    fixture.componentRef.setInput('q', 'menage');
    await fixture.whenStable();

    http.expectOne(r => r.url === '/api/metiers').flush(metiers);
    http.expectOne(r => r.url === '/api/zones').flush([]);
    http.expectOne(r => r.url === '/api/artisans').flush([artisan]);
    await fixture.whenStable();

    const champ = (fixture.nativeElement as HTMLElement)
      .querySelector('.champ-recherche input') as HTMLInputElement;
    expect(champ.value).toBe('menage');

    const puces = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('[aria-label="Métiers"] .puce');
    expect([...puces].map(p => p.textContent?.trim())).toEqual(['Ménage']);
  });
});
