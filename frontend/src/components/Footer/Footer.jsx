import './Footer.css'

/**
 * Footer component — simple glassmorphism footer.
 * @param {Function} onNavigate
 */
function Footer({ onNavigate }) {
  const handleNav = (path) => {
    if (onNavigate) onNavigate(path)
  }

  return (
    <footer className="footer glass-capsule">
      <div className="footer__inner container">
        <div className="footer__brand">
          <span className="footer__brand-name">Elingo</span>
          <p className="footer__tagline text-body-sm">
            Học tiếng Anh hiệu quả — mọi lúc, mọi nơi.
          </p>
        </div>

        <nav className="footer__nav" aria-label="Footer navigation">
          <div className="footer__nav-group">
            <span className="footer__nav-heading text-label-md">Học tập</span>
            <a href="/learning" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/learning') }}>Bài học</a>
            <a href="/flashcard" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/flashcard') }}>Flashcard</a>
            <a href="/vocabulary" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/vocabulary') }}>Từ vựng</a>
          </div>

          <div className="footer__nav-group">
            <span className="footer__nav-heading text-label-md">Tính năng</span>
            <a href="/battle" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/battle') }}>Thi đấu</a>
            <a href="/speaking" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/speaking') }}>Luyện nói</a>
            <a href="/community" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/community') }}>Cộng đồng</a>
          </div>

          <div className="footer__nav-group">
            <span className="footer__nav-heading text-label-md">Tài khoản</span>
            <a href="/profile" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/profile') }}>Hồ sơ</a>
            <a href="/progress" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/progress') }}>Tiến độ</a>
            <a href="/premium" className="footer__nav-link" onClick={(e) => { e.preventDefault(); handleNav('/premium') }}>Premium</a>
          </div>
        </nav>
      </div>

      <div className="footer__bottom container">
        <p className="text-body-sm" style={{ color: 'var(--color-text-disabled)' }}>
          © {new Date().getFullYear()} Elingo. All rights reserved.
        </p>
      </div>
    </footer>
  )
}

export default Footer
