import { TestBed } from '@angular/core/testing';
import { LOCALE_ID } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { provideRouter } from '@angular/router';
import { Suivi } from './suivi';

registerLocaleData(localeFr, 'fr');

const course = {
  type: 'course', codeSuivi: 'R4TB9HCN', statut: 'EN_ROUTE', creeLe: '2026-10-16T11:00:00',
  resume: 'Médina → Golf Sud · deux cartons', prixEstime: 2400,
  intervenant: {
    nom: 'Pape Sow', telephone: '771000001', cle: 'moto',
    latitude: 14.6794, longitude: -17.4489, derniereActivite: new Date().toISOString()
  }
};

describe('Suivi', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Suivi],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: LOCALE_ID, useValue: 'fr' }
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  async function ouvrir(code: string, reponse: object | null, statut = 200) {
    const fixture = TestBed.createComponent(Suivi);
    fixture.componentRef.setInput('code', code);
    fixture.detectChanges();
    const req = http.expectOne(`/api/suivi/${code}`);
    if (statut === 200) req.flush(reponse);
    else req.flush({}, { status: statut, statusText: 'Not Found' });
    await fixture.whenStable();
    return fixture;
  }

  it('affiche le trajet, le prix et le livreur', async () => {
    const el = (await ouvrir('R4TB9HCN', course)).nativeElement as HTMLElement;
    expect(el.textContent).toContain('Médina → Golf Sud');
    expect(el.textContent).toContain('2 400 FCFA');
    expect(el.textContent).toContain('Pape Sow');
  });

  it('marque les étapes franchies jusqu\'à « En route »', async () => {
    const el = (await ouvrir('R4TB9HCN', course)).nativeElement as HTMLElement;
    const etapes = [...el.querySelectorAll('.etapes-tampon li')];
    expect(etapes.map(e => e.textContent?.trim()))
      .toEqual(['Envoyée', 'Acceptée', 'En route', 'Terminée']);
    expect(etapes.filter(e => e.classList.contains('faite')).length).toBe(3);
    expect(etapes[2].classList.contains('courante')).toBe(true);
  });

  it('le formulaire part vraiment quand on valide le code à la main', async () => {
    // Sans FormsModule, (ngSubmit) ne se déclenchait jamais : le bouton ne faisait rien
    const fixture = TestBed.createComponent(Suivi);
    fixture.detectChanges();
    const champ = (fixture.nativeElement as HTMLElement).querySelector('input') as HTMLInputElement;
    champ.value = 'cr4tb9hcn';
    (fixture.nativeElement as HTMLElement).querySelector('form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));

    // Le code saisi en minuscules est accepté
    http.expectOne('/api/suivi/CR4TB9HCN').flush(course);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Pape Sow');
  });

  it('signale un code inconnu sans vider la saisie', async () => {
    const el = (await ouvrir('ZZZZZZZZ', null, 404)).nativeElement as HTMLElement;
    expect(el.querySelector('.erreur')?.textContent).toContain('Aucune demande avec ce code');
    expect((el.querySelector('input') as HTMLInputElement).value).toBe('ZZZZZZZZ');
  });

  it('dit que personne n\'a encore pris la demande', async () => {
    const sansIntervenant = { ...course, statut: 'NOUVELLE', intervenant: undefined };
    const el = (await ouvrir('K7M2PQX4', sansIntervenant)).nativeElement as HTMLElement;
    expect(el.querySelector('.vide')?.textContent).toContain('Personne n\'a encore pris');
  });
});
