import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

// Namespaces VI
import commonVI from './locales/vi/common.json'
import apiVI from './locales/vi/api.json'
import authVI from './locales/vi/auth.json'
import navVI from './locales/vi/nav.json'
import adminVI from './locales/vi/admin.json'
import battleVI from './locales/vi/battle.json'
import communityVI from './locales/vi/community.json'
import gamificationVI from './locales/vi/gamification.json'
import homeVI from './locales/vi/home.json'
import learningVI from './locales/vi/learning.json'
import premiumVI from './locales/vi/premium.json'
import progressVI from './locales/vi/progress.json'
import reportVI from './locales/vi/report.json'
import speakingVI from './locales/vi/speaking.json'
import userVI from './locales/vi/user.json'

// Namespaces EN
import commonEN from './locales/en/common.json'
import apiEN from './locales/en/api.json'
import authEN from './locales/en/auth.json'
import navEN from './locales/en/nav.json'
import adminEN from './locales/en/admin.json'
import battleEN from './locales/en/battle.json'
import communityEN from './locales/en/community.json'
import gamificationEN from './locales/en/gamification.json'
import homeEN from './locales/en/home.json'
import learningEN from './locales/en/learning.json'
import premiumEN from './locales/en/premium.json'
import progressEN from './locales/en/progress.json'
import reportEN from './locales/en/report.json'
import speakingEN from './locales/en/speaking.json'
import userEN from './locales/en/user.json'

const resources = {
  vi: {
    common: commonVI,
    api: apiVI,
    auth: authVI,
    nav: navVI,
    admin: adminVI,
    battle: battleVI,
    community: communityVI,
    gamification: gamificationVI,
    home: homeVI,
    learning: learningVI,
    premium: premiumVI,
    progress: progressVI,
    report: reportVI,
    speaking: speakingVI,
    user: userVI,
  },
  en: {
    common: commonEN,
    api: apiEN,
    auth: authEN,
    nav: navEN,
    admin: adminEN,
    battle: battleEN,
    community: communityEN,
    gamification: gamificationEN,
    home: homeEN,
    learning: learningEN,
    premium: premiumEN,
    progress: progressEN,
    report: reportEN,
    speaking: speakingEN,
    user: userEN,
  },
}

// Đăng ký TRƯỚC init() để không bỏ lỡ sự kiện lần đầu
i18n.on('languageChanged', (lng) => {
  if (typeof document !== 'undefined') {
    document.documentElement.lang = lng
  }
})

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: 'vi',
    supportedLngs: ['vi', 'en'],
    nonExplicitSupportedLngs: true,
    defaultNS: 'common',
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'elingo_lng',
      caches: ['localStorage'],
    },
    interpolation: {
      escapeValue: false,
    },
  })

// Set thủ công sau init() để chắc chắn html[lang] đúng ngay từ lần load đầu
if (typeof document !== 'undefined') {
  document.documentElement.lang = i18n.resolvedLanguage || 'vi'
}

export default i18n
