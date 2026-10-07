import { useTranslation } from 'react-i18next'
import './Footer.css'

/**
 * Footer component — simple glassmorphism footer.
 * @param {Function} onNavigate
 */
function Footer({ onNavigate }) {
  const { t } = useTranslation('nav')

  const handleNav = (path) => {
    if (onNavigate) onNavigate(path)
  }

  return (
    <footer className="footer glass-capsule">
      <div className="footer__inner container">
        <div className="footer__brand">
          <span className="footer__brand-name">Elingo</span>
          <p className="footer__tagline text-body-sm">
            {t('footer.tagline')}
          </p>
        </div>

        <nav className="footer__nav" aria-label="Footer navigation">
          <div className="footer__nav-group">
            <span className="footer__nav-heading text-label-md">{t('footer.learningGroup')}</span>
            <a href="/learning" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/learning') }}>{t('footer.lessons')}</a>
            <a href="/flashcard" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/flashcard') }}>Flashcard</a>
            <a href="/vocabulary" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/vocabulary') }}>{t('footer.vocabulary')}</a>
          </div>

          <div className="footer__nav-group">
            <span className="footer__nav-heading text-label-md">{t('footer.featuresGroup')}</span>
            <a href="/battle" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/battle') }}>{t('battle')}</a>
            <a href="/speaking" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/speaking') }}>{t('footer.speaking')}</a>
            <a href="/community" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/community') }}>{t('community')}</a>
          </div>

          <div className="footer__nav-group">
            <span className="footer__nav-heading text-label-md">{t('footer.accountGroup')}</span>
            <a href="/profile" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/profile') }}>{t('footer.profileLink')}</a>
            <a href="/progress" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/progress') }}>{t('footer.progressLink')}</a>
            <a href="/premium" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/premium') }}>{t('footer.premiumLink')}</a>
          </div>
        </nav>
      </div>

      <div className="footer__bottom container">
        <p className="text-body-sm" style={{ color: 'var(--color-text-disabled)' }}>
          {t('footer.copyright', { year: new Date().getFullYear() })}
        </p>
      </div>
    </footer>
  )
}

export default Footer
