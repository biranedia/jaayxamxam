import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';

import { routes } from './app.routes';

// Dates et nombres en français : sans cet enregistrement, le pipe « date » échoue
registerLocaleData(localeFr, 'fr');

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(), // appels HTTP vers l'API Spring Boot
    provideRouter(routes, withComponentInputBinding()), // paramètres d'URL liés aux inputs des pages
    { provide: LOCALE_ID, useValue: 'fr' }
  ]
};
