import { Routes } from '@angular/router';
import { Accueil } from './pages/accueil/accueil';
import { Recherche } from './pages/recherche/recherche';
import { FicheArtisan } from './pages/fiche-artisan/fiche-artisan';
import { FormulaireDemande } from './pages/demande/demande';
import { Yonni } from './pages/yonni/yonni';
import { FormulaireCourse } from './pages/course/course';
import { Suivi } from './pages/suivi/suivi';
import { Inscription } from './pages/inscription/inscription';

export const routes: Routes = [
  { path: '', component: Accueil },
  // La recherche lit « famille » et « q » dans l'URL : l'accueil y envoie déjà filtré
  { path: 'artisans', component: Recherche },
  { path: 'artisan/:id', component: FicheArtisan },
  { path: 'demande', component: FormulaireDemande },
  { path: 'yonni', component: Yonni },
  { path: 'course', component: FormulaireCourse },
  // La carte est chargée seulement quand on l'ouvre : Leaflet pèse lourd en 3G
  { path: 'carte', loadComponent: () => import('./pages/carte/carte').then(m => m.Carte) },
  { path: 'suivi', component: Suivi },
  { path: 'inscription', component: Inscription },
  // Pas d'onglet vers l'administration : on y va par l'adresse, et le jeton fait le reste
  { path: 'admin', loadComponent: () => import('./pages/admin/admin').then(m => m.Admin) },
  { path: '**', redirectTo: '' }
];
