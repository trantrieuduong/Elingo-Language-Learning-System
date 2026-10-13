import i18n from '@/i18n'
export const LANGS = ['vi', 'en']
export const tr = (key, options) => i18n.t(key, options) // tr('auth:usernameLabel')
export const useLang = (lng) => i18n.changeLanguage(lng)

const escapeRegExp = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
export const trRx = (key, options) => new RegExp(escapeRegExp(tr(key, options)), 'i')
