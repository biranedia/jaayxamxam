import { distanceMetres, estEnLigne, formatDistance, minutesTrajet } from './geo';

describe('Outils de géolocalisation', () => {
  const medina = { lat: 14.68, lon: -17.45 };
  const golfSud = { lat: 14.78, lon: -17.39 };

  it('mesure la distance entre deux quartiers de Dakar', () => {
    // Médina → Golf Sud : environ 12,8 km à vol d'oiseau
    const d = distanceMetres(medina, golfSud);
    expect(d).toBeGreaterThan(12_000);
    expect(d).toBeLessThan(13_500);
  });

  it('donne une distance nulle entre un point et lui-même', () => {
    expect(distanceMetres(medina, medina)).toBe(0);
  });

  it('écrit les mètres en dessous du kilomètre et les kilomètres au-dessus', () => {
    expect(formatDistance(280)).toBe('280 m');
    expect(formatDistance(1240)).toBe('1,2 km');
    expect(formatDistance(12_800)).toBe('12,8 km');
  });

  it('considère en ligne une position envoyée il y a moins de dix minutes', () => {
    const ilYa = (min: number) => new Date(Date.now() - min * 60_000).toISOString();
    expect(estEnLigne(ilYa(2))).toBe(true);
    expect(estEnLigne(ilYa(45))).toBe(false);
    expect(estEnLigne(undefined)).toBe(false);
  });

  it('estime une durée de trajet d\'au moins trois minutes', () => {
    expect(minutesTrajet(100)).toBe(3);
    expect(minutesTrajet(9000)).toBe(30);
  });
});
