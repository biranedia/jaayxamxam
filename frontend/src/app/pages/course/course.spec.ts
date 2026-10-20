import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { FormulaireCourse } from './course';

const zones = [
  { id: 2, ville: 'Dakar', quartier: 'Médina', latitude: 14.68, longitude: -17.45 },
  { id: 5, ville: 'Guédiawaye', quartier: 'Golf Sud', latitude: 14.78, longitude: -17.39 }
];

describe('Demande de course', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormulaireCourse],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  async function ouvrir() {
    const fixture = TestBed.createComponent(FormulaireCourse);
    fixture.detectChanges();
    http.expectOne('/api/zones').flush(zones);
    await fixture.whenStable();
    return fixture;
  }

  it('ne montre aucun prix tant que le trajet n\'est pas choisi', async () => {
    const el = (await ouvrir()).nativeElement as HTMLElement;
    expect(el.querySelector('.recu')).toBeNull();
  });

  it('calcule le prix dès que les deux quartiers sont choisis', async () => {
    const fixture = await ouvrir();
    const composant = fixture.componentInstance as unknown as { form: { patchValue(v: unknown): void } };
    composant.form.patchValue({ departZoneId: '2', destinationZoneId: '5', vehicule: 'moto' });
    await fixture.whenStable();

    // Médina → Golf Sud : ~12,8 km à vol d'oiseau, majorés de 30 % pour les rues
    const resume = (fixture.nativeElement as HTMLElement).querySelector('.recu')?.textContent ?? '';
    expect(resume).toContain('16,7 km');
    expect(resume).toContain('5 000 FCFA');
  });

  it('applique le tarif du véhicule choisi', async () => {
    const fixture = await ouvrir();
    const composant = fixture.componentInstance as unknown as { form: { patchValue(v: unknown): void } };
    composant.form.patchValue({ departZoneId: '2', destinationZoneId: '5', vehicule: 'camionnette' });
    await fixture.whenStable();

    // 16,7 km × 800 FCFA, bien au-dessus du minimum de 5 000 FCFA
    expect((fixture.nativeElement as HTMLElement).querySelector('.recu')?.textContent)
      .toContain('13 400 FCFA');
  });

  it('refuse un numéro de téléphone invalide', async () => {
    const fixture = await ouvrir();
    const composant = fixture.componentInstance as unknown as {
      form: { patchValue(v: unknown): void; invalid: boolean };
    };
    composant.form.patchValue({
      clientNom: 'Awa Diop', clientTelephone: '123', departZoneId: '2', destinationZoneId: '5'
    });
    expect(composant.form.invalid).toBe(true);

    composant.form.patchValue({ clientTelephone: '77 000 00 09' });
    expect(composant.form.invalid).toBe(false);
  });
});
