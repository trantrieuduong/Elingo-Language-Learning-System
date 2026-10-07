import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import './LandingPage.css';

function SectionHeading({ badge, title, description }) {
  return (
    <div className="landing-page__heading">
      <span className="landing-page__badge glass-capsule">{badge}</span>
      <h2 className="text-headline-lg">{title}</h2>
      <p className="text-body-md">{description}</p>
    </div>
  );
}

function LandingPage({ onNavigate }) {
  const { t } = useTranslation('home');
  const [activeMode, setActiveMode] = useState('vocabulary');

  const handleNavigate = (path) => {
    if (onNavigate) onNavigate(path);
  };

  const MODES = [
    { key: 'vocabulary', label: t('modes.vocabulary'), icon: 'style' },
    { key: 'dictation', label: t('modes.dictation'), icon: 'hearing' },
    { key: 'shadowing', label: t('modes.shadowing'), icon: 'mic' },
  ];

  const OVERVIEW_ITEMS = [
    { key: 'vocabulary', icon: 'style' },
    { key: 'dictation', icon: 'hearing' },
    { key: 'shadowing', icon: 'mic' },
    { key: 'arena', icon: 'bolt' },
    { key: 'community', icon: 'groups' },
  ];

  const FEATURE_KEYS = ['vocabulary', 'dictation', 'shadowing', 'arena', 'community', 'premium'];
  const FEATURE_ICONS = {
    vocabulary: 'view_carousel',
    dictation: 'hearing',
    shadowing: 'mic',
    arena: 'bolt',
    community: 'forum',
    premium: 'workspace_premium',
  };

  return (
    <main className="landing-page">
      {/* ── HERO ── */}
      <section className="landing-hero container" id="hero">
        <div className="landing-hero__content">
          <span className="landing-hero__eyebrow glass-capsule">
            <i />
            {t('hero.eyebrow')}
          </span>
          <h1 className="landing-hero__title">
            {t('hero.title').split('<brand>')[0]}
            <span>ELINGO</span>
            {t('hero.title').split('</brand>')[1]}
          </h1>
          <p className="landing-hero__description">{t('hero.description')}</p>
          <div className="landing-hero__actions">
            <button className="btn-primary" onClick={() => handleNavigate('/signup')}>
              {t('hero.cta')}
            </button>
            <a className="btn-secondary" href="#features">
              {t('hero.ctaSecondary')}
            </a>
          </div>
          <div className="landing-hero__stats">
            {[
              ['45K+', t('hero.stats.learners')],
              ['4.9 / 5', t('hero.stats.rating')],
              ['92%', t('hero.stats.reflex')],
            ].map(([value, label]) => (
              <div className="landing-hero__stat glass-card" key={label}>
                <strong>{value}</strong>
                <span>{label}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="landing-hero__preview-wrap">
          <span className="landing-hero__floating-label glass-capsule">Shadowing 2.0</span>
          <article className="landing-hero__preview glass-capsule glass-sheen">
            <div className="landing-hero__preview-top">
              <span className="landing-page__chip glass-pill">
                <i />
                {t('hero.preview.chip')}
              </span>
              <button className="btn-icon" aria-label={t('hero.preview.saveLesson')}>
                <span className="material-symbols-outlined">bookmark</span>
              </button>
            </div>
            <div className="landing-hero__word glass-well">
              <div className="landing-hero__word-title">
                <h3>Resilient</h3>
                <span>/rɪˈzɪl.jənt/ • adj</span>
              </div>
              <p>{t('hero.preview.wordDesc')}</p>
              <div className="landing-hero__word-actions">
                <button className="landing-page__small-button glass-pill">
                  <span className="material-symbols-outlined">volume_up</span>
                  {t('hero.preview.listenBtn')}
                </button>
                <button className="landing-page__small-button glass-thumb">{t('hero.preview.exampleBtn')}</button>
              </div>
            </div>
            <div className="landing-hero__metrics">
              {[
                [t('hero.preview.streakLabel'), t('hero.preview.streakVal')],
                [t('hero.preview.vocabLabel'), t('hero.preview.vocabVal')],
                [t('hero.preview.arenaLabel'), t('hero.preview.arenaVal')],
              ].map(([label, value]) => (
                <div className="glass-pill" key={label}>
                  <span>{label}</span>
                  <strong>{value}</strong>
                </div>
              ))}
            </div>
          </article>
        </div>
      </section>

      {/* ── MODE SWITCHER ── */}
      <section className="landing-mode container" aria-label={t('modes.ariaLabel')}>
        <div className="landing-mode__switcher glass-capsule">
          {MODES.map((mode) => (
            <button
              key={mode.key}
              type="button"
              className={
                activeMode === mode.key
                  ? 'landing-mode__button landing-mode__button--active glass-thumb'
                  : 'landing-mode__button'
              }
              onClick={() => setActiveMode(mode.key)}
            >
              <span className="material-symbols-outlined">{mode.icon}</span>
              {mode.label}
            </button>
          ))}
        </div>
      </section>

      {/* ── OVERVIEW ── */}
      <section className="landing-section container">
        <SectionHeading
          badge={t('overview.badge')}
          title={t('overview.title')}
          description={t('overview.description')}
        />
        <div className="landing-overview">
          {OVERVIEW_ITEMS.map((item) => (
            <article className="landing-overview__card glass-card" key={item.key}>
              <div className="landing-overview__icon glass-thumb">
                <span className="material-symbols-outlined">{item.icon}</span>
              </div>
              <h3>{t(`overview.items.${item.key}.title`)}</h3>
              <p>{t(`overview.items.${item.key}.description`)}</p>
            </article>
          ))}
        </div>
      </section>

      {/* ── FEATURES ── */}
      <section className="landing-section container" id="features">
        <SectionHeading
          badge={t('features.badge')}
          title={t('features.title')}
          description={t('features.description')}
        />
        <div className="landing-feature-grid">
          {FEATURE_KEYS.map((key) => (
            <article className="landing-feature-card glass-card" key={key}>
              <div>
                <div className="landing-feature-card__icon glass-thumb">
                  <span className="material-symbols-outlined">{FEATURE_ICONS[key]}</span>
                </div>
                <h3>{t(`features.items.${key}.title`)}</h3>
                <p>{t(`features.items.${key}.description`)}</p>
              </div>
              <button
                className="landing-feature-card__action"
                onClick={() => handleNavigate('/signup')}
              >
                {t(`features.items.${key}.action`)}
                <span className="material-symbols-outlined">arrow_forward</span>
              </button>
            </article>
          ))}
        </div>
      </section>

      {/* ── LEARNING STEPS ── */}
      <section className="landing-section container" id="learning">
        <SectionHeading
          badge={t('learning.badge')}
          title={t('learning.title')}
          description={t('learning.description')}
        />
        <div className="landing-steps">
          <StepCard kind="flashcard" icon="menu_book" t={t} />
          <StepCard kind="dictation" icon="edit_note" t={t} />
          <StepCard kind="review" icon="cached" t={t} />
          <StepCard kind="progress" icon="insights" t={t} />
        </div>
      </section>

      {/* ── COMMUNITY ── */}
      <section className="landing-section container" id="community">
        <SectionHeading
          badge={t('community.badge')}
          title={t('community.title')}
          description={t('community.description')}
        />
        <div className="landing-community">
          <ArenaPreview t={t} />
          <article className="landing-feed glass-capsule">
            <div className="landing-panel__top">
              <span className="landing-page__chip glass-thumb">{t('community.feed.chip')}</span>
              <small>{t('community.feed.online')}</small>
            </div>
            <div>
              <h3>{t('community.feed.title')}</h3>
              <p>{t('community.feed.description')}</p>
            </div>
            <CommunityPost
              initials="LA"
              name="Lan Anh"
              time={t('community.feed.post1.time')}
              likes="24"
              comments="9"
              commentsLabel={t('community.feed.comments')}
              text={t('community.feed.post1.text')}
            />
            <CommunityPost
              initials="HN"
              name="Hoàng Nam"
              time={t('community.feed.post2.time')}
              likes="58"
              comments="18"
              commentsLabel={t('community.feed.comments')}
              text={t('community.feed.post2.text')}
            />
          </article>
        </div>
      </section>

      {/* ── PRICING ── */}
      <section className="landing-section container" id="pricing">
        <SectionHeading
          badge={t('pricing.badge')}
          title={t('pricing.title')}
          description={t('pricing.description')}
        />
        <div className="landing-pricing">
          <PlanCard
            name={t('pricing.basic.name')}
            price={t('pricing.basic.price')}
            period={t('pricing.basic.period')}
            description={t('pricing.basic.description')}
            features={t('pricing.basic.features', { returnObjects: true })}
            popularLabel={t('pricing.popularFree')}
            buttonLabel={t('pricing.basic.btn')}
            onClick={() => handleNavigate('/signup')}
          />
          <PlanCard
            isPro
            name={t('pricing.pro.name')}
            price={t('pricing.pro.price')}
            period={t('pricing.pro.period')}
            description={t('pricing.pro.description')}
            features={t('pricing.pro.features', { returnObjects: true })}
            popularLabel={t('pricing.popularPro')}
            buttonLabel={t('pricing.pro.btn')}
            onClick={() => handleNavigate('/signup')}
          />
        </div>
      </section>

      {/* ── FINAL CTA ── */}
      <section className="landing-final-cta container">
        <div className="landing-final-cta__panel glass-capsule glass-sheen">
          <div className="landing-final-cta__icon glass-thumb">
            <span className="material-symbols-outlined">auto_stories</span>
          </div>
          <h2 className="text-headline-lg">{t('cta.title')}</h2>
          <p className="text-body-lg">{t('cta.description')}</p>
          <button className="btn-primary" onClick={() => handleNavigate('/signup')}>
            {t('cta.btn')}
          </button>
          <div className="landing-final-cta__benefits">
            <span className="glass-thumb">
              <i className="material-symbols-outlined">verified</i>{t('cta.benefit1')}
            </span>
            <span className="glass-thumb">
              <i className="material-symbols-outlined">credit_card_off</i>{t('cta.benefit2')}
            </span>
            <span className="glass-thumb">
              <i className="material-symbols-outlined">lock_reset</i>{t('cta.benefit3')}
            </span>
          </div>
        </div>
      </section>
    </main>
  );
}

function StepCard({ kind, icon, t }) {
  const previews = {
    flashcard: (
      <>
        <strong>Eloquent</strong>
        <span>/ˈel.ə.kwənt/ • adj</span>
        <p>Có tài hùng biện, diễn đạt lưu loát gãy gọn.</p>
        <b>
          <span className="material-symbols-outlined">graphic_eq</span>
          {t('learning.steps.flashcard.listenBtn')}
        </b>
      </>
    ),
    dictation: (
      <>
        <div className="landing-step__wave">
          {Array.from({ length: 7 }, (_, index) => (
            <i key={index} />
          ))}
        </div>
        <p>
          &quot;She delivered an <b>[ _____ ]</b> speech.&quot;
        </p>
        <b>{t('learning.steps.dictation.playBtn')}</b>
      </>
    ),
    review: (
      <>
        <div className="landing-step__memory">
          <span>{t('learning.steps.review.memoryLabel')}</span>
          <b>96%</b>
        </div>
        <div className="landing-step__bar">
          <i />
        </div>
        <span>
          <span className="material-symbols-outlined">alarm</span>
          {t('learning.steps.review.reminderLabel')}
        </span>
      </>
    ),
    progress: (
      <>
        <div>
          <strong>Level 14</strong>
          <span>85% {t('learning.steps.progress.monthGoal')}</span>
        </div>
        <div className="landing-step__ring">85%</div>
      </>
    ),
  };

  return (
    <article className={`landing-step landing-step--${kind} glass-card`}>
      <div className="landing-step__top">
        <span className="landing-page__chip glass-thumb">{t(`learning.steps.${kind}.step`)}</span>
        <span className="material-symbols-outlined">{icon}</span>
      </div>
      <h3>{t(`learning.steps.${kind}.title`)}</h3>
      <div className="landing-step__well glass-well">{previews[kind]}</div>
      <p>{t(`learning.steps.${kind}.description`)}</p>
    </article>
  );
}

function ArenaPreview({ t }) {
  return (
    <article className="landing-arena glass-capsule">
      <div className="landing-panel__top">
        <span className="landing-page__chip glass-thumb">{t('community.arena.chip')}</span>
        <span className="landing-arena__timer glass-thumb">08s</span>
      </div>
      <div>
        <h3>{t('community.arena.title')}</h3>
        <p>{t('community.arena.description')}</p>
      </div>
      <div className="landing-arena__players glass-well">
        <div>
          <b>MT</b>
          <span>
            <strong>Minh Triết</strong>
            <small>Level 12 • 450 pts</small>
          </span>
        </div>
        <em>VS</em>
        <div>
          <b>TH</b>
          <span>
            <strong>Thu Hà</strong>
            <small>Level 14 • 480 pts</small>
          </span>
        </div>
      </div>
      <div className="landing-arena__question glass-well">
        <span>{t('community.arena.question')}</span>
        <strong>&quot;Never giving up despite difficulties&quot;</strong>
      </div>
      <div className="landing-arena__options">
        <button className="landing-arena__option landing-arena__option--correct glass-thumb">
          A. Tenacious <span className="material-symbols-outlined">check_circle</span>
        </button>
        {['B. Fragile', 'C. Casual', 'D. Hesitant'].map((option) => (
          <button className="landing-arena__option glass-pill" key={option}>
            {option}
          </button>
        ))}
      </div>
      <small>{t('community.arena.avgTime')}</small>
    </article>
  );
}

function CommunityPost({ initials, name, time, text, likes, comments, commentsLabel }) {
  return (
    <article className="landing-post glass-well">
      <div className="landing-post__author">
        <b className="glass-thumb">{initials}</b>
        <span>
          <strong>{name}</strong>
          <small>{time}</small>
        </span>
      </div>
      <p>{text}</p>
      <div className="landing-post__actions">
        <button className="glass-thumb">♥ {likes}</button>
        <button className="glass-thumb">
          <span className="material-symbols-outlined">chat_bubble</span>
          {comments} {commentsLabel}
        </button>
      </div>
    </article>
  );
}

function PlanCard({ name, price, period, description, features, popularLabel, buttonLabel, isPro = false, onClick }) {
  return (
    <article className={`landing-plan ${isPro ? 'landing-plan--pro glass-pro glass-sheen' : 'glass-card'}`}>
      <span className="landing-plan__popular glass-thumb">{popularLabel}</span>
      <div>
        <div className="landing-plan__title">
          <h3>{name}</h3>
        </div>
        <div className="landing-plan__price">
          <strong>{price}</strong>
          <span>{period}</span>
        </div>
        <p>{description}</p>
        <ul>
          {Array.isArray(features) && features.map((feature) => (
            <li key={feature}>
              <span className="material-symbols-outlined">{isPro ? 'check_circle' : 'check'}</span>
              {feature}
            </li>
          ))}
        </ul>
      </div>
      <button className={isPro ? 'btn-primary' : 'btn-secondary'} onClick={onClick}>
        {buttonLabel}
      </button>
    </article>
  );
}

export default LandingPage;
