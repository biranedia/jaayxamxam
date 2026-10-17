-- Géolocalisation, livreurs (Yonni la) et courses

-- 1. Chaque quartier a un point central : sert à estimer le prix d'une course
ALTER TABLE zone
    ADD COLUMN latitude  DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;

UPDATE zone SET latitude = 14.6690, longitude = -17.4370 WHERE ville = 'Dakar'      AND quartier = 'Plateau';
UPDATE zone SET latitude = 14.6800, longitude = -17.4500 WHERE ville = 'Dakar'      AND quartier = 'Médina';
UPDATE zone SET latitude = 14.7700, longitude = -17.4300 WHERE ville = 'Dakar'      AND quartier = 'Parcelles Assainies';
UPDATE zone SET latitude = 14.7400, longitude = -17.5100 WHERE ville = 'Dakar'      AND quartier = 'Almadies';
UPDATE zone SET latitude = 14.7800, longitude = -17.3900 WHERE ville = 'Guédiawaye' AND quartier = 'Golf Sud';
UPDATE zone SET latitude = 14.7600, longitude = -17.3700 WHERE ville = 'Pikine'     AND quartier = 'Thiaroye';

-- 2. Dernière position connue de l'artisan ; derniere_activite détermine « en ligne »
ALTER TABLE artisan
    ADD COLUMN latitude          DOUBLE PRECISION,
    ADD COLUMN longitude         DOUBLE PRECISION,
    ADD COLUMN derniere_activite TIMESTAMP;

-- 3. Position d'où le client a envoyé sa demande
ALTER TABLE demande
    ADD COLUMN latitude  DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;

-- Une demande peut maintenant passer par « en route »
ALTER TABLE demande DROP CONSTRAINT ck_demande_statut;
ALTER TABLE demande ADD CONSTRAINT ck_demande_statut
    CHECK (statut IN ('NOUVELLE', 'ACCEPTEE', 'EN_ROUTE', 'REFUSEE', 'TERMINEE'));

-- 4. Livreurs et transporteurs
CREATE TABLE livreur (
    id                BIGSERIAL PRIMARY KEY,
    nom               VARCHAR(120) NOT NULL,
    telephone         VARCHAR(20)  NOT NULL UNIQUE,
    vehicule          VARCHAR(20)  NOT NULL,
    charge_max_kg     INTEGER      NOT NULL DEFAULT 30,
    prix_par_km       INTEGER      NOT NULL,                 -- en FCFA
    prix_minimum      INTEGER      NOT NULL,                 -- en FCFA
    verifie           BOOLEAN      NOT NULL DEFAULT FALSE,
    actif             BOOLEAN      NOT NULL DEFAULT TRUE,
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    derniere_activite TIMESTAMP,
    cree_le           TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_livreur_vehicule CHECK (vehicule IN ('moto', 'tricycle', 'voiture', 'camionnette')),
    CONSTRAINT ck_livreur_prix     CHECK (prix_par_km >= 0 AND prix_minimum >= 0)
);

CREATE INDEX idx_livreur_vehicule ON livreur (vehicule) WHERE actif;

-- 5. Courses Yonni la
CREATE TABLE course (
    id                  BIGSERIAL PRIMARY KEY,
    client_nom          VARCHAR(120) NOT NULL,
    client_telephone    VARCHAR(20)  NOT NULL,
    vehicule            VARCHAR(20)  NOT NULL,
    depart_zone_id      BIGINT       NOT NULL REFERENCES zone(id),
    destination_zone_id BIGINT       NOT NULL REFERENCES zone(id),
    repere              VARCHAR(200),                          -- « en face de la pharmacie »
    description         TEXT,
    distance_m          INTEGER      NOT NULL,
    prix_estime         INTEGER      NOT NULL,                 -- en FCFA
    statut              VARCHAR(20)  NOT NULL DEFAULT 'NOUVELLE',
    code_suivi          VARCHAR(36)  NOT NULL UNIQUE,
    livreur_id          BIGINT       REFERENCES livreur(id),
    cree_le             TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_course_statut   CHECK (statut IN ('NOUVELLE', 'ACCEPTEE', 'EN_ROUTE', 'REFUSEE', 'TERMINEE')),
    CONSTRAINT ck_course_vehicule CHECK (vehicule IN ('moto', 'tricycle', 'voiture', 'camionnette'))
);

CREATE INDEX idx_course_livreur ON course (livreur_id);

-- 6. Un avis porte soit sur un artisan (via une demande), soit sur un livreur (via une course)
ALTER TABLE avis
    ALTER COLUMN artisan_id DROP NOT NULL,
    ALTER COLUMN demande_id DROP NOT NULL,
    ADD COLUMN livreur_id BIGINT REFERENCES livreur(id),
    ADD COLUMN course_id  BIGINT UNIQUE REFERENCES course(id);

ALTER TABLE avis
    ADD CONSTRAINT ck_avis_cible  CHECK ((artisan_id IS NOT NULL) <> (livreur_id IS NOT NULL)),
    ADD CONSTRAINT ck_avis_source CHECK ((demande_id IS NOT NULL) <> (course_id IS NOT NULL));

CREATE INDEX idx_avis_livreur ON avis (livreur_id);
