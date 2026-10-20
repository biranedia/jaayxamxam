import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Espace } from './espace';

const CODE = 'A7KM2QXP4';

const ilYa = (min: number) => new Date(Date.now() - min * 60_000).toISOString();

function profil(extra: Record<string, unknown> = {}) {
  return {
    type: 'artisan', id: 1, nom: 'Moussa Ndiaye', telephone: '770000001',
    verifie: true, actif: true, refuse: false, etat: 'VALIDE',
    metier: 'Plombier', metierIcone: 'plombier', metierFamille: 'batiment',
    quartier: 'Médina', ville: 'Dakar', travaux: [], ...extra
  };
}

const demande = {
  id: 42, codeSuivi: 'DK4M2P9TX', statut: 'NOUVELLE', clientNom: 'Awa Diop',
  clientTelephone: '770000055', description: 'Fuite sous l\'évier', urgente: true,
  lieu: 'Médina, Dakar', creeLe: ilYa(30)
};

describe('Espace travailleur', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    try { localStorage.clear(); } catch { /* sans importance */ }
    await TestBed.configureTestingModule({
      imports: [Espace],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** La page demande sa configuration au démarrage ; on y répond une fois pour toutes. */
  function servirConfig(): void {
    http.expectOne(r => r.url === '/api/config').flush({ whatsappVerification: '' });
  }

  async function ouvrirAvec(corps: Record<string, unknown>) {
    const fixture = TestBed.createComponent(Espace);
    await fixture.whenStable();
    servirConfig();

    const el = fixture.nativeElement as HTMLElement;
    const champ = el.querySelector('.saisie-code') as HTMLInputElement;
    champ.value = CODE;
    champ.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    el.querySelector('.formulaire-code')!.dispatchEvent(new Event('submit'));
    http.expectOne(r => r.url === '/api/espace' && r.method === 'POST').flush(corps);
    await fixture.whenStable();
    return fixture;
  }

  it('demande un code avant de montrer quoi que ce soit', async () => {
    const fixture = TestBed.createComponent(Espace);
    await fixture.whenStable();
    servirConfig();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.saisie-code')).toBeTruthy();
    expect(el.querySelector('.interrupteur')).toBeNull();
  });

  it('ouvre l\'atelier et affiche le nom du travailleur', async () => {
    const el = (await ouvrirAvec(profil())).nativeElement as HTMLElement;
    expect(el.querySelector('.banniere-atelier h1')?.textContent).toContain('Moussa Ndiaye');
    expect(el.querySelector('.interrupteur')).toBeTruthy();
  });

  // Le code envoyé ne doit jamais se retrouver dans l'adresse : c'est le seul secret du travailleur
  it('envoie le code dans le corps de la requête, pas dans l\'URL', async () => {
    const fixture = TestBed.createComponent(Espace);
    await fixture.whenStable();
    servirConfig();

    const el = fixture.nativeElement as HTMLElement;
    const champ = el.querySelector('.saisie-code') as HTMLInputElement;
    champ.value = CODE;
    champ.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    el.querySelector('.formulaire-code')!.dispatchEvent(new Event('submit'));

    const requete = http.expectOne(r => r.method === 'POST' && r.url === '/api/espace');
    expect(requete.request.url).not.toContain(CODE);
    expect(requete.request.body).toEqual({ code: CODE });
    requete.flush(profil());
    await fixture.whenStable();
  });

  it('montre l\'interrupteur éteint quand personne ne s\'est déclaré', async () => {
    const el = (await ouvrirAvec(profil())).nativeElement as HTMLElement;
    expect(el.querySelector('.interrupteur.allume')).toBeNull();
    expect(el.querySelector('.bascule')?.textContent).toContain('Je commence');
  });

  it('montre l\'interrupteur allumé après une déclaration du matin', async () => {
    // Sept heures : avec l'ancienne fenêtre de dix minutes, il serait éteint
    const el = (await ouvrirAvec(profil({ derniereActivite: ilYa(7 * 60) })))
      .nativeElement as HTMLElement;
    expect(el.querySelector('.interrupteur.allume')).toBeTruthy();
    expect(el.querySelector('.bascule')?.textContent).toContain('Je m\'arrête');
  });

  it('interdit de se déclarer et explique pourquoi quand le profil attend sa validation', async () => {
    const el = (await ouvrirAvec(profil({ actif: false, etat: 'EN_ATTENTE' })))
      .nativeElement as HTMLElement;
    expect(el.querySelector('.bandeau-etat.attente')).toBeTruthy();
    // Pas d'interrupteur du tout : on ne montre pas un bouton qui renverra 403
    expect(el.querySelector('.interrupteur')).toBeNull();
  });

  it('classe le travail reçu : à répondre, en cours, terminé', async () => {
    const el = (await ouvrirAvec(profil({
      travaux: [
        demande,
        { ...demande, id: 43, statut: 'ACCEPTEE', urgente: false },
        { ...demande, id: 44, statut: 'TERMINEE', urgente: false }
      ]
    }))).nativeElement as HTMLElement;

    const groupes = [...el.querySelectorAll('.sous-titre-groupe')].map(g => g.textContent?.trim());
    expect(groupes).toEqual(['À répondre (1)', 'En cours (1)', 'Terminé (1)']);
    expect(el.querySelector('.fiche-travail.urgent .marque-urgent')?.textContent).toContain('Urgent');
  });

  it('accepte une demande et renvoie le code avec le nouvel état', async () => {
    const fixture = await ouvrirAvec(profil({ travaux: [demande] }));
    const el = fixture.nativeElement as HTMLElement;

    const accepter = [...el.querySelectorAll('.fiche-travail .bouton')]
      .find(b => b.textContent?.includes('J\'accepte')) as HTMLButtonElement;
    accepter.click();

    const requete = http.expectOne(r => r.url === '/api/espace/travaux/42/statut');
    expect(requete.request.body).toEqual({ code: CODE, statut: 'ACCEPTEE' });
    requete.flush(profil({ travaux: [{ ...demande, statut: 'ACCEPTEE' }] }));
    await fixture.whenStable();

    expect(el.querySelector('.fiche-travail.en-cours')).toBeTruthy();
  });

  it('affiche le message du serveur plutôt qu\'une formule générique', async () => {
    const fixture = TestBed.createComponent(Espace);
    await fixture.whenStable();
    servirConfig();

    const el = fixture.nativeElement as HTMLElement;
    const champ = el.querySelector('.saisie-code') as HTMLInputElement;
    champ.value = 'ZZZZZZZZZ';
    champ.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    el.querySelector('.formulaire-code')!.dispatchEvent(new Event('submit'));

    http.expectOne(r => r.url === '/api/espace').flush(
      { message: 'Code inconnu. Vérifiez celui reçu à l\'inscription.' },
      { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(el.querySelector('.erreur')?.textContent).toContain('Code inconnu');
  });

  it('oublie un code refusé pour ne pas boucler à la prochaine visite', async () => {
    try { localStorage.setItem('jaayxamxam.code', 'PERIME99'); } catch { /* sans importance */ }

    const fixture = TestBed.createComponent(Espace);
    await fixture.whenStable();
    servirConfig();
    // Le code rangé déclenche une tentative d'ouverture automatique
    http.expectOne(r => r.url === '/api/espace')
      .flush({ message: 'Code inconnu.' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(localStorage.getItem('jaayxamxam.code')).toBeNull();
  });
});
