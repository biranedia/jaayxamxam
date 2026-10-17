import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
  });

  it('crée la coquille de l\'application', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('affiche la marque dans le bandeau', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.marque')?.textContent).toContain('Jaay sa xam-xam');
  });

  it('propose les cinq onglets du bas, accueil en tête', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const onglets = (fixture.nativeElement as HTMLElement).querySelectorAll('.onglets a');
    expect([...onglets].map(o => o.textContent?.trim()))
      .toEqual(['Accueil', 'Artisans', 'Yonni la', 'Carte', 'Suivi']);
  });

  it('reprend les mêmes liens dans la navigation du grand écran', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const liens = (fixture.nativeElement as HTMLElement).querySelectorAll('.nav-large a');
    expect([...liens].map(l => l.textContent?.trim()))
      .toEqual(['Accueil', 'Artisans', 'Yonni la', 'Carte', 'Suivi']);
  });

  // L'inscription a quitté la barre du bas : elle doit rester visible partout ailleurs,
  // sinon les travailleurs ne trouvent plus comment proposer leurs services.
  it("garde l'inscription accessible depuis l'en-tête", async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const bouton = (fixture.nativeElement as HTMLElement)
      .querySelector('.bouton-inscription') as HTMLAnchorElement | null;
    expect(bouton?.textContent).toContain("M'inscrire");
    expect(bouton?.getAttribute('href')).toBe('/inscription');
  });

  it('invite à se localiser tant que la position est inconnue', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('.puce-lieu')?.textContent)
      .toContain('Me localiser');
  });
});
