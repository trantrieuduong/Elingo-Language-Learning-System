import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import FlipCard from '../FlipCard/FlipCard';
import './VocabularyFlashCard.css';

const formatLocale = (locale) => {
  if (!locale || typeof locale !== 'string') return '';
  const upper = locale.toUpperCase().trim();
  if (upper.includes('UK')) return 'UK';
  if (upper.includes('US')) return 'US';
  return locale;
};

const formatPhonetic = (text) => {
  if (!text || typeof text !== 'string') return '';
  const trimmed = text.trim();
  if (
    (trimmed.startsWith('/') && trimmed.endsWith('/')) ||
    (trimmed.startsWith('[') && trimmed.endsWith(']'))
  )
    return trimmed;
  return `/${trimmed}/`;
};

function VocabularyFlashCard({
  card,
  onPlayAudio,
  playingAudioKey,
  frontMeta,
  compact = false,
  className: additionalClassName = '',
}) {
  const { t } = useTranslation('learning');
  const [imgError, setImgError] = useState(false);
  const phonetics = Array.isArray(card.phonetics)
    ? card.phonetics.filter((phonetic) => phonetic && (phonetic.text || phonetic.audioUrl))
    : [];
  const className = [
    'vocabulary-flashcard',
    compact ? 'vocabulary-flashcard--compact' : '',
    additionalClassName,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <FlipCard
      className={className}
      frontClassName="flashcard-front glass-card"
      backClassName="flashcard-back glass-card"
      front={
        <div className="flashcard-face-content flashcard-front-content">
          {frontMeta && <div className="vocabulary-flashcard__meta">{frontMeta}</div>}
          {card.imageUrl && !imgError && (
            <div className="flashcard-image-wrapper">
              <img
                src={card.imageUrl}
                alt={card.term || t('flashcard.card.imageAlt')}
                className="flashcard-image"
                loading="lazy"
                onError={() => setImgError(true)}
              />
            </div>
          )}
          <h2 className="flashcard-term">{card.term || ''}</h2>
          {card.pos && <span className="badge badge--primary flashcard-pos">{card.pos}</span>}

          {phonetics.length > 0 && (
            <div className="flashcard-phonetics-group">
              {phonetics.map((phonetic, index) => {
                const key = `phonetic-${index}`;
                const localeLabel = formatLocale(phonetic.locale);
                const formattedPhonetic = formatPhonetic(phonetic.text);
                const isPlaying = playingAudioKey === key;

                return (
                  <div key={key} className="flashcard-phonetic-item">
                    {localeLabel && (
                      <span className="flashcard-phonetic-locale">{localeLabel}</span>
                    )}
                    {formattedPhonetic && (
                      <span className="flashcard-phonetic-text">{formattedPhonetic}</span>
                    )}
                    {phonetic.audioUrl && onPlayAudio && (
                      <button
                        type="button"
                        className={`btn-icon flashcard-audio-btn ${isPlaying ? 'playing' : ''}`}
                        onClick={(event) => onPlayAudio(event, phonetic.audioUrl, key)}
                        onPointerDown={(event) => event.stopPropagation()}
                        title={t('flashcard.card.playAudioTitle', {
                          locale: localeLabel ? `(${localeLabel})` : '',
                          term: card.term,
                        })}
                      >
                        <span
                          className={`material-symbols-outlined${isPlaying ? ' pulse-icon' : ''}`}
                        >
                          volume_up
                        </span>
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          )}

          <p className="flashcard-hint">
            <span className="material-symbols-outlined flashcard-hint-icon">touch_app</span>
            {t('flashcard.card.hintFront')}
          </p>
        </div>
      }
      back={
        <div className="flashcard-face-content flashcard-back-content">
          <h2 className="flashcard-term">{card.translation || ''}</h2>

          <div className="flashcard-details">
            {card.explanationVi && (
              <div className="flashcard-section">
                <span className="material-symbols-outlined">
                  menu_book
                </span>
                <div>
                  <p className="flashcard-section-label">{t('flashcard.card.definitionVi')}</p>
                  <p className="flashcard-explanation-text">{card.explanationVi}</p>
                </div>
              </div>
            )}

            {card.explanationEn && (
              <div className="flashcard-section">
                <span className="material-symbols-outlined">
                  menu_book
                </span>
                <div>
                  <p className="flashcard-section-label">{t('flashcard.card.definitionEn')}</p>
                  <p className="flashcard-explanation-text">{card.explanationEn}</p>
                </div>
              </div>
            )}

            {(card.examplesEn || card.examplesVi) && (
              <div className="flashcard-section">
                <span className="material-symbols-outlined">
                  format_quote
                </span>
                <div>
                  <p className="flashcard-section-label">{t('flashcard.card.examples')}</p>
                  {card.examplesEn && <p className="flashcard-example-en">"{card.examplesEn}"</p>}
                  {card.examplesVi && <p className="flashcard-example-vi">{card.examplesVi}</p>}
                </div>
              </div>
            )}
          </div>

          <p className="flashcard-hint">
            <span className="material-symbols-outlined flashcard-hint-icon">touch_app</span>
            {t('flashcard.card.hintBack')}
          </p>
        </div>
      }
    />
  );
}

export default VocabularyFlashCard;
