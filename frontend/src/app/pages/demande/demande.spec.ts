import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { FormulaireDemande } from './demande';

function saisir(el: HTMLElement, selecteur: string, valeur: string, evenement = 'input'): void {
  const champ = el.querySelector(selecteur) as HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;
  champ.value = valeur;
  champ.dispatchEvent(new Event(evenement));
}

describe('FormulaireDemande', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormulaireDemande],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  async function ouvrir() {
    const fixture = TestBed.createComponent(FormulaireDemande);
    fixture.componentRef.setInput('artisanId', '3');
    fixture.componentRef.setInput('metierId', '1');
    fixture.componentRef.setInput('zoneId', '2');
    fixture.detectChanges();
    http.expectOne('/api/metiers').flush([{ id: 1, nom: 'Plombier' }]);
    http.expectOne('/api/zones').flush([{ id: 2, ville: 'Dakar', quartier: 'Médina' }]);
    await fixture.whenStable();
    return fixture;
  }

  it('désactive l\'envoi tant que le formulaire est incomplet', async () => {
    const fixture = await ouvrir();
    const bouton = (fixture.nativeElement as HTMLElement).querySelector('form button') as HTMLButtonElement;
    expect(bouton.disabled).toBe(true);
  });

  it('envoie la demande liée à l\'artisan et affiche le code de suivi', async () => {
    const fixture = await ouvrir();
    const el = fixture.nativeElement as HTMLElement;

    saisir(el, 'input[formControlName="clientNom"]', 'Awa Diop');
    saisir(el, 'input[formControlName="clientTelephone"]', '77 000 00 09');
    saisir(el, 'textarea', 'Fuite sous l\'évier');
    await fixture.whenStable();

    const bouton = el.querySelector('form button') as HTMLButtonElement;
    expect(bouton.disabled).toBe(false);

    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    const req = http.expectOne('/api/demandes');
    expect(req.request.body).toMatchObject({
      clientNom: 'Awa Diop', clientTelephone: '77 000 00 09',
      metierId: 1, zoneId: 2, artisanId: 3, urgente: false
    });
    req.flush({ id: 10, codeSuivi: 'code-secret-123', statut: 'NOUVELLE' });
    await fixture.whenStable();

    expect(el.textContent).toContain('code-secret-123');
  });
});
