import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { messageErreur, urlPhoto } from '../../utils';

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

/**
 * Écran d'administration, volontairement austère.
 *
 * <p>Le jeton reste en mémoire : il n'est écrit ni dans le navigateur ni dans l'adresse. Fermer
 * l'onglet le perd, et c'est voulu — un mot de passe d'administration qui traîne dans le stockage
 * du navigateur se retrouve dans n'importe quelle faille d'affichage.
 */
@Component({
  selector: 'app-admin',
  imports: [DatePipe],
  templateUrl: './admin.html'
})
export class Admin {
  private readonly http = inject(HttpClient);

  protected readonly jeton = signal('');
  protected readonly resume = signal<Resume | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly enCours = signal(false);
  protected readonly urlPhoto = urlPhoto;

  protected readonly ouvert = computed(() => this.resume() !== null);

  protected entrer(valeur: string, evenement: Event): void {
    evenement.preventDefault();
    this.jeton.set(valeur.trim());
    this.charger();
  }

  protected charger(): void {
    if (!this.jeton()) return;
    this.enCours.set(true);
    this.erreur.set(null);
    this.http.get<Resume>('/api/admin/attente', { headers: this.entetes() }).subscribe({
      next: r => { this.resume.set(r); this.enCours.set(false); },
      error: e => {
        this.resume.set(null);
        this.enCours.set(false);
        this.erreur.set(e.status === 403
          ? "Jeton refusé, ou administration non configurée sur le serveur."
          : messageErreur(e.status));
      }
    });
  }

  protected agir(profil: EnAttente, action: 'valider' | 'refuser' | 'verifier'): void {
    this.enCours.set(true);
    this.message.set(null);
    this.http.post(`/api/admin/${profil.type}/${profil.id}/${action}`, null, { headers: this.entetes() })
      .subscribe({
        next: () => {
          this.message.set(`${profil.nom} : ${this.libelle(action)}.`);
          this.charger();   // la file se recalcule côté serveur
        },
        error: e => {
          this.enCours.set(false);
          this.erreur.set(messageErreur(e.status));
        }
      });
  }

  private libelle(action: string): string {
    if (action === 'valider') return 'profil rendu visible';
    if (action === 'refuser') return 'inscription écartée';
    return 'badge « Vérifié » accordé';
  }

  private entetes(): HttpHeaders {
    return new HttpHeaders({ 'X-Admin-Token': this.jeton() });
  }
}
