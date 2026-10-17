import { TestBed } from '@angular/core/testing';
import { LOCALE_ID } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { Admin } from './admin';

registerLocaleData(localeFr, 'fr');

const file = {
  artisansEnAttente: 1,
  livreursEnAttente: 1,
  artisansActifs: 4,
  livreursActifs: 2,
  file: [
    { id: 10, type: 'artisan', nom: 'Moussa Ndiaye', telephone: '770000001',
      detail: 'Plombier · Médina', creeLe: '2026-10-16T09:00:00' },
    { id: 20, type: 'livreur', nom: 'Pape Sow', telephone: '771000001',
      detail: 'moto · jusqu\'à 30 kg', creeLe: '2026-10-16T10:00:00' }
  ]
};

describe('Administration', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Admin],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: LOCALE_ID, useValue: 'fr' }],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  function creer() {
    const fixture = TestBed.createComponent(Admin);
    fixture.detectChanges();
    return fixture;
  }

  async function ouvrir(jeton = 'bon-jeton') {
    const fixture = creer();
    const champ = (fixture.nativeElement as HTMLElement).querySelector('input') as HTMLInputElement;
    champ.value = jeton;
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    return fixture;
  }

  it('ne montre rien avant que le jeton soit accepté', () => {
    const el = creer().nativeElement as HTMLElement;
    expect(el.querySelector('input[type=password]')).not.toBeNull();
    expect(el.textContent).not.toContain("File d'attente");
  });

  it('envoie le jeton dans un en-tête, jamais dans l\'adresse', async () => {
    const fixture = await ouvrir('bon-jeton');

    const req = http.expectOne('/api/admin/attente');
    expect(req.request.headers.get('X-Admin-Token')).toBe('bon-jeton');
    expect(req.request.urlWithParams).toBe('/api/admin/attente');
    req.flush(file);
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Moussa Ndiaye');
    expect(el.textContent).toContain('Pape Sow');
  });

  it('explique un jeton refusé sans ouvrir la file', async () => {
    const fixture = await ouvrir('mauvais');
    http.expectOne('/api/admin/attente').flush({}, { status: 403, statusText: 'Forbidden' });
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.erreur')?.textContent).toContain('Jeton refusé');
    expect(el.querySelector('input[type=password]')).not.toBeNull();
  });

  it('valide un profil puis recharge la file', async () => {
    const fixture = await ouvrir();
    http.expectOne('/api/admin/attente').flush(file);
    await fixture.whenStable();

    const boutons = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')];
    (boutons.find(b => b.textContent?.includes('Rendre visible')) as HTMLButtonElement).click();

    const action = http.expectOne('/api/admin/artisan/10/valider');
    expect(action.request.method).toBe('POST');
    expect(action.request.headers.get('X-Admin-Token')).toBe('bon-jeton');
    action.flush({});

    // La file est relue côté serveur plutôt que bricolée côté navigateur
    http.expectOne('/api/admin/attente').flush({ ...file, file: [] });
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('.vide')).not.toBeNull();
  });

  it('écarte une inscription sans la supprimer', async () => {
    const fixture = await ouvrir();
    http.expectOne('/api/admin/attente').flush(file);
    await fixture.whenStable();

    const boutons = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')];
    (boutons.find(b => b.textContent?.includes('Écarter')) as HTMLButtonElement).click();

    expect(http.expectOne('/api/admin/artisan/10/refuser').request.method).toBe('POST');
  });
});
