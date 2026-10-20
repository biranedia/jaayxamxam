import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Admin } from './admin';

const bord = {
  artisansVisibles: 6, livreursVisibles: 4, enAttente: 2, refuses: 1,
  demandesTotal: 12, demandesSansReponse: 3, coursesTotal: 5, coursesSansReponse: 0,
  sessionsOuvertes: 1,
  alertes: [
    { niveau: 'attention', texte: '2 inscriptions attendent votre validation.' },
    { niveau: 'urgent', texte: '3 clients attendent depuis plus de 48 h sans qu\'on leur réponde.' }
  ]
};

describe('Back-office', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Admin],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Au démarrage, la page demande au serveur si le cookie est encore valable. */
  function repondreMoi(connecte: boolean): void {
    http.expectOne(r => r.url === '/api/admin/moi')
      .flush({ connecte, minutesInactivite: 45, sessionsOuvertes: connecte ? 1 : 0 });
  }

  async function connecter() {
    const fixture = TestBed.createComponent(Admin);
    await fixture.whenStable();
    repondreMoi(false);
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('input[type=password]') as HTMLInputElement).value = 'ma phrase de passe';
    el.querySelector('.console-porte form')!.dispatchEvent(new Event('submit'));

    http.expectOne(r => r.url === '/api/admin/connexion')
      .flush({ connecte: true, minutesInactivite: 45, sessionsOuvertes: 1 });
    await fixture.whenStable();

    http.expectOne(r => r.url === '/api/admin/tableau-de-bord').flush(bord);
    await fixture.whenStable();
    return fixture;
  }

  it('demande le mot de passe avant toute chose', async () => {
    const fixture = TestBed.createComponent(Admin);
    await fixture.whenStable();
    repondreMoi(false);
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('input[type=password]')).toBeTruthy();
    expect(el.querySelector('.console-nav')).toBeNull();
  });

  /**
   * Le mot de passe part une seule fois, dans le corps. Rien ne doit se retrouver dans l'adresse,
   * ni être conservé pour les appels suivants.
   */
  it('envoie le mot de passe dans le corps, avec les cookies', async () => {
    const fixture = TestBed.createComponent(Admin);
    await fixture.whenStable();
    repondreMoi(false);
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('input[type=password]') as HTMLInputElement).value = 'ma phrase de passe';
    el.querySelector('.console-porte form')!.dispatchEvent(new Event('submit'));

    const req = http.expectOne(r => r.url === '/api/admin/connexion');
    expect(req.request.body).toEqual({ motDePasse: 'ma phrase de passe' });
    expect(req.request.url).not.toContain('ma phrase');
    expect(req.request.withCredentials).toBe(true);

    req.flush({ connecte: true, minutesInactivite: 45, sessionsOuvertes: 1 });
    await fixture.whenStable();
    http.expectOne(r => r.url === '/api/admin/tableau-de-bord').flush(bord);
    await fixture.whenStable();
  });

  it('affiche les alertes avant les chiffres', async () => {
    const el = (await connecter()).nativeElement as HTMLElement;
    const alertes = [...el.querySelectorAll('.console-alerte')].map(a => a.textContent?.trim());
    expect(alertes[0]).toContain('2 inscriptions attendent');
    expect(el.querySelector('.console-alerte.urgent')?.textContent).toContain('3 clients attendent');
    expect(el.querySelectorAll('.console-vignette dd').length).toBe(8);
  });

  /** La deuxième serrure contre le CSRF : sans cet en-tête le serveur refuse toute action. */
  it('pose l\'en-tête anti-CSRF et les cookies sur chaque appel', async () => {
    const fixture = await connecter();
    const el = fixture.nativeElement as HTMLElement;

    const ongletAttente = [...el.querySelectorAll('.console-nav button')]
      .find(b => b.textContent?.includes('À valider')) as HTMLButtonElement;
    ongletAttente.click();

    const req = http.expectOne(r => r.url === '/api/admin/attente');
    expect(req.request.headers.get('X-Admin-Requete')).toBe('1');
    expect(req.request.withCredentials).toBe(true);
    req.flush({ artisansEnAttente: 0, livreursEnAttente: 0, artisansActifs: 6, livreursActifs: 4, file: [] });
    await fixture.whenStable();
  });

  /** Une session expirée ramène à la connexion : afficher des tableaux vides induirait en erreur. */
  it('revient à la connexion quand la session a expiré', async () => {
    const fixture = await connecter();
    const el = fixture.nativeElement as HTMLElement;

    const ongletJournal = [...el.querySelectorAll('.console-nav button')]
      .find(b => b.textContent?.includes('Journal')) as HTMLButtonElement;
    ongletJournal.click();

    http.expectOne(r => r.url === '/api/admin/journal')
      .flush({ message: 'Session expirée. Reconnectez-vous.' },
        { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(el.querySelector('input[type=password]')).toBeTruthy();
    expect(el.querySelector('.console-erreur')?.textContent).toContain('Session expirée');
  });

  /** Le code de gestion ouvre l'espace d'un travailleur : il ne s'affiche pas tout seul. */
  it('cache le code de gestion tant qu\'on ne le demande pas', async () => {
    const fixture = await connecter();
    const el = fixture.nativeElement as HTMLElement;

    ([...el.querySelectorAll('.console-nav button')]
      .find(b => b.textContent?.includes('Travailleurs')) as HTMLButtonElement).click();

    http.expectOne(r => r.url.startsWith('/api/admin/travailleurs')).flush([{
      id: 1, type: 'artisan', nom: 'Moussa Ndiaye', telephone: '770000001',
      detail: 'Plombier · Médina', etat: 'VISIBLE', verifie: true,
      codeGestion: 'A7KM2QXP4', creeLe: new Date().toISOString()
    }]);
    await fixture.whenStable();

    expect(el.textContent).not.toContain('A7KM2QXP4');

    const voir = [...el.querySelectorAll('.console-bouton')]
      .find(b => b.textContent?.includes('Voir')) as HTMLButtonElement;
    voir.click();
    await fixture.whenStable();

    expect(el.textContent).toContain('A7KM2QXP4');
  });

  it('ferme la session et revient au mot de passe', async () => {
    const fixture = await connecter();
    const el = fixture.nativeElement as HTMLElement;

    ([...el.querySelectorAll('.console-bouton')]
      .find(b => b.textContent?.includes('Quitter')) as HTMLButtonElement).click();

    http.expectOne(r => r.url === '/api/admin/deconnexion')
      .flush({ connecte: false, minutesInactivite: 45, sessionsOuvertes: 0 });
    await fixture.whenStable();

    expect(el.querySelector('input[type=password]')).toBeTruthy();
  });
});
