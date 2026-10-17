-- Demandes de service et avis

CREATE TABLE demande (
    id                BIGSERIAL PRIMARY KEY,
    client_nom        VARCHAR(120) NOT NULL,
    client_telephone  VARCHAR(20)  NOT NULL,
    description       TEXT         NOT NULL,
    urgente           BOOLEAN      NOT NULL DEFAULT FALSE,
    statut            VARCHAR(20)  NOT NULL DEFAULT 'NOUVELLE',
    code_suivi        VARCHAR(36)  NOT NULL UNIQUE,   -- secret remis au client : suivre sa demande et noter
    metier_id         BIGINT       NOT NULL REFERENCES metier(id),
    zone_id           BIGINT       NOT NULL REFERENCES zone(id),
    artisan_id        BIGINT       REFERENCES artisan(id),   -- artisan contacté (facultatif)
    cree_le           TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_demande_statut CHECK (statut IN ('NOUVELLE', 'ACCEPTEE', 'REFUSEE', 'TERMINEE'))
);

CREATE INDEX idx_demande_metier_zone ON demande(metier_id, zone_id);
CREATE INDEX idx_demande_artisan ON demande(artisan_id);

CREATE TABLE avis (
    id           BIGSERIAL PRIMARY KEY,
    artisan_id   BIGINT    NOT NULL REFERENCES artisan(id),
    demande_id   BIGINT    NOT NULL UNIQUE REFERENCES demande(id),   -- un seul avis par demande
    note         INTEGER   NOT NULL CHECK (note BETWEEN 1 AND 5),
    commentaire  TEXT,
    masque       BOOLEAN   NOT NULL DEFAULT FALSE,                    -- avis masqué par la modération
    cree_le      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_avis_artisan ON avis(artisan_id);
