import { FormControl } from '@angular/forms';
import { lienWhatsapp, messageErreur, telephoneValide, echapper, sansAccent } from './utils';

describe('lienWhatsapp', () => {
  it('ajoute l\'indicatif 221 et retire les espaces', () => {
    expect(lienWhatsapp('77 000 00 01')).toBe('https://wa.me/221770000001');
  });

  it('ne double pas l\'indicatif', () => {
    expect(lienWhatsapp('+221 77 000 00 01')).toBe('https://wa.me/221770000001');
  });
});

describe('telephoneValide', () => {
  const ok = (v: string) => telephoneValide(new FormControl(v)) === null;

  it('accepte 9 chiffres, avec ou sans +221', () => {
    expect(ok('770000001')).toBe(true);
    expect(ok('77 000 00 01')).toBe(true);
    expect(ok('+221770000001')).toBe(true);
  });

  it('refuse un numéro trop court, trop long ou vide', () => {
    expect(ok('7700')).toBe(false);
    expect(ok('77000000012345')).toBe(false);
    expect(ok('')).toBe(false);
  });
});

describe('messageErreur', () => {
  it('donne un message selon le code HTTP', () => {
    expect(messageErreur(0)).toContain('joindre le serveur');
    expect(messageErreur(409)).toContain('existe déjà');
    expect(messageErreur(500)).toContain('erreur');
  });
});

describe('Échappement du HTML', () => {
  it('neutralise une balise glissée dans un nom', () => {
    // Leaflet écrit notre HTML tel quel : sans échappement, ce nom exécuterait du code
    expect(echapper('<img src=x onerror="alert(1)">'))
      .toBe('&lt;img src=x onerror=&quot;alert(1)&quot;&gt;');
  });

  it('échappe l\'esperluette en premier, sinon le reste est cassé', () => {
    expect(echapper('Diop & Fils <plomberie>')).toBe('Diop &amp; Fils &lt;plomberie&gt;');
  });

  it('laisse un nom normal intact', () => {
    expect(echapper('Moussa Ndiaye')).toBe('Moussa Ndiaye');
  });

  it('échappe aussi les apostrophes et guillemets des attributs', () => {
    expect(echapper(`a'b"c`)).toBe('a&#39;b&quot;c');
  });
});

describe('Recherche sans accent', () => {
  it('trouve « Maçon » en tapant « macon »', () => {
    expect(sansAccent('Maçon')).toBe('macon');
    expect(sansAccent('Électricien')).toBe('electricien');
    expect(sansAccent('Ménage')).toBe('menage');
  });
});

describe('Lien WhatsApp sans numéro', () => {
  it('ne casse pas quand le numéro manque', () => {
    expect(lienWhatsapp(undefined)).toBe('');
    expect(lienWhatsapp(null)).toBe('');
    expect(lienWhatsapp('')).toBe('');
    expect(lienWhatsapp('77')).toBe('');
  });

  it('fonctionne toujours avec un vrai numéro', () => {
    expect(lienWhatsapp('77 000 00 01')).toBe('https://wa.me/221770000001');
  });
});
