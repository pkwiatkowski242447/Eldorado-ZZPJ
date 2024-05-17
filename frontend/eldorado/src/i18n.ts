import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import HttpApi from 'i18next-http-backend';

i18n
    .use(HttpApi) // load translation using http -> see /public/locales
    .use(LanguageDetector) // detect user language
    .use(initReactI18next) // pass the i18n instance to react-i18next.
    .init({
        fallbackLng: 'en-GB', // use en if detected lng is not available
        debug: true,

        interpolation: {
            escapeValue: false, // react already safes from xss
        },

    });

export default i18n;