import { Component, computed, inject, signal } from '@angular/core';
import { ApiService, Config, Espace as EspaceDto, Travail } from '../../api.service';
import { Geo, estDisponible } from '../../geo';
import { Glyphe, teinte } from '../../glyphe';
import { fcfa, initiales, messageHttp, urlPhoto } from '../../utils';

/** Clé de rangement du code dans le navigateur : on ne le fait pas ressaisir chaque matin. */
const RANGEMENT = 'jaayxamxam.code';

@Component({
  selector: 'app-espace',
  imports: [Glyphe],
  templateUrl: './espace.html'
})
export class Espace {
  private readonly api = inject(ApiService);
  protected readonly geo = inject(Geo);

  protected readonly code = signal('');
  protected readonly profil = signal<EspaceDto | null>(null);
  protected readonly chargement = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly travailEnCours = signal<number | null>(null);
  protected readonly config = signal<Config | null>(null);

  protected readonly urlPhoto = urlPhoto;
  protected readonly initiales = initiales;
  protected readonly teinte = teinte;
  protected readonly fcfa = fcfa;
  protected readonly estDisponible = estDisponible;

  protected readonly disponible = computed(() => estDisponible(this.profil()?.derniereActivite));

  /** Le travail déjà traité descend en bas : ce qui demande une décision reste en haut. */
  protected readonly aTraiter = computed(
    () => (this.profil()?.travaux ?? []).filter(t => t.statut === 'NOUVELLE')
  );
  protected readonly enCours = computed(
    () => (this.profil()?.travaux ?? []).filter(t => t.statut === 'ACCEPTEE' || t.statut === 'EN_ROUTE')
  );
  protected readonly termines = computed(
    () => (this.profil()?.travaux ?? []).filter(t => t.statut === 'TERMINEE' || t.statut === 'REFUSEE')
  );

  constructor() {
    this.api.config().subscribe({ next: c => this.config.set(c), error: () => undefined });

    // Reconnexion silencieuse : si le code rangé n'est plus valable, on revient au formulaire
    const range = this.lireRangement();
    if (range) {
      this.code.set(range);
      this.ouvrir();
    }
  }

  protected ouvrir(evenement?: Event): void {
    evenement?.preventDefault();
    const code = this.code().trim();
    if (!code) return;

    this.chargement.set(true);
    this.erreur.set(null);
    this.api.ouvrirEspace(code).subscribe({
      next: p => { this.accepter(p); this.ecrireRangement(code); },
      error: e => { this.echec(e); this.effacerRangement(); }
    });
  }

  /**
   * « Je travaille aujourd'hui » : on demande la position une fois, puis on l'envoie.
   * Un seul appui, pas de suivi continu — voir le commentaire de estDisponible().
   */
  protected seDeclarer(): void {
    this.erreur.set(null);
    this.message.set(null);

    const envoyer = (lat: number, lon: number) => {
      this.chargement.set(true);
      this.api.envoyerPosition(this.code().trim(), lat, lon).subscribe({
        next: p => {
          this.accepter(p);
          this.message.set('Vous êtes visible par les clients. Pensez à vous retirer ce soir.');
        },
        error: e => this.echec(e)
      });
    };

    const connue = this.geo.position();
    if (connue) { envoyer(connue.lat, connue.lon); return; }

    // La position n'est demandée qu'ici, au moment où elle sert : c'est plus clair pour la
    // personne que de la réclamer à l'ouverture de la page.
    this.geo.demander();
    const attendre = setInterval(() => {
      const p = this.geo.position();
      const etat = this.geo.etat();
      if (p) { clearInterval(attendre); envoyer(p.lat, p.lon); }
      else if (etat === 'refusee' || etat === 'indisponible') {
        clearInterval(attendre);
        this.erreur.set(
          "Sans votre position, les clients ne peuvent pas savoir qui est près d'eux. " +
          'Autorisez la localisation dans votre navigateur, puis réessayez.');
      }
    }, 300);
  }

  protected seRetirer(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.api.passerHorsLigne(this.code().trim()).subscribe({
      next: p => { this.accepter(p); this.message.set('Vous n\'apparaissez plus dans les recherches.'); },
      error: e => this.echec(e)
    });
  }

  protected repondre(travail: Travail, statut: string): void {
    this.travailEnCours.set(travail.id);
    this.erreur.set(null);
    this.api.changerStatut(this.code().trim(), travail.id, statut).subscribe({
      next: p => { this.profil.set(p); this.travailEnCours.set(null); },
      error: e => { this.echec(e); this.travailEnCours.set(null); }
    });
  }

  protected envoyerPhoto(evenement: Event): void {
    const entree = evenement.target as HTMLInputElement;
    const fichier = entree.files?.[0];
    const p = this.profil();
    if (!fichier || !p) return;

    this.chargement.set(true);
    this.erreur.set(null);
    this.api.envoyerPhoto(p.type, this.code().trim(), fichier).subscribe({
      next: r => {
        this.profil.set({ ...p, photo: r.photo });
        this.chargement.set(false);
        this.message.set('Photo enregistrée.');
        entree.value = '';          // sans cela, renvoyer le même fichier ne déclenche rien
      },
      error: e => { this.echec(e); entree.value = ''; }
    });
  }

  protected quitter(): void {
    this.profil.set(null);
    this.code.set('');
    this.message.set(null);
    this.effacerRangement();
  }

  /** Lien pour s'envoyer son propre code sur WhatsApp : c'est lui qui écrit, rien n'est facturé. */
  protected lienCode(): string {
    const texte = `Mon code Jaay sa xam-xam : ${this.code().trim()}`;
    return `https://wa.me/?text=${encodeURIComponent(texte)}`;
  }

  /** Lien vers la plateforme pour faire confirmer son numéro, quand un numéro est configuré. */
  protected lienVerification(): string | null {
    const numero = this.config()?.whatsappVerification;
    const p = this.profil();
    if (!numero || !p) return null;
    const texte = `Bonjour, je suis ${p.nom}. Je veux faire vérifier mon profil. Mon code : ${this.code().trim()}`;
    return `https://wa.me/${numero}?text=${encodeURIComponent(texte)}`;
  }

  protected heure(horodatage?: string): string {
    if (!horodatage) return '';
    return new Date(horodatage).toLocaleTimeString('fr', { hour: '2-digit', minute: '2-digit' });
  }

  // ---------- Petits utilitaires privés ----------

  private accepter(p: EspaceDto): void {
    this.profil.set(p);
    this.chargement.set(false);
  }

  private echec(e: unknown): void {
    this.chargement.set(false);
    this.erreur.set(messageHttp(e));
  }

  // Le rangement du navigateur peut lever en navigation privée : on n'en dépend jamais
  private lireRangement(): string | null {
    try { return localStorage.getItem(RANGEMENT); } catch { return null; }
  }
  private ecrireRangement(code: string): void {
    try { localStorage.setItem(RANGEMENT, code); } catch { /* sans importance */ }
  }
  private effacerRangement(): void {
    try { localStorage.removeItem(RANGEMENT); } catch { /* sans importance */ }
  }
}
