-- Message vocal joint à une demande.
--
-- Beaucoup de clients parlent mieux qu'ils n'écrivent. Un champ texte obligatoire écartait
-- exactement ceux que la plateforme doit servir.
--
-- On ne garde que le nom du fichier : le contenu vit sur le disque, comme les photos. Le nom
-- fait 32 caractères hexadécimaux tirés au hasard, plus l'extension du contenant.

ALTER TABLE demande ADD COLUMN vocal VARCHAR(64);

COMMENT ON COLUMN demande.vocal IS 'Nom du fichier audio, ou NULL si le client a seulement écrit';

-- La description cesse d'être obligatoire : un vocal seul suffit désormais à décrire le travail.
-- C'est le serveur qui vérifie qu'au moins l'un des deux est présent ; la base, elle, ne peut pas
-- exprimer « l'un ou l'autre » sans une contrainte qu'il faudrait maintenir à la main.
ALTER TABLE demande ALTER COLUMN description DROP NOT NULL;

-- Garde-fou minimal : on refuse une demande sans aucune description, ni écrite ni parlée.
ALTER TABLE demande ADD CONSTRAINT demande_a_une_description
    CHECK (description IS NOT NULL OR vocal IS NOT NULL);
