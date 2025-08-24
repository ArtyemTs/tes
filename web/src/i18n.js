// Minimal i18n with pub/sub and persistence
const STORAGE_KEY = 'tes_lang';

const dict = {
  en: {
    title: 'Through Every Season — MVP',
    lang: 'Language',
    showId: 'Show ID',
    targetSeason: 'Target season (start from)',
    immersion: 'Immersion (1–5)',
    getRecs: 'Get recommendations',
    seasonEp: (s, e) => `S${s}E${e}`,
    error: 'Error',
  },
  ru: {
    title: 'Through Every Season — MVP',
    lang: 'Язык',
    showId: 'Сериал (ID)',
    targetSeason: 'Целевой сезон (старт)',
    immersion: 'Погружение (1–5)',
    getRecs: 'Получить рекомендации',
    seasonEp: (s, e) => `С${s}С${e}`,
    error: 'Ошибка',
  },
};

function createStore(initial) {
  let lang = initial || localStorage.getItem(STORAGE_KEY) || 'en';
  const subs = new Set();
  const t = (key, ...args) => {
    const v = dict[lang]?.[key] ?? key;
    return typeof v === 'function' ? v(...args) : v;
  };
  function setLang(next) {
    lang = next;
    localStorage.setItem(STORAGE_KEY, next);
    subs.forEach(fn => fn(next));
  }
  function subscribe(fn) {
    subs.add(fn);
    fn(lang);
    return () => subs.delete(fn);
  }
  return { get lang(){ return lang; }, setLang, subscribe, t };
}

export const i18n = createStore('en');
export const createI18n = (l) => createStore(l);