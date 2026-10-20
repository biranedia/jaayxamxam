import { AbstractControl, ValidationErrors } from '@angular/forms';

// Lien WhatsApp direct (indicatif Sénégal +221 ajouté si absent).
// Tolère un numéro absent : une donnée manquante ne doit pas casser l'affichage de la page.
export function lienWhatsapp(telephone?: string | null): string {
  const chiffres = String(telephone ?? '').replace(/\D/g, '');
  if (chiffres.length < 9) return '';
  const complet = chiffres.startsWith('221') ? chiffres : '221' + chiffres;
  return `https://wa.me/${complet}`;
}

// Même règle que le backend : 9 chiffres, avec ou sans +221
export function telephoneValide(controle: AbstractControl): ValidationErrors | null {
  const chiffres = String(controle.value ?? '').replace(/\D/g, '');
  const nombre = chiffres.length === 12 && chiffres.startsWith('221') ? 9 : chiffres.length;
  return nombre === 9 ? null : { telephone: true };
}

// Message lisible selon le code HTTP renvoyé par l'API
export function messageErreur(statut: number): string {
  switch (statut) {
    case 0:
      return 'Impossible de joindre le serveur. Vérifiez votre connexion.';
    case 400:
      return 'Les informations saisies sont invalides.';
    case 404:
      return 'Élément introuvable. Vérifiez le code saisi.';
    case 409:
      return 'Un avis existe déjà pour cette demande.';
    default:
      return 'Une erreur est survenue, merci de réessayer.';
  }
}

/**
 * Message à montrer pour une erreur HTTP, en préférant celui du serveur.
 *
 * <p>L'API répond {"message": "..."} avec des phrases précises — « Votre profil attend encore sa
 * validation », « Code inconnu » — bien plus utiles que les formules génériques par code. On ne
 * retombe sur messageErreur() que si le serveur n'a rien dit d'exploitable.
 *
 * <p>Le texte du serveur est inséré par Angular, qui échappe tout : aucun risque d'injection.
 */
export function messageHttp(erreur: unknown): string {
  const e = erreur as { status?: number; error?: { message?: string } } | null;
  const duServeur = e?.error?.message;
  if (typeof duServeur === 'string' && duServeur.trim()) return duServeur.trim();
  return messageErreur(typeof e?.status === 'number' ? e.status : -1);
}

// Initiales affichées dans le bloc de peinture quand il n'y a pas encore de photo
export function initiales(nom: string): string {
  return nom.trim().split(/\s+/).slice(0, 2).map(m => m[0] ?? '').join('').toUpperCase();
}

// Note en étoiles pleines et vides, arrondie au demi-point le plus proche
export function etoiles(note: number): string {
  const pleines = Math.round(note);
  return '★'.repeat(pleines) + '☆'.repeat(Math.max(0, 5 - pleines));
}

// Prix en francs CFA avec séparateur de milliers (3 500 FCFA)
export function fcfa(montant: number): string {
  return montant.toLocaleString('fr-FR').replace(/ | /g, ' ') + ' FCFA';
}

// Comparaison sans accent ni casse : « maçon » se trouve en tapant « macon »
export function sansAccent(texte: string): string {
  return texte.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

// Échappe le texte destiné à du HTML écrit à la main (Leaflet, notamment).
// Angular échappe tout seul dans ses gabarits ; ce n'est pas le cas des bibliothèques externes.
export function echapper(texte: string): string {
  return texte
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// Adresse d'une photo de profil servie par l'API
export function urlPhoto(nom?: string): string | null {
  return nom ? `/api/photos/${nom}` : null;
}
