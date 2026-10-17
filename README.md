# Jaay sa xam-xam

Plateforme de mise en relation entre artisans (plombier, maçon, électricien, carreleur...) et clients
au Sénégal, avec un module de livraison et de transport appelé **Yonni la**.

- `backend/` : API Spring Boot 3.5 (Java 21), JPA, Flyway
- `frontend/` : Angular 21
- `docker-compose.yml` : base PostgreSQL 16 pour le développement

## Les cinq écrans

| Onglet | Ce qu'on y fait |
|---|---|
| Artisans | Chercher par métier et par quartier, voir qui est en ligne et à quelle distance |
| Yonni la | Trouver un livreur (moto, tricycle, voiture, camionnette) et demander une course |
| Carte | Voir artisans et livreurs autour de soi, avec leur distance |
| Suivi | Entrer son code de suivi pour voir où en est sa demande ou sa course |
| M'inscrire | Côté travailleur : proposer ses services, comme artisan ou comme livreur |

L'écran d'administration est à `/admin`. Il n'a volontairement aucun lien dans l'application :
on y accède par l'adresse, et le jeton fait le reste.

La position du visiteur n'est demandée qu'au clic sur la puce en haut de l'écran. Sans elle, tout
fonctionne : il n'y a simplement ni distance ni tri par proximité.

## Les métiers

25 métiers actifs, rangés en cinq familles. La **famille donne la couleur** du panneau, le **métier
donne le dessin de l'outil** : au-delà d'une dizaine de teintes, plus personne ne les distingue.

| Famille | Métiers |
|---|---|
| Bâtiment | Maçon, Plombier, Électricien, Peintre, Carreleur, Menuisier, Soudeur, Vitrier, Plaquiste |
| Maison | Ménage, Jardinier, Froid et climatisation, Antenne et parabole |
| Mécanique | Mécanicien auto, Mécanicien moto, Tôlier, Électricien auto, Lavage auto |
| Couture et beauté | Tailleur, Coiffure femme, Coiffure homme, Cordonnier |
| Services | Vigile, Cuisine à domicile, Répétiteur |

Ajouter un métier, c'est une ligne dans la table `metier` : un nom, une famille et une clé d'icône.
Sans dessin correspondant dans `glyphe.ts`, le métier s'affiche avec une boîte à outils générique.
L'ancien métier « Livreur » est passé à `actif = false` : il est repris par Yonni la.

Comme la liste dépasse la dizaine, l'écran ne montre pas tous les métiers d'un coup : on choisit
d'abord une famille, ou on tape le nom du métier (la recherche ignore les accents, « macon » trouve
« Maçon »).

## Démarrer en local

1. Base de données :
   ```bash
   docker compose up -d
   ```
   Elle écoute sur **le port 5433**, pas 5432. Beaucoup de postes ont déjà un
   PostgreSQL installé qui occupe 5432 ; on s'y connecterait alors sans le savoir,
   et l'erreur affichée serait un refus d'authentification difficile à interpréter.
2. Backend (http://localhost:8080) :
   ```bash
   cd backend
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   Sous Windows sans Maven installé, utiliser le lanceur fourni :
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
   ```
   Le profil `dev` ajoute des artisans et des livreurs fictifs, avec leur position.
   Sans lui, le schéma est créé mais la base reste vide.
   Test rapide : http://localhost:8080/api/artisans
3. Frontend (http://localhost:4200) :
   ```bash
   cd frontend
   npm install --legacy-peer-deps
   npm start
   ```

Le frontend appelle `/api/...`, redirigé vers le port 8080 par `frontend/proxy.conf.json`.

## API actuelle

| Méthode | URL | Rôle |
|---|---|---|
| GET | `/api/metiers` | Liste des métiers |
| GET | `/api/zones` | Liste des quartiers, avec leur point central |
| GET | `/api/artisans?metierId=&zoneId=&lat=&lon=` | Recherche ; avec `lat`/`lon`, distance et tri par proximité |
| GET | `/api/artisans/{id}?lat=&lon=` | Fiche détaillée : note moyenne et avis |
| GET | `/api/livreurs?vehicule=&lat=&lon=` | Livreurs Yonni la, mêmes règles de distance |
| POST | `/api/demandes` | Crée une demande ; renvoie un `codeSuivi` secret (8 caractères) |
| POST | `/api/courses` | Crée une course ; renvoie le `codeSuivi`, la distance et le prix annoncé |
| GET | `/api/suivi/{code}` | État d'une demande ou d'une course, et position de l'intervenant |
| POST | `/api/avis` | Dépose un avis (note 1-5) avec le `codeSuivi` ; un seul avis par demande ou par course |
| POST | `/api/inscriptions/artisan` | Inscription d'un artisan ; profil inactif, renvoie un `codeGestion` |
| POST | `/api/inscriptions/livreur` | Inscription d'un livreur ; prix facultatifs, tarif du véhicule par défaut |
| GET | `/api/contacts/{type}/{id}` | Le numéro de téléphone d'un profil, un à la fois |
| POST | `/api/photos/{type}` | Photo de profil (multipart : `code` + `fichier`) |
| GET | `/api/photos/{nom}` | Sert la photo ; nom tiré au hasard, cache d'un an |
| GET | `/api/admin/attente` | File des inscriptions à valider (jeton requis) |
| POST | `/api/admin/{type}/{id}/valider` | Rend le profil visible |
| POST | `/api/admin/{type}/{id}/verifier` | Donne le badge « Vérifié » |
| POST | `/api/admin/{type}/{id}/refuser` | Écarte l'inscription, sans la supprimer |
| POST | `/api/admin/avis/{id}/masquer` | Masque un avis, sans l'effacer |
| GET | `/actuator/health` | État de l'application |

## Inscription des travailleurs

Un travailleur s'inscrit depuis l'onglet **M'inscrire**, en choisissant d'abord s'il est artisan ou
livreur. Son profil est créé avec `actif = false` : **il n'apparaît nulle part tant que personne ne
l'a validé**. C'est volontaire — sans vérification du téléphone, n'importe qui pourrait inscrire le
numéro d'un concurrent.

Il reçoit un `codeGestion` à conserver : c'est ce qui lui permettra de reprendre la main sur son
profil quand l'authentification existera. Un numéro ne porte qu'un seul profil, artisan ou livreur.

Pour valider un profil, en attendant l'écran d'administration :

```sql
-- La file d'attente
SELECT id, nom, telephone, cree_le FROM artisan WHERE NOT actif ORDER BY cree_le;
-- Rendre visible, puis donner le badge après contrôle de la pièce d'identité
UPDATE artisan SET actif = TRUE WHERE id = 42;
UPDATE artisan SET verifie = TRUE WHERE id = 42;
```

## Les photos

Le travailleur ajoute sa photo juste après son inscription, avec son code de gestion. Le serveur ne
fait jamais confiance au fichier reçu :

1. **Les octets, pas le nom.** Le type vient des premiers octets. Un `.jpg` qui commence autrement
   est refusé.
2. **Les dimensions avant le décodage.** Une image annoncée en 25 000 × 25 000 pèse quelques
   kilo-octets sur le disque et plusieurs gigaoctets en mémoire : c'est la *bombe de décompression*.
   On lit l'en-tête, on refuse, on ne décode jamais.
3. **Ré-encodage systématique** en JPEG 640 px. Cela efface les métadonnées EXIF — **dont les
   coordonnées GPS de l'endroit où la photo a été prise**, souvent le domicile du travailleur — et
   neutralise les fichiers hybrides image/script. Au passage, 230 ko deviennent 8 ko, ce qui compte
   en 3G.
4. **Nom tiré au hasard** (32 caractères hexadécimaux). Le nom d'origine n'est jamais réutilisé :
   ni chemin glissé dedans, ni extension choisie par l'envoyeur.

Les fichiers vivent dans `app.photos.dossier` (`PHOTOS_DIR`), **hors de toute racine web**. En
production, ce dossier doit être un volume persistant, sinon les photos disparaissent au prochain
déploiement.

## Administration

```bash
export ADMIN_TOKEN=$(openssl rand -hex 24)
```

Sans cette variable, `/api/admin` répond 403 à tout le monde : **pas d'« admin/admin »**, un serveur
mal configuré n'expose rien. Le jeton voyage dans l'en-tête `X-Admin-Token`, jamais dans l'adresse,
et n'est pas conservé par le navigateur : il faut le ressaisir à chaque ouverture de `/admin`.

Rien n'y est destructeur. Écarter une inscription la garde en base (`refuse = true`), masquer un
avis le garde aussi. Une erreur à 23 h ne doit pas effacer le gagne-pain de quelqu'un.

## Les codes remis aux utilisateurs

Neuf caractères : une lettre de type puis huit tirés au hasard, sans I, L, O, 0 ni 1 — les
caractères qu'on confond en recopiant.

| Lettre | Code de quoi | Qui le reçoit |
|---|---|---|
| `D` | Demande d'artisan | le client, à l'envoi |
| `C` | Course Yonni la | le client, à l'envoi |
| `A` | Profil artisan | l'artisan, à l'inscription |
| `L` | Profil livreur | le livreur, à l'inscription |

La lettre de type n'est pas décorative. Chaque table a sa propre contrainte d'unicité : sans elle,
une demande et une course pourraient porter le même code, et ce code ne désignerait plus une seule
chose. La lettre rend la collision impossible entre types.

## Comment marchent la position et le prix

- **En ligne** : un artisan ou un livreur est « en ligne » si son téléphone a envoyé sa position il y a
  moins de dix minutes (`derniere_activite`).
- **Distance** : calculée par la formule de haversine, côté serveur quand `lat`/`lon` sont fournis,
  côté navigateur pour la carte et la fiche. Les deux calculs donnent le même résultat.
- **Prix d'une course** : distance entre les deux quartiers, majorée de 30 % pour les rues, multipliée
  par le tarif au kilomètre, arrondie à la centaine de francs, jamais en dessous du minimum du livreur.
  Les prix du livreur choisi l'emportent sur le tarif indicatif du véhicule (`Tarifs.java`).

## Sécurité

### Traité

- **Les numéros de téléphone ont quitté les listes.** `GET /api/artisans` ne les renvoie plus : ils
  se demandent un par un via `/api/contacts/{type}/{id}`, compté et journalisé. Aspirer l'annuaire
  demande désormais une requête par profil, au lieu d'une seule pour tout Dakar. Les boutons
  « Appeler » et « WhatsApp » gardent le même geste : le numéro est récupéré au moment du clic.
- **Envoi de photos verrouillé** : octets magiques, dimensions avant décodage, ré-encodage qui
  efface l'EXIF, nom tiré au hasard (voir la section « Les photos »).
- **Administration fermée par défaut**, jeton en variable d'environnement, comparé à temps constant
  (voir la section « Administration »).
- **En-têtes de sécurité** sur toutes les réponses (`EntetesSecurite.java`) : `nosniff`,
  anti-cadrage, politique de contenu fermée sur `/api`, pas de référent transmis.
- **Erreurs uniformes** (`GestionnaireErreurs.java`) : jamais de pile d'appels renvoyée au
  navigateur, qui dirait à un attaquant quelles bibliothèques tournent et dans quelles versions.
- **Positions publiques arrondies à ~110 m** (`Positions.java`). Un artisan partage sa position pour
  être trouvé, pas pour qu'on sache où il dort. La position exacte reste en base, sert au calcul des
  distances côté serveur, et n'est donnée qu'au client qui suit un travail déjà accepté.
- **Échappement du HTML des marqueurs de carte** (`utils.ts`, `carte.ts`). Leaflet écrit notre HTML
  tel quel : un artisan nommé `<img src=x onerror=…>` aurait fait exécuter son code chez chaque
  visiteur. Angular échappe tout seul dans ses gabarits, pas les bibliothèques externes.
- **Limitation de débit par adresse IP** (`LimiteurDebit.java`) : 10 créations et 30 essais de code
  de suivi par heure. Freine le spam de fausses demandes et l'essai de codes au hasard.
- **Validation des entrées** côté serveur (jakarta validation + `Telephone`, `Vehicule`), jamais
  seulement côté navigateur.
- **Requêtes paramétrées** partout (JPA) : pas d'injection SQL.
- **CORS fermé** par défaut à `http://localhost:4200`, à changer par `CORS_ORIGINS` en production.
- **Actuator** n'expose que `health` et `info`.

### Non traité, à décider

- **Aucune authentification des travailleurs.** C'est le verrou qui manque, et tout en découle :
  l'inscription doit être validée à la main, et le code de gestion remplace un vrai compte.
- **Le code de suivi est le seul secret du client.** Qui l'a voit le travail et peut déposer l'avis.
  Il passe dans l'adresse (`/api/suivi/{code}`), donc il se retrouve dans les journaux du serveur et
  l'historique du navigateur. À déplacer dans un en-tête le jour où ça compte — c'est déjà le cas
  pour le code de gestion, qui passe par le formulaire.
- **La collecte des numéros est ralentie, pas rendue impossible.** 60 numéros par heure et par
  adresse : un script patient finit par passer. Les journaux permettent de le voir.
- **La limitation de débit est en mémoire** : remise à zéro à chaque redémarrage, et propre à chaque
  instance. Il faudra Redis au-delà d'un serveur. `X-Forwarded-For` n'est fiable que derrière un
  proxy de confiance.
- **Pas de modération des avis** au-delà d'un avis par demande, ni de signalement d'abus.
- **Pas de journalisation** des accès.

### Obligatoire avant la mise en ligne

1. **HTTPS**, terminé par le proxy. Sans lui, numéros et codes de suivi circulent en clair.
2. **Mots de passe par variables d'environnement.** Ceux de `application.yml` et de
   `docker-compose.yml` sont des valeurs de développement : ils ne doivent jamais partir en
   production.
3. **`CORS_ORIGINS`** réglé sur le vrai domaine.
4. **Sauvegardes de la base**, testées en restauration.
5. **Décider de l'authentification** : sans elle, l'espace artisan ne peut pas exister.

## Tests

| Où | Quoi | Lancer |
|---|---|---|
| `frontend/src/**/*.spec.ts` | 74 tests : écrans, calculs de distance et de prix, échappement HTML | `npm test` |
| `backend/src/test/java/**` | Calculs, numéros, codes, positions, limitation, photos, administration, contrôleurs | `mvn test` |

Le traitement des photos, lui, **a été réellement exécuté** hors de Spring, sur de vrais fichiers
hostiles : fichier texte renommé, hybride image/script, bombe de décompression de 25 000 × 25 000
refusée en 5 ms, photo de téléphone avec coordonnées GPS dans l'EXIF — 230 ko en entrée, 8 ko en
sortie, plus aucune métadonnée. C'est pour cela que `Photos.java` ne porte aucune annotation
Spring : du code de sécurité doit pouvoir se tester seul.

Les autres tests du backend n'ont **jamais été exécutés** : Maven Central est bloqué dans l'environnement où
ce code a été écrit, donc rien n'a pu être compilé avec ses dépendances. Seules la syntaxe et les
classes sans dépendance (`Geo`, `Telephone`, `CodeSuivi`, `Positions`, `CompteurAppels`, `Vehicule`,
`Tarifs`) ont été vérifiées. Le premier `mvn test` chez toi est donc un vrai test de ces tests :
envoie-moi les erreurs.

## Configuration (variables d'environnement)

| Variable | Rôle |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Base PostgreSQL |
| `CORS_ORIGINS` | Origine du frontend, à régler sur le vrai domaine |
| `ADMIN_TOKEN` | Ouvre `/api/admin`. Sans lui, l'administration est injoignable |
| `PHOTOS_DIR` | Dossier des photos ; **volume persistant** en production |
| `LIMITE_CREATIONS`, `LIMITE_SUIVIS`, `LIMITE_CONTACTS`, `LIMITE_ADMIN` | Limites par adresse IP et par heure | Les valeurs par défaut de `application.yml` sont
réservées au développement.

## Ce qui reste à faire

- **Vérification du téléphone** (code par SMS ou par WhatsApp) : c'est le verrou qui manque. Tant
  qu'elle n'existe pas, les inscriptions sont validées à la main depuis `/admin`.
- **Espace du travailleur** : modifier son profil avec son code de gestion, et surtout le bouton
  « je suis disponible » qui envoie la position. Sans lui, personne n'est jamais « en ligne » et la
  carte reste vide.
- **Côté artisan : voir et accepter les demandes reçues.** Aujourd'hui elles arrivent en base, mais
  l'artisan n'a aucun écran pour y répondre — seul le téléphone fait le lien.
- **Notifications WhatsApp** quand une demande arrive.
- **Photos de chantier** en plus de la photo de profil.
- **Mise en cache hors ligne** (service worker) pour que l'application s'ouvre sans réseau.
