import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

const resources = {
  en: { translation: {
    app_title: 'Through Every Season',
    show: 'Show',
    target_season: 'Target season',
    immersion: 'Immersion',
    get_recommendations: 'Get recommendations',
    loading: 'Loading...',
    error: 'Something went wrong',
    language: 'Language',
  }},
  ru: { translation: {
    app_title: 'Through Every Season',
    show: 'Сериал',
    target_season: 'Целевой сезон',
    immersion: 'Погружение',
    get_recommendations: 'Получить рекомендации',
    loading: 'Загрузка...',
    error: 'Что-то пошло не так',
    language: 'Язык',
  }},
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: 'en',
    interpolation: { escapeValue: false },
    detection: { order: ['querystring', 'localStorage', 'navigator'], caches: ['localStorage'] },
  });

export default i18n;