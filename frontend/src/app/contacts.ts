import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { lienWhatsapp, messageErreur } from './utils';

export type TypeProfil = 'artisan' | 'livreur';

/**
 * Appeler ou écrire à un travailleur.
 *
 * <p>Le numéro n'est plus dans la liste : il est demandé au moment du clic, puis on ouvre le
 * téléphone ou WhatsApp. On navigue dans le même onglet plutôt que d'en ouvrir un nouveau, parce
 * qu'un nouvel onglet ouvert après une attente est bloqué par les navigateurs. Sur téléphone, cela
 * ouvre l'application, ce qui est exactement ce qu'on veut.
 */
@Injectable({ providedIn: 'root' })
export class Contacts {
  private readonly api = inject(ApiService);

  readonly erreur = signal<string | null>(null);
  readonly enCours = signal<string | null>(null);   // « artisan:3 » pendant la demande

  appeler(type: TypeProfil, id: number): void {
    this.ouvrir(type, id, telephone => `tel:${telephone}`);
  }

  whatsapp(type: TypeProfil, id: number): void {
    this.ouvrir(type, id, telephone => lienWhatsapp(telephone));
  }

  private ouvrir(type: TypeProfil, id: number, adresse: (telephone: string) => string): void {
    this.erreur.set(null);
    this.enCours.set(`${type}:${id}`);
    this.api.contact(type, id).subscribe({
      next: r => {
        this.enCours.set(null);
        window.location.href = adresse(r.telephone);
      },
      error: e => {
        this.enCours.set(null);
        this.erreur.set(e.status === 429
          ? 'Trop de numéros demandés en une heure. Réessayez plus tard.'
          : messageErreur(e.status));
      }
    });
  }
}
