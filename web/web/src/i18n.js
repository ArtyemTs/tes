const translations = {
  en: {
    title: "Through Every Season (MVP)",
    showId: "Show ID",
    targetSeason: "Target Season",
    immersion: "Immersion (1..5)",
    getRecs: "Get recommendations",
    lang: "Language",
    seasonEp: (s,e) => `S${s}E${e}`,
  },
  ru: {
    title: "Through Every Season (MVP)",
    showId: "ID сериала",
    targetSeason: "Целевой сезон",
    immersion: "Погружение (1..5)",
    getRecs: "Получить рекомендации",
    lang: "Язык",
    seasonEp: (s,e) => `S${s}E${e}`,
  }
};

export function createI18n(defaultLang = 'en') {
  let current = defaultLang;
  const listeners = new Set();
  const t = (key, ...args) => {
    const dict = translations[current] || translations.en;
    const val = dict[key];
    return typeof val === 'function' ? val(...args) : (val ?? key);
  };
  return {
    t,
    get lang(){ return current; },
    setLang(l){ current = (translations[l] ? l : 'en'); listeners.forEach(cb=>cb(current)); },
    subscribe(cb){ listeners.add(cb); return ()=>listeners.delete(cb); }
  };
}