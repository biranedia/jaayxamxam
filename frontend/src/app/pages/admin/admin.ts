import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { messageHttp, urlPhoto } from '../../utils';

export interface EnAttente {
  id: number;
  type: 'artisan' | 'livreur';
  nom: string;
  telephone: string;
  detail: string;
  photo?: string;
  creeLe: string;
}

export interface Resume {
  artisansEnAttente: number;
  livreursEnAttente: number;
  artisansActifs: number;
  livreursActifs: number;
  file: EnAttente[];
}

export interface Alerte { niveau: 'urgent' | 'attention' | 'calme'; texte: string; }

export interface TableauDeBord {
  artisansVisibles: number;
  livreursVisibles: number;
  enAttente: number;
  refuses: number;
  demandesTotal: number;
  demandesSansReponse: number;
  coursesTotal: number;
  coursesSansReponse: number;
  sessionsOuvertes: number;
  alertes: Alerte[];
}

export interface Travailleur {
  id: number;
  type: 'artisan' | 'livreur';
  nom: string;
  telephone: string;
  detail: string;
  etat: 'VISIBLE' | 'EN_ATTENTE' | 'REFUSE';
  verifie: boolean;
  photo?: string;
  codeGestion?: string;
  derniereActivite?: string;
  creeLe: string;
}

export interface TravailAdmin {
  id: number;
  type: 'demande' | 'course';
  codeSuivi: string;
  statut: string;
  clientNom: string;
  clientTelephone: string;
  resume: string;
  assigneA?: string;
  creeLe: string;
}

export interface AvisAdmin {
  id: number;
  note: number;
  commentaire?: string;
  masque: boolean;
  surQui: string;
  creeLe: string;
}

export interface LigneJournal {
  id: number;
  adresse?: string;
  action: string;
  cible?: string;
  detail?: string;
  faiteLe: string;
}

export interface EtatAdmin { connecte: boolean; minutesInactivite: number; sessionsOuvertes: number; }

export interface Candidat {
  id: number; nom: string; detail: string; verifie: boolean; disponible: boolean;
}

export interface MetierAdmin {
  id: number; nom: string; famille: string; icone?: string; actif: boolean; artisans: number;
}

type Onglet = 'bord' | 'attente' | 'travaux' | 'travailleurs' | 'metiers' | 'avis' | 'journal';

/**
 * Back-office : le poste de pilotage de la plateforme.
 *
 * <p>La session vit dans un cookie {@code HttpOnly} posé par le serveur : ce code ne peut ni le
 * lire ni l'écrire, et c'est exactement le but — une faille d'affichage ailleurs dans
 * l'application ne permettrait pas de voler l'accès. Le navigateur renvoie le cookie tout seul,
 * à condition que les requêtes partent avec {@code withCredentials}.
 *
 * <p>Le mot de passe, lui, ne quitte jamais le champ de saisie : il part une fois à la connexion
 * et n'est gardé nulle part, ni en mémoire ni dans le navigateur.
 */
@Component({
  selector: 'app-admin',
  imports: [DatePipe],
  templateUrl: './admin.html'
})
export class Admin {
  private readonly http = inject(HttpClient);

  protected readonly connecte = signal(false);
  protected readonly etat = signal<EtatAdmin | null>(null);
  protected readonly onglet = signal<Onglet>('bord');
  protected readonly erreur = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly enCours = signal(false);

  protected readonly bord = signal<TableauDeBord | null>(null);
  protected readonly resume = signal<Resume | null>(null);
  protected readonly travailleurs = signal<Travailleur[]>([]);
  protected readonly travaux = signal<TravailAdmin[]>([]);
  protected readonly avis = signal<AvisAdmin[]>([]);
  protected readonly journal = signal<LigneJournal[]>([]);

  protected readonly filtreEtat = signal('');
  protected readonly recherche = signal('');
  protected readonly codeRevele = signal<number | null>(null);
  protected readonly nouveauCode = signal<string | null>(null);

  protected readonly metiers = signal<MetierAdmin[]>([]);
  /** Travail dont on est en train de choisir le destinataire. */
  protected readonly confieEnCours = signal<TravailAdmin | null>(null);
  protected readonly candidats = signal<Candidat[]>([]);
  protected readonly etats = ['NOUVELLE', 'ACCEPTEE', 'EN_ROUTE', 'TERMINEE', 'REFUSEE'];

  protected readonly urlPhoto = urlPhoto;

  protected readonly onglets: { cle: Onglet; nom: string }[] = [
    { cle: 'bord', nom: 'Tableau de bord' },
    { cle: 'attente', nom: 'À valider' },
    { cle: 'travaux', nom: 'Demandes' },
    { cle: 'travailleurs', nom: 'Travailleurs' },
    { cle: 'metiers', nom: 'Métiers' },
    { cle: 'avis', nom: 'Avis' },
    { cle: 'journal', nom: 'Journal' }
  ];

  /** Compteur affiché sur l'onglet : ce qui attend se voit sans y aller. */
  protected compteur(cle: Onglet): number {
    const b = this.bord();
    if (!b) return 0;
    if (cle === 'attente') return b.enAttente;
    if (cle === 'travaux') return b.demandesSansReponse + b.coursesSansReponse;
    return 0;
  }

  /** Un travail que personne n'a pris : c'est là qu'il faut intervenir. */
  protected abandonne(t: TravailAdmin): boolean {
    return t.statut === 'NOUVELLE' && !t.assigneA;
  }

  protected readonly alertesVives = computed(
    () => (this.bord()?.alertes ?? []).filter(a => a.niveau !== 'calme')
  );

  constructor() {
    // Le cookie survit au rechargement : on demande au serveur s'il est encore valable
    this.http.get<EtatAdmin>('/api/admin/moi', { withCredentials: true }).subscribe({
      next: e => { this.etat.set(e); if (e.connecte) { this.connecte.set(true); this.charger(); } },
      error: () => undefined
    });
  }

  // ---------- Connexion ----------

  protected connexion(motDePasse: string, evenement: Event): void {
    evenement.preventDefault();
    const phrase = motDePasse.trim();
    if (!phrase) return;

    this.enCours.set(true);
    this.erreur.set(null);
    this.http.post<EtatAdmin>('/api/admin/connexion', { motDePasse: phrase },
      { withCredentials: true }).subscribe({
      next: e => {
        this.etat.set(e);
        this.connecte.set(true);
        this.enCours.set(false);
        this.charger();
      },
      error: e => { this.enCours.set(false); this.erreur.set(messageHttp(e)); }
    });
  }

  protected deconnexion(): void {
    this.http.post<EtatAdmin>('/api/admin/deconnexion', null, this.options()).subscribe({
      next: () => this.reinitialiser(),
      error: () => this.reinitialiser()
    });
  }

  /** Ferme toutes les sessions, y compris celles ouvertes ailleurs. */
  protected fermerTout(): void {
    this.http.post<EtatAdmin>('/api/admin/fermer-tout', null, this.options()).subscribe({
      next: () => this.reinitialiser(),
      error: () => this.reinitialiser()
    });
  }

  private reinitialiser(): void {
    this.connecte.set(false);
    this.bord.set(null);
    this.resume.set(null);
    this.travailleurs.set([]);
    this.travaux.set([]);
    this.avis.set([]);
    this.journal.set([]);
    this.message.set(null);
  }

  // ---------- Chargement ----------

  protected choisirOnglet(cle: Onglet): void {
    this.onglet.set(cle);
    this.message.set(null);
    this.nouveauCode.set(null);
    this.charger();
  }

  protected charger(): void {
    this.enCours.set(true);
    this.erreur.set(null);

    const fini = () => this.enCours.set(false);
    const echec = (e: unknown) => {
      fini();
      // 401 = session expirée : on repasse à l'écran de connexion plutôt que d'afficher du vide
      if ((e as { status?: number })?.status === 401) this.reinitialiser();
      this.erreur.set(messageHttp(e));
    };

    switch (this.onglet()) {
      case 'bord':
        this.http.get<TableauDeBord>('/api/admin/tableau-de-bord', this.options())
          .subscribe({ next: b => { this.bord.set(b); fini(); }, error: echec });
        break;
      case 'attente':
        this.http.get<Resume>('/api/admin/attente', this.options())
          .subscribe({ next: r => { this.resume.set(r); fini(); }, error: echec });
        break;
      case 'travailleurs': {
        let url = '/api/admin/travailleurs';
        const params: string[] = [];
        if (this.filtreEtat()) params.push('etat=' + encodeURIComponent(this.filtreEtat()));
        if (this.recherche().trim()) params.push('q=' + encodeURIComponent(this.recherche().trim()));
        if (params.length) url += '?' + params.join('&');
        this.http.get<Travailleur[]>(url, this.options())
          .subscribe({ next: t => { this.travailleurs.set(t); fini(); }, error: echec });
        break;
      }
      case 'travaux':
        this.http.get<TravailAdmin[]>('/api/admin/travaux', this.options())
          .subscribe({ next: t => { this.travaux.set(t); fini(); }, error: echec });
        break;
      case 'avis':
        this.http.get<AvisAdmin[]>('/api/admin/avis', this.options())
          .subscribe({ next: a => { this.avis.set(a); fini(); }, error: echec });
        break;
      case 'metiers':
        this.http.get<MetierAdmin[]>('/api/admin/metiers', this.options())
          .subscribe({ next: m => { this.metiers.set(m); fini(); }, error: echec });
        break;
      case 'journal':
        this.http.get<LigneJournal[]>('/api/admin/journal', this.options())
          .subscribe({ next: j => { this.journal.set(j); fini(); }, error: echec });
        break;
    }
  }

  // ---------- Actions ----------

  protected agir(profil: EnAttente | Travailleur, action: string, valeur?: boolean): void {
    this.enCours.set(true);
    this.message.set(null);
    const suffixe = valeur === false ? '?valeur=false' : '';
    this.http.post(`/api/admin/${profil.type}/${profil.id}/${action}${suffixe}`, null, this.options())
      .subscribe({
        next: () => { this.message.set(`${profil.nom} : ${this.libelle(action, valeur)}.`); this.charger(); },
        error: e => { this.enCours.set(false); this.erreur.set(messageHttp(e)); }
      });
  }

  protected masquerAvis(a: AvisAdmin): void {
    this.enCours.set(true);
    this.http.post(`/api/admin/avis/${a.id}/masquer?valeur=${!a.masque}`, null, this.options())
      .subscribe({
        next: () => { this.message.set(a.masque ? 'Avis réaffiché.' : 'Avis masqué.'); this.charger(); },
        error: e => { this.enCours.set(false); this.erreur.set(messageHttp(e)); }
      });
  }

  /** Nouveau code de gestion : l'ancien cesse immédiatement de fonctionner. */
  protected regenererCode(t: Travailleur): void {
    this.enCours.set(true);
    this.nouveauCode.set(null);
    this.http.post<{ codeGestion: string }>(
      `/api/admin/${t.type}/${t.id}/code`, null, this.options()).subscribe({
      next: r => {
        this.nouveauCode.set(r.codeGestion);
        this.message.set(`Nouveau code pour ${t.nom}. L'ancien ne marche plus : donnez-lui celui-ci.`);
        this.charger();
      },
      error: e => { this.enCours.set(false); this.erreur.set(messageHttp(e)); }
    });
  }

  // ---------- Interventions ----------

  /**
   * Ouvre le choix du destinataire pour un travail, et demande au serveur qui peut le prendre.
   * Le serveur range les disponibles en premier : c'est l'ordre dans lequel on téléphonerait.
   */
  protected ouvrirConfier(t: TravailAdmin): void {
    if (this.confieEnCours()?.id === t.id && this.confieEnCours()?.type === t.type) {
      this.confieEnCours.set(null);
      return;
    }
    this.confieEnCours.set(t);
    this.candidats.set([]);
    this.http.get<Candidat[]>(`/api/admin/${t.type}/${t.id}/candidats`, this.options())
      .subscribe({
        next: c => this.candidats.set(c),
        error: e => this.erreur.set(messageHttp(e))
      });
  }

  protected confier(t: TravailAdmin, c: Candidat): void {
    this.enCours.set(true);
    this.http.post<{ message: string }>(`/api/admin/${t.type}/${t.id}/confier`,
      { travailleurId: c.id }, this.options()).subscribe({
      next: r => { this.message.set(r.message); this.confieEnCours.set(null); this.charger(); },
      error: e => { this.enCours.set(false); this.erreur.set(messageHttp(e)); }
    });
  }

  /** Pose un état à la main : pour clore ce qui s'est réglé au téléphone, ou corriger une erreur. */
  protected changerEtat(t: TravailAdmin, etat: string): void {
    if (!etat || etat === t.statut) return;
    this.enCours.set(true);
    this.http.post<{ message: string }>(
      `/api/admin/${t.type}/${t.id}/etat?valeur=${encodeURIComponent(etat)}`,
      null, this.options()).subscribe({
      next: r => { this.message.set(r.message); this.charger(); },
      error: e => { this.enCours.set(false); this.erreur.set(messageHttp(e)); }
    });
  }

  protected basculerMetier(m: MetierAdmin): void {
    this.enCours.set(true);
    this.http.post<{ message: string }>(`/api/admin/metier/${m.id}/actif?valeur=${!m.actif}`,
      null, this.options()).subscribe({
      next: r => { this.message.set(r.message); this.charger(); },
      error: e => { this.enCours.set(false); this.erreur.set(messageHttp(e)); }
    });
  }

  /** Lien WhatsApp vers un numéro sénégalais, pour relancer sans recopier le numéro. */
  protected whatsapp(telephone: string): string {
    const chiffres = String(telephone ?? '').replace(/\D/g, '');
    const complet = chiffres.length === 9 ? '221' + chiffres : chiffres;
    return 'https://wa.me/' + complet;
  }

  protected revelerCode(id: number): void {
    this.codeRevele.set(this.codeRevele() === id ? null : id);
  }

  protected chercher(texte: string): void {
    this.recherche.set(texte);
    if (this.onglet() === 'travailleurs') this.charger();
  }

  protected filtrer(etat: string): void {
    this.filtreEtat.set(etat);
    this.charger();
  }

  private libelle(action: string, valeur?: boolean): string {
    if (action === 'valider') return valeur === false ? 'retiré de la recherche' : 'rendu visible';
    if (action === 'refuser') return 'inscription écartée';
    if (action === 'verifier') return valeur === false ? 'badge retiré' : 'badge « Vérifié » accordé';
    return 'action effectuée';
  }

  /**
   * {@code withCredentials} envoie le cookie de session ; l'en-tête est la deuxième serrure
   * contre le CSRF — un formulaire déclenché depuis un autre site ne peut pas le poser.
   */
  private options() {
    return {
      withCredentials: true,
      headers: new HttpHeaders({ 'X-Admin-Requete': '1' })
    };
  }
}
