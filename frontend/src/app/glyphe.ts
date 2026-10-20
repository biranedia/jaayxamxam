import { ChangeDetectionStrategy, Component, input } from '@angular/core';

// La couleur dit la famille de métier (ou le véhicule), le dessin dit le métier.
// Avec 25 métiers, une couleur par métier ne serait plus lisible.
export const TEINTES: Record<string, string> = {
  batiment: 'var(--f-batiment)',
  maison: 'var(--f-maison)',
  mecanique: 'var(--f-mecanique)',
  beaute: 'var(--f-beaute)',
  services: 'var(--f-services)',
  moto: 'var(--f-maison)',
  tricycle: 'var(--f-batiment)',
  voiture: 'var(--f-services)',
  camionnette: 'var(--f-mecanique)'
};

export const FAMILLES = [
  { cle: 'batiment', nom: 'Bâtiment' },
  { cle: 'maison', nom: 'Maison' },
  { cle: 'mecanique', nom: 'Mécanique' },
  { cle: 'beaute', nom: 'Couture et beauté' },
  { cle: 'services', nom: 'Services' }
];

export function teinte(cle?: string): string {
  return TEINTES[cle ?? ''] ?? 'var(--f-services)';
}

// Outils dessinés au trait : un dessin par métier, comme sur un panneau d'atelier
@Component({
  selector: 'app-glyphe',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg [attr.width]="taille()" [attr.height]="taille()" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
         aria-hidden="true" focusable="false">
      @switch (nom()) {
        <!-- Bâtiment -->
        @case ('plombier') {
          <rect x="2" y="3" width="4" height="4" rx="1" />
          <path d="M6 5h4a6 6 0 0 1 6 6v6" />
          <rect x="13" y="17" width="6" height="4" rx="1" />
        }
        @case ('electricien') {
          <path d="M13 2 5 13h6l-1 9 9-12h-6z" fill="currentColor" stroke-width="1.5" />
        }
        @case ('macon') {
          <rect x="2.5" y="5" width="19" height="14" rx="1" />
          <path d="M2.5 9.7h19M2.5 14.3h19M9 5v4.7M16 9.7v4.6M9 14.3v4.7" />
        }
        @case ('peintre') {
          <rect x="3" y="4" width="11" height="5" rx="1" />
          <path d="M14 6.5h4a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2h-6v2" />
          <rect x="10" y="14.5" width="4" height="6.5" rx="1" />
        }
        @case ('carreleur') {
          <rect x="3" y="3" width="18" height="18" rx="1" />
          <path d="M3 12h18M12 3v18" />
        }
        @case ('menuisier') {
          <path d="M4 7h11l5 5H4z" />
          <path d="M4 12l2 2 2-2 2 2 2-2 2 2 2-2" />
        }
        @case ('soudeur') {
          <path d="M5 4h14v8a7 7 0 0 1-14 0z" />
          <path d="M9 10h6M3 3l1.5 1.5M21 3l-1.5 1.5" />
        }
        @case ('vitrier') {
          <rect x="3" y="3" width="18" height="18" rx="1" />
          <path d="M7 14l7-7M7 9l3-3" />
        }
        @case ('plaquiste') {
          <path d="M4 4h12l4 4v12H4z" />
          <path d="M16 4v4h4" />
        }

        <!-- Maison -->
        @case ('menage') {
          <path d="M12 2v8" />
          <path d="M7 10h10l1.5 11h-13z" />
          <path d="M10 10v11M12 10v11M14 10v11" />
        }
        @case ('jardinier') {
          <path d="M12 21v-7" />
          <path d="M12 14c0-3-2.3-5-5.5-5 0 3 2.3 5 5.5 5z" />
          <path d="M12 14c0-3.5 2.3-6 5.5-6 0 3.5-2.3 6-5.5 6z" />
        }
        @case ('froid') {
          <path d="M12 2v20M3.5 7l17 10M20.5 7l-17 10" />
          <path d="M12 6l2.5-2M12 6L9.5 4M12 18l2.5 2M12 18l-2.5 2" />
        }
        @case ('antenne') {
          <path d="M12 21v-8" />
          <path d="M5 3l7 10 7-10" />
          <path d="M8.5 8h7" />
        }

        <!-- Mécanique -->
        @case ('mecanicien') {
          <circle cx="12" cy="12" r="7" />
          <circle cx="12" cy="12" r="3" />
          <path d="M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2" />
        }
        @case ('tolier') {
          <path d="M13 4h5l3 4-3 4h-5z" />
          <path d="M13 8H9l-6 9 2 2 8-6z" />
        }
        @case ('batterie') {
          <rect x="2" y="7" width="20" height="12" rx="2" />
          <path d="M6 7V4h4v3M14 7V4h4v3M6 13h4M16 13h4M18 11v4" />
        }
        @case ('laveur') {
          <path d="M12 3s5.5 6.5 5.5 9.5a5.5 5.5 0 0 1-11 0C6.5 9.5 12 3 12 3z" />
        }
        @case ('voiture') {
          <path d="M3 16v-3l2.2-4.5A2 2 0 0 1 7 7h10a2 2 0 0 1 1.8 1.5L21 13v3M3 16h18" />
          <circle cx="7.5" cy="17.5" r="1.8" />
          <circle cx="16.5" cy="17.5" r="1.8" />
        }
        @case ('camionnette') {
          <path d="M2 6h11v11H2zM13 10h4l4 4v3h-8" />
          <circle cx="7" cy="18" r="1.9" />
          <circle cx="17.5" cy="18" r="1.9" />
        }
        @case ('tricycle') {
          <circle cx="5" cy="17" r="3" />
          <circle cx="19" cy="17" r="3" />
          <rect x="9" y="7" width="9" height="7" rx="1" />
          <path d="M5 17l3-7h2" />
        }

        <!-- Couture et beauté -->
        @case ('tailleur') {
          <circle cx="6" cy="18" r="2.6" />
          <circle cx="18" cy="18" r="2.6" />
          <path d="M7.8 16.2 20 3M16.2 16.2 4 3" />
        }
        @case ('peigne') {
          <rect x="3" y="4" width="18" height="5" rx="1" />
          <path d="M6 9v9M9.5 9v9M13 9v9M16.5 9v9M20 9v9" />
        }
        @case ('tondeuse') {
          <rect x="4" y="3" width="16" height="7" rx="1" />
          <path d="M6 10v3h12v-3" />
          <rect x="9" y="13" width="6" height="8" rx="1" />
        }
        @case ('chaussure') {
          <path d="M3 17v-7h4l3 3h5a6 6 0 0 1 6 4v3H3z" />
          <path d="M3 17h18" />
        }

        <!-- Services -->
        @case ('bouclier') {
          <path d="M12 2l8 3v6c0 5-3.4 9-8 11-4.6-2-8-6-8-11V5z" />
        }
        @case ('marmite') {
          <path d="M4 9h16v6a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4z" />
          <path d="M2 9h20M8 6V3.5M12 6V3.5M16 6V3.5" />
        }
        @case ('livre') {
          <path d="M4 4h6a2 2 0 0 1 2 2 2 2 0 0 1 2-2h6v14h-6a2 2 0 0 0-2 2 2 2 0 0 0-2-2H4z" />
          <path d="M12 6v14" />
        }

        <!-- Repères et navigation -->
        @case ('position') {
          <path d="M12 21s7-6.4 7-11a7 7 0 1 0-14 0c0 4.6 7 11 7 11z" />
          <circle cx="12" cy="10" r="2.6" />
        }
        @case ('cible') {
          <circle cx="12" cy="12" r="7" />
          <circle cx="12" cy="12" r="2.4" />
          <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
        }
        @case ('loupe') {
          <circle cx="11" cy="11" r="7" />
          <path d="M16 16l5 5" />
        }
        @case ('carte') {
          <path d="M9 4 3 6.5v13L9 17l6 2.5 6-2.5v-13L15 6.5z" />
          <path d="M9 4v13M15 6.5v13" />
        }
        @case ('liste') {
          <rect x="4" y="3" width="16" height="18" rx="2" />
          <path d="M8 8h8M8 12h8M8 16h5" />
        }
        @case ('sceau') {
          <path d="M20 6 9 17l-5-5" />
        }
        @case ('photo') {
          <path d="M3 8h3.2l1.4-2.2h7.8L16.8 8H21v12H3z" />
          <circle cx="12" cy="13.6" r="3.6" />
        }
        @case ('maison') {
          <path d="M3 10.5 12 3l9 7.5" />
          <path d="M5.5 9.5V20h13V9.5" />
          <path d="M10 20v-5.5h4V20" />
        }

        <!-- moto et livreur sont dessinés plus bas, par le bloc @if -->
        @case ('moto') {}
        @case ('livreur') {}

        @default {
          <rect x="2" y="8" width="20" height="12" rx="2" />
          <path d="M9 8V6a3 3 0 0 1 6 0v2M2 13h20" />
        }
      }

      @if (nom() === 'moto' || nom() === 'livreur') {
        <circle cx="5" cy="17" r="3.2" />
        <circle cx="19" cy="17" r="3.2" />
        <path d="M5 17l4-6h5l2 6M9 11H6M14 11l2-3h3" />
      }
    </svg>
  `,
  styles: ':host { display: inline-flex; }'
})
export class Glyphe {
  readonly nom = input.required<string>();
  readonly taille = input(20);
}
