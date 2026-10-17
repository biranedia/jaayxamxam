-- Inscription des artisans et des livreurs.
--
-- Tant qu'il n'y a pas d'authentification par téléphone, le travailleur reçoit un « code de gestion »
-- à conserver : c'est ce qui lui permettra plus tard de reprendre la main sur son profil.
-- Un profil inscrit arrive avec actif = FALSE : il n'apparaît dans la recherche qu'une fois validé.

ALTER TABLE artisan ADD COLUMN code_gestion VARCHAR(36) UNIQUE;
ALTER TABLE livreur ADD COLUMN code_gestion VARCHAR(36) UNIQUE;

-- Les profils en attente de validation, les plus anciens d'abord : c'est la file de l'administrateur
CREATE INDEX idx_artisan_en_attente ON artisan (cree_le) WHERE NOT actif;
CREATE INDEX idx_livreur_en_attente ON livreur (cree_le) WHERE NOT actif;
