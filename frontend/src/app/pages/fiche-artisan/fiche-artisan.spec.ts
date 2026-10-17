import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { FicheArtisan } from './fiche-artisan';

const detail = {
  artisan: {
    id: 1, nom: 'Démo Plombier', telephone: '770000001', description: 'Fuites et WC',
    anneesExperience: 8, tarifIndicatif: 5000, verifie: true,
    metierId: 1, metier: 'Plombier', metierIcone: 'plombier',
    zoneId: 2, ville: 'Dakar', quartier: 'Médina'
  },
  noteMoyenne: 4.5,
  nombreAvis: 12,
  avis: [
    { note: 5, commentaire: 'Très bien', creeLe: '2026-10-15T10:00:00' },
    { note: 4, creeLe: '2026-10-14T10:00:00' }
  ]
};

registerLocaleData(localeFr, 'fr');

describe('FicheArtisan', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FicheArtisan],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: LOCALE_ID, useValue: 'fr' }
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  async function ouvrir() {
    const fixture = TestBed.createComponent(FicheArtisan);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();
    http.expectOne('/api/artisans/1').flush(detail);
    await fixture.whenStable();
    return fixture;
  }

  it('affiche la fiche, la note moyenne et les avis', async () => {
    const fixture = await ouvrir();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('Démo Plombier');
    expect(texte).toContain('4,5');
    expect(texte).toContain('12 avis');
    expect(texte).toContain('Très bien');
  });

  it('désactive l\'envoi de l\'avis tant que le code de suivi est vide', async () => {
    const fixture = await ouvrir();
    const bouton = (fixture.nativeElement as HTMLElement).querySelector('form button') as HTMLButtonElement;
    expect(bouton.disabled).toBe(true);
  });

  it('affiche « Artisan introuvable » sur une erreur 404', async () => {
    const fixture = TestBed.createComponent(FicheArtisan);
    fixture.componentRef.setInput('id', '99');
    fixture.detectChanges();
    http.expectOne('/api/artisans/99').flush({}, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Artisan introuvable');
  });
});
