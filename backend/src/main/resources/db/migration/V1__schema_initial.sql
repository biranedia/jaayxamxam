-- Schéma initial : métiers, zones, artisans

CREATE TABLE metier (
    id      BIGSERIAL PRIMARY KEY,
    nom     VARCHAR(80) NOT NULL UNIQUE,
    icone   VARCHAR(40)
);

CREATE TABLE zone (
    id      BIGSERIAL PRIMARY KEY,
    ville   VARCHAR(80) NOT NULL,
    quartier VARCHAR(80) NOT NULL,
    UNIQUE (ville, quartier)
);

CREATE TABLE artisan (
    id                  BIGSERIAL PRIMARY KEY,
    nom                 VARCHAR(120) NOT NULL,
    telephone           VARCHAR(20)  NOT NULL UNIQUE,
    description         TEXT,
    annees_experience   INTEGER      NOT NULL DEFAULT 0,
    tarif_indicatif     INTEGER,                      -- en FCFA
    verifie             BOOLEAN      NOT NULL DEFAULT FALSE,
    actif               BOOLEAN      NOT NULL DEFAULT TRUE,
    metier_id           BIGINT       NOT NULL REFERENCES metier(id),
    zone_id             BIGINT       NOT NULL REFERENCES zone(id),
    cree_le             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_artisan_metier_zone ON artisan(metier_id, zone_id);

-- Données de départ (Dakar, premiers métiers ciblés)
INSERT INTO metier (nom, icone) VALUES
    ('Plombier', 'plombier'),
    ('Électricien', 'electricien'),
    ('Maçon', 'macon'),
    ('Peintre', 'peintre'),
    ('Carreleur', 'carreleur'),
    ('Mécanicien auto', 'mecanicien'),
    ('Livreur', 'livreur');

INSERT INTO zone (ville, quartier) VALUES
    ('Dakar', 'Plateau'),
    ('Dakar', 'Médina'),
    ('Dakar', 'Parcelles Assainies'),
    ('Dakar', 'Almadies'),
    ('Guédiawaye', 'Golf Sud'),
    ('Pikine', 'Thiaroye');
