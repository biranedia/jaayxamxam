import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Inscription } from './inscription';

const metiers = [
  { id: 1, nom: 'Plombier', icone: 'plombier', famille: 'batiment' },
  { id: 12, nom: 'Ménage', icone: 'menage', famille: 'maison' }
];

// jsdom ne fournit pas l'aperçu d'un fichier local : on le remplace le temps des tests
beforeAll(() => {
  URL.createObjectURL = () => 'blob:apercu';
  URL.revokeObjectURL = () => undefined;
});

describe('Inscription', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Inscription],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  async function ouvrir() {
    const fixture = TestBed.createComponent(Inscription);
    fixture.detectChanges();
    http.expectOne('/api/metiers').flush(metiers);
    http.expectOne('/api/zones').flush([{ id: 2, ville: 'Dakar', quartier: 'Médina' }]);
    await fixture.whenStable();
    return fixture;
  }

  const bouton = (fixture: { nativeElement: unknown }, texte: string) =>
    [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')]
      .find(b => b.textContent?.includes(texte)) as HTMLButtonElement;

  it('demande d\'abord quel métier on exerce', async () => {
    const el = (await ouvrir()).nativeElement as HTMLElement;
    expect(el.textContent).toContain('Je suis artisan');
    expect(el.textContent).toContain('Je livre ou je transporte');
    expect(el.querySelector('form')).toBeNull();
  });

  it('ouvre le formulaire artisan avec ses métiers rangés par famille', async () => {
    const fixture = await ouvrir();
    bouton(fixture, 'Je suis artisan').click();
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('form')).not.toBeNull();
    const groupes = [...el.querySelectorAll('optgroup')].map(g => g.label);
    expect(groupes).toContain('Bâtiment');
    expect(groupes).toContain('Maison');
  });

  it('inscrit un artisan et affiche son code de gestion', async () => {
    const fixture = await ouvrir();
    bouton(fixture, 'Je suis artisan').click();
    await fixture.whenStable();

    const composant = fixture.componentInstance as unknown as { formArtisan: { patchValue(v: unknown): void } };
    composant.formArtisan.patchValue({
      nom: 'Moussa Ndiaye', telephone: '77 000 00 01', metierId: '1', zoneId: '2',
      anneesExperience: 8, tarifIndicatif: '5000', description: 'Fuites et WC'
    });
    await fixture.whenStable();
    bouton(fixture, "M'inscrire").click();

    const req = http.expectOne('/api/inscriptions/artisan');
    expect(req.request.body.telephone).toBe('77 000 00 01');
    expect(req.request.body.metierId).toBe(1);
    expect(req.request.body.tarifIndicatif).toBe(5000);
    req.flush({ id: 10, codeGestion: 'K7M2PQX4', statut: 'EN_ATTENTE' });
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.code')?.textContent).toContain('K7M2PQX4');
    expect(el.textContent).toContain("n'est pas encore visible");
  });

  it('propose la charge habituelle du véhicule choisi', async () => {
    const fixture = await ouvrir();
    bouton(fixture, 'Je livre').click();
    await fixture.whenStable();

    const composant = fixture.componentInstance as unknown as {
      formLivreur: { getRawValue(): { chargeMaxKg: number; vehicule: string } };
    };
    expect(composant.formLivreur.getRawValue().chargeMaxKg).toBe(30);

    bouton(fixture, 'Camionnette').click();
    await fixture.whenStable();
    expect(composant.formLivreur.getRawValue().vehicule).toBe('camionnette');
    expect(composant.formLivreur.getRawValue().chargeMaxKg).toBe(1200);
  });

  it('laisse les prix vides : le serveur applique le tarif du véhicule', async () => {
    const fixture = await ouvrir();
    bouton(fixture, 'Je livre').click();
    await fixture.whenStable();

    const composant = fixture.componentInstance as unknown as { formLivreur: { patchValue(v: unknown): void } };
    composant.formLivreur.patchValue({ nom: 'Pape Sow', telephone: '771000001' });
    await fixture.whenStable();
    bouton(fixture, "M'inscrire").click();

    const req = http.expectOne('/api/inscriptions/livreur');
    expect(req.request.body.prixParKm).toBeUndefined();
    expect(req.request.body.chargeMaxKg).toBe(30);
    req.flush({ id: 20, codeGestion: 'R4TB9HCN', statut: 'EN_ATTENTE' });
  });

  it('explique clairement qu\'un numéro déjà inscrit est refusé', async () => {
    const fixture = await ouvrir();
    bouton(fixture, 'Je suis artisan').click();
    await fixture.whenStable();

    const composant = fixture.componentInstance as unknown as { formArtisan: { patchValue(v: unknown): void } };
    composant.formArtisan.patchValue({
      nom: 'Moussa', telephone: '770000001', metierId: '1', zoneId: '2', anneesExperience: 8
    });
    await fixture.whenStable();
    bouton(fixture, "M'inscrire").click();
    http.expectOne('/api/inscriptions/artisan').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('.erreur')?.textContent)
      .toContain('déjà inscrit');
  });

  it('propose d\'ajouter une photo une fois l\'inscription envoyée', async () => {
    const fixture = await ouvrir();
    bouton(fixture, 'Je suis artisan').click();
    await fixture.whenStable();

    const composant = fixture.componentInstance as unknown as {
      formArtisan: { patchValue(v: unknown): void };
      choisirPhoto(e: Event): void;
      envoyerPhoto(): void;
    };
    composant.formArtisan.patchValue({
      nom: 'Moussa', telephone: '770000001', metierId: '1', zoneId: '2', anneesExperience: 8
    });
    await fixture.whenStable();
    bouton(fixture, "M'inscrire").click();
    http.expectOne('/api/inscriptions/artisan')
      .flush({ id: 10, codeGestion: 'AM9TQ4KBX', statut: 'EN_ATTENTE' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('input[type=file]')).not.toBeNull();

    // La photo part avec le code de gestion : c'est lui qui prouve que le profil est le sien
    const photo = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'moi.jpg', { type: 'image/jpeg' });
    composant.choisirPhoto({ target: { files: [photo] } } as unknown as Event);
    await fixture.whenStable();
    composant.envoyerPhoto();

    const req = http.expectOne('/api/photos/artisan');
    expect(req.request.body.get('code')).toBe('AM9TQ4KBX');
    expect((req.request.body.get('fichier') as File).name).toBe('moi.jpg');
    req.flush({ photo: 'a1b2c3d4e5f60718293a4b5c6d7e8f90.jpg' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Photo enregistrée');
  });

  it('refuse un fichier qui n\'est pas une photo, sans appeler le serveur', async () => {
    const fixture = await ouvrir();
    bouton(fixture, 'Je suis artisan').click();
    await fixture.whenStable();

    const composant = fixture.componentInstance as unknown as { choisirPhoto(e: Event): void };
    const document = new File(['%PDF-1.4'], 'facture.pdf', { type: 'application/pdf' });
    composant.choisirPhoto({ target: { files: [document] } } as unknown as Event);
    await fixture.whenStable();

    http.expectNone('/api/photos/artisan');
  });

  it('n\'envoie rien tant que le numéro est invalide', async () => {
    const fixture = await ouvrir();
    bouton(fixture, 'Je suis artisan').click();
    await fixture.whenStable();

    const composant = fixture.componentInstance as unknown as { formArtisan: { patchValue(v: unknown): void } };
    composant.formArtisan.patchValue({
      nom: 'Moussa', telephone: '123', metierId: '1', zoneId: '2', anneesExperience: 8
    });
    await fixture.whenStable();
    bouton(fixture, "M'inscrire").click();

    http.expectNone('/api/inscriptions/artisan');
  });
});
