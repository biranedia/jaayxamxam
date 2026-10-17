import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Yonni } from './yonni';

const livreur = {
  id: 1, nom: 'Pape Sow', telephone: '771000001', vehicule: 'moto',
  chargeMaxKg: 30, prixParKm: 300, prixMinimum: 1000, verifie: true,
  derniereActivite: new Date().toISOString(), distanceM: 140, noteMoyenne: 4.9, nombreAvis: 34
};

describe('Yonni la', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Yonni],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  async function ouvrir(livreurs: unknown[]) {
    const fixture = TestBed.createComponent(Yonni);
    fixture.detectChanges();
    http.expectOne(r => r.url === '/api/livreurs').flush(livreurs);
    await fixture.whenStable();
    return fixture;
  }

  it('affiche le livreur, sa charge et son tarif au kilomètre', async () => {
    const el = (await ouvrir([livreur])).nativeElement as HTMLElement;
    expect(el.textContent).toContain('Pape Sow');
    expect(el.textContent).toContain('30 kg');
    expect(el.textContent).toContain('300 FCFA/km');
    expect(el.textContent).toContain('min. 1 000 FCFA');
  });

  it('affiche la distance et une durée de trajet', async () => {
    const el = (await ouvrir([livreur])).nativeElement as HTMLElement;
    expect(el.querySelector('.distance')?.textContent).toContain('140 m');
    expect(el.querySelector('.distance')?.textContent).toContain('min');
  });

  it('filtre sur le véhicule choisi', async () => {
    const fixture = await ouvrir([]);
    const puces = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button.puce')];
    (puces.find(p => p.textContent?.includes('Camionnette')) as HTMLButtonElement).click();
    await fixture.whenStable();

    const req = http.expectOne(r => r.url === '/api/livreurs');
    expect(req.request.params.get('vehicule')).toBe('camionnette');
    req.flush([]);
  });

  it('annonce la liste vide sans faire croire à une panne', async () => {
    const el = (await ouvrir([])).nativeElement as HTMLElement;
    expect(el.querySelector('.vide')?.textContent).toContain('Aucun livreur');
    expect(el.querySelector('.erreur')).toBeNull();
  });
});
