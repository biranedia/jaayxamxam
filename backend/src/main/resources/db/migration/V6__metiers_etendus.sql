-- Les métiers passent de 7 à 25 et sont regroupés par famille.
-- La famille donne la couleur du panneau, l'icône donne le dessin de l'outil.

ALTER TABLE metier
    ADD COLUMN famille VARCHAR(20),
    ADD COLUMN actif   BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE metier SET famille = 'batiment'  WHERE nom IN ('Plombier', 'Électricien', 'Maçon', 'Peintre', 'Carreleur');
UPDATE metier SET famille = 'mecanique' WHERE nom = 'Mécanicien auto';

-- « Livreur » est repris par le module Yonni la : on le retire de la recherche d'artisans
-- sans le supprimer, pour ne pas casser les profils déjà rattachés.
UPDATE metier SET famille = 'services', actif = FALSE WHERE nom = 'Livreur';

ALTER TABLE metier ALTER COLUMN famille SET NOT NULL;
ALTER TABLE metier ADD CONSTRAINT ck_metier_famille
    CHECK (famille IN ('batiment', 'maison', 'mecanique', 'beaute', 'services'));

INSERT INTO metier (nom, icone, famille) VALUES
    -- Bâtiment
    ('Menuisier',               'menuisier',  'batiment'),
    ('Soudeur',                 'soudeur',    'batiment'),
    ('Vitrier',                 'vitrier',    'batiment'),
    ('Plaquiste',               'plaquiste',  'batiment'),
    -- Maison
    ('Ménage',                  'menage',     'maison'),
    ('Jardinier',               'jardinier',  'maison'),
    ('Froid et climatisation',  'froid',      'maison'),
    ('Antenne et parabole',     'antenne',    'maison'),
    -- Mécanique
    ('Mécanicien moto',         'moto',       'mecanique'),
    ('Tôlier',                  'tolier',     'mecanique'),
    ('Électricien auto',        'batterie',   'mecanique'),
    ('Lavage auto',             'laveur',     'mecanique'),
    -- Couture et beauté
    ('Tailleur',                'tailleur',   'beaute'),
    ('Coiffure femme',          'peigne',     'beaute'),
    ('Coiffure homme',          'tondeuse',   'beaute'),
    ('Cordonnier',              'chaussure',  'beaute'),
    -- Services
    ('Vigile',                  'bouclier',   'services'),
    ('Cuisine à domicile',      'marmite',    'services'),
    ('Répétiteur',              'livre',      'services');

CREATE INDEX idx_metier_famille ON metier (famille) WHERE actif;
