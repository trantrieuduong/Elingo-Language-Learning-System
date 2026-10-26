import BackButton from '../../../../../components/BackButton/BackButton';
import './FlashcardSessionLayout.css';

function FlashcardSessionLayout({
  backLabel,
  onBack,
  progress,
  sidebar,
  sidebarClassName = 'flashcard-sidebar',
  layoutClassName = '',
  children,
}) {
  return (
    <div className="flashcard-page-container">
      <div className={`flashcard-layout ${layoutClassName}`.trim()}>
        <div className="flashcard-header-bar">
          <BackButton label={backLabel} onClick={onBack} />
        </div>

        {progress}

        <aside className={sidebarClassName}>{sidebar}</aside>

        <main className="flashcard-main-content glass-well">{children}</main>
      </div>
    </div>
  );
}

export default FlashcardSessionLayout;
