-- Journal des actions d'administration.
--
-- Rien n'est jamais modifié ni supprimé ici : une trace qu'on peut réécrire ne prouve rien.
-- Le jour où un artisan conteste un refus, ou si quelqu'un essaie d'entrer la nuit, c'est la
-- seule chose qui réponde.

CREATE TABLE action_admin (
    id        BIGSERIAL PRIMARY KEY,
    adresse   VARCHAR(64),
    action    VARCHAR(48)  NOT NULL,
    cible     VARCHAR(64),
    detail    VARCHAR(255),
    faite_le  TIMESTAMP    NOT NULL
);

-- Le back-office affiche les plus récentes d'abord : l'index sert exactement à ça
CREATE INDEX idx_action_admin_date ON action_admin (faite_le DESC);

-- Et pour retrouver tout ce qui concerne une personne précise
CREATE INDEX idx_action_admin_cible ON action_admin (cible) WHERE cible IS NOT NULL;
