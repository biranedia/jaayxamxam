-- File d'attente de validation des inscriptions.
--
-- Un profil refusé n'est pas supprimé : on garde la trace, et la personne peut être validée plus
-- tard après vérification. La file d'attente, c'est « ni actif, ni refusé ».

ALTER TABLE artisan ADD COLUMN refuse BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE livreur ADD COLUMN refuse BOOLEAN NOT NULL DEFAULT FALSE;

DROP INDEX IF EXISTS idx_artisan_en_attente;
DROP INDEX IF EXISTS idx_livreur_en_attente;

CREATE INDEX idx_artisan_en_attente ON artisan (cree_le) WHERE NOT actif AND NOT refuse;
CREATE INDEX idx_livreur_en_attente ON livreur (cree_le) WHERE NOT actif AND NOT refuse;
