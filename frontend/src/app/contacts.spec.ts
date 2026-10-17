import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Contacts } from './contacts';

describe('Contacts', () => {
  let http: HttpTestingController;
  let contacts: Contacts;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    contacts = TestBed.inject(Contacts);
  });

  it('demande le numéro au serveur, un profil à la fois', () => {
    contacts.appeler('artisan', 7);

    const req = http.expectOne('/api/contacts/artisan/7');
    expect(req.request.method).toBe('GET');
    expect(contacts.enCours()).toBe('artisan:7');
    req.flush({ telephone: '770000001' });
  });

  it('distingue les artisans des livreurs', () => {
    contacts.whatsapp('livreur', 3);
    http.expectOne('/api/contacts/livreur/3').flush({ telephone: '771000001' });
  });

  it('explique la limite horaire plutôt que de rester muet', () => {
    contacts.appeler('artisan', 7);
    http.expectOne('/api/contacts/artisan/7')
      .flush({}, { status: 429, statusText: 'Too Many Requests' });

    expect(contacts.erreur()).toContain('Trop de numéros demandés');
    expect(contacts.enCours()).toBeNull();
  });

  it('signale un profil devenu introuvable', () => {
    contacts.appeler('artisan', 99);
    http.expectOne('/api/contacts/artisan/99')
      .flush({}, { status: 404, statusText: 'Not Found' });

    expect(contacts.erreur()).not.toBeNull();
    expect(contacts.enCours()).toBeNull();
  });

  it('efface l\'erreur précédente avant une nouvelle tentative', () => {
    contacts.appeler('artisan', 7);
    http.expectOne('/api/contacts/artisan/7').flush({}, { status: 404, statusText: 'Not Found' });
    expect(contacts.erreur()).not.toBeNull();

    contacts.appeler('artisan', 8);
    expect(contacts.erreur()).toBeNull();
    http.expectOne('/api/contacts/artisan/8').flush({ telephone: '770000002' });
  });
});
