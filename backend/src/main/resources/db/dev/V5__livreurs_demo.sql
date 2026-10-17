-- Données de démonstration (profil dev uniquement) : positions et livreurs fictifs

-- Position et activité récente des artisans de démonstration
UPDATE artisan SET latitude = 14.6815, longitude = -17.4521, derniere_activite = NOW() - INTERVAL '2 minutes'
WHERE telephone = '770000001';
UPDATE artisan SET latitude = 14.7688, longitude = -17.4312, derniere_activite = NOW() - INTERVAL '6 minutes'
WHERE telephone = '770000002';
UPDATE artisan SET latitude = 14.7812, longitude = -17.3921, derniere_activite = NOW() - INTERVAL '3 hours'
WHERE telephone = '770000003';

INSERT INTO livreur (nom, telephone, vehicule, charge_max_kg, prix_par_km, prix_minimum, verifie, latitude, longitude, derniere_activite) VALUES
    ('Démo Moto',        '771000001', 'moto',        30,   300, 1000, TRUE,  14.6794, -17.4489, NOW() - INTERVAL '1 minute'),
    ('Démo Tricycle',    '771000002', 'tricycle',    250,  400, 1500, FALSE, 14.6841, -17.4553, NOW() - INTERVAL '3 minutes'),
    ('Démo Voiture',     '771000003', 'voiture',     150,  500, 2000, TRUE,  14.7108, -17.4602, NOW() - INTERVAL '4 hours'),
    ('Démo Camionnette', '771000004', 'camionnette', 1200, 800, 5000, FALSE, 14.7405, -17.4698, NOW() - INTERVAL '7 minutes');
