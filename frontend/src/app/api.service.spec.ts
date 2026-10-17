import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envoie les filtres de recherche', () => {
    api.artisans(2, 5).subscribe();
    const req = http.expectOne(r => r.url === '/api/artisans');
    expect(req.request.params.get('metierId')).toBe('2');
    expect(req.request.params.get('zoneId')).toBe('5');
    req.flush([]);
  });

  it('n\'envoie aucun paramètre sans filtre', () => {
    api.artisans().subscribe();
    const req = http.expectOne(r => r.url === '/api/artisans');
    expect(req.request.params.keys().length).toBe(0);
    req.flush([]);
  });

  it('charge la fiche d\'un artisan', () => {
    api.artisan(7).subscribe();
    http.expectOne('/api/artisans/7').flush({});
  });

  it('crée une demande', () => {
    const corps = {
      clientNom: 'Awa', clientTelephone: '770000001', description: 'Fuite',
      urgente: true, metierId: 1, zoneId: 2, artisanId: 3
    };
    api.creerDemande(corps).subscribe();
    const req = http.expectOne('/api/demandes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(corps);
    req.flush({ id: 1, codeSuivi: 'abc', statut: 'NOUVELLE' });
  });

  it('crée un avis', () => {
    api.creerAvis({ codeSuivi: 'abc', note: 5 }).subscribe();
    const req = http.expectOne('/api/avis');
    expect(req.request.method).toBe('POST');
    req.flush({ note: 5, creeLe: '2026-10-16T10:00:00' });
  });
});
