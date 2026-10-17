-- Données FICTIVES pour le développement (profil "dev" uniquement, jamais en production)
INSERT INTO artisan (nom, telephone, description, annees_experience, tarif_indicatif, verifie, metier_id, zone_id) VALUES
    ('Démo Plombier', '770000001', 'Fuites, WC, chauffe-eau. Intervention rapide.', 8, 5000, TRUE,
        (SELECT id FROM metier WHERE nom = 'Plombier'), (SELECT id FROM zone WHERE quartier = 'Médina')),
    ('Démo Électricien', '770000002', 'Installation et dépannage électrique.', 5, 7000, FALSE,
        (SELECT id FROM metier WHERE nom = 'Électricien'), (SELECT id FROM zone WHERE quartier = 'Parcelles Assainies')),
    ('Démo Maçon', '770000003', 'Construction, dalles, murs de clôture.', 12, NULL, TRUE,
        (SELECT id FROM metier WHERE nom = 'Maçon'), (SELECT id FROM zone WHERE quartier = 'Golf Sud'));
