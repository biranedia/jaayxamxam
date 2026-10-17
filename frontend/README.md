# Jaay sa xam-xam — application web

Interface de la plateforme de mise en relation entre artisans, livreurs et
clients au Sénégal. L'API se trouve dans `../backend`.

Angular 21, composants autonomes, signaux, sans zone.

## Démarrer

L'API doit tourner sur le port 8080 (voir le README à la racine du projet).

```bash
npm install --legacy-peer-deps
npm start
```

L'application répond sur http://localhost:4200. Les appels `/api/...` sont
redirigés vers le port 8080 par `proxy.conf.json`.

## Les écrans

| Route | Rôle |
|---|---|
| `/` | Accueil : chiffres en direct, familles de métiers, Yonni la |
| `/artisans` | Recherche, filtrable par `?famille=` et `?q=` |
| `/artisan/:id` | Fiche détaillée et avis |
| `/yonni` | Livreurs et transporteurs |
| `/course` | Demande de course, prix calculé avant validation |
| `/carte` | Carte Leaflet, chargée à la demande |
| `/suivi` | Retrouver une demande par son code |
| `/inscription` | Inscription des artisans et des livreurs |
| `/admin` | Modération, sans lien depuis la navigation |

## Identité visuelle

« Enseigne peinte » : les panneaux peints à la main des ateliers de Dakar et
les couleurs des cars rapides — cobalt, safran, brique. Une couleur par
famille de métier, un dessin au trait par métier. Les polices sont
auto-hébergées : aucun appel à un serveur extérieur.

La mise en page va d'une colonne sur téléphone à trois sur ordinateur, où la
barre d'onglets du bas cède la place à une navigation dans l'en-tête.

## Tests

```bash
npm test
```

83 tests. Ils couvrent les calculs de distance, l'échappement des textes
injectés dans Leaflet, les liens WhatsApp, les formulaires et la navigation.

## Points d'attention

- `npm install` doit être lancé sur le système où l'application tournera :
  esbuild installe un binaire propre à chaque système d'exploitation.
- Les numéros de téléphone ne sont jamais dans les listes : ils sont demandés
  un par un à `/api/contacts/{type}/{id}` au moment du clic.
