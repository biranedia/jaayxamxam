-- Photo de profil des travailleurs.
--
-- La base ne stocke que le nom du fichier ; l'image vit sur le disque du serveur, dans le dossier
-- indiqué par app.photos.dossier. En production, ce dossier doit être un volume persistant :
-- sinon les photos disparaissent au prochain déploiement.

ALTER TABLE artisan ADD COLUMN photo VARCHAR(64);
ALTER TABLE livreur ADD COLUMN photo VARCHAR(64);
