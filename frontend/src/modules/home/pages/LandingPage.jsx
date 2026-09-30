import { useState } from 'react';
import './LandingPage.css';

const MODES = [
  { key: 'vocabulary', label: 'Từ vựng', icon: 'style' },
  { key: 'dictation', label: 'Nghe chép', icon: 'hearing' },
  { key: 'shadowing', label: 'Shadowing', icon: 'mic' },
];
const OVERVIEW_ITEMS = [
  {
    icon: 'style',
    title: 'Kho từ vựng',
    description: 'Flashcard thông minh với lịch ôn tập cá nhân.',
  },
  { icon: 'hearing', title: 'Nghe chép', description: 'Bài luyện nghe theo ngữ cảnh đời sống.' },
  { icon: 'mic', title: 'Shadowing AI', description: 'Phản hồi phát âm theo từng âm tiết.' },
  { icon: 'bolt', title: 'Arena 1vs1', description: 'Đấu trường từ vựng theo thời gian thực.' },
  { icon: 'groups', title: 'Cộng đồng', description: 'Cùng học, chia sẻ và duy trì động lực.' },
];
const FEATURE_ITEMS = [
  [
    'view_carousel',
    'Vocabulary Spaced Repetition',
    'Học từ vựng thông minh qua Flashcard 2 mặt kèm phiên âm IPA, hình ảnh minh họa và thuật toán nhắc học từ vựng.',
    'Tìm hiểu thuật toán',
  ],
  [
    'hearing',
    'Dictation Nghe Chép Chính Tả',
    'Luyện nghe qua bài tập chép chính tả từ hội thoại đời sống, tin tức ngắn và phim tài liệu với tốc độ tùy chỉnh.',
    'Khám phá kho audio',
  ],
  [
    'mic',
    'Shadowing & AI Pronunciation',
    'Luyện nói theo giọng bản ngữ, phân tích ngữ điệu và phát hiện lỗi phát âm từng nguyên âm, phụ âm.',
    'Luyện nói thử',
  ],
  [
    'bolt',
    'Vocabulary Arena 1vs1',
    'Thi đấu từ vựng trực tiếp theo thời gian thực để tranh thứ hạng hàng tuần.',
    'Vào đấu trường',
  ],
  [
    'forum',
    'Cộng Đồng Học Tập Tương Tác',
    'Không gian giao lưu, chia sẻ kinh nghiệm học tập thực chiến và tìm đồng đội luyện phản xạ mỗi ngày.',
    'Tham gia thảo luận',
  ],
  [
    'workspace_premium',
    'Lộ Trình & Phân Tích Chuyên Sâu',
    'Mở khóa kho tài liệu không giới hạn.',
    'Xem quyền lợi Pro',
  ],
];
const PLAN_FEATURES = {
  basic: [
    'Kho từ vựng cơ bản',
    'Kho bài nghe chép Dictation cơ bản',
    '3 trận đấu Arena 1vs1 mỗi ngày',
    'Tham gia thảo luận cộng đồng',
  ],
  pro: [
    'Không giới hạn bộ từ vựng',
    'Toàn bộ kho bài tập Dictation chuyên sâu',
    'Đấu trường Arena 1vs1 không giới hạn lượt chơi',
  ],
};

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
  const [activeMode, setActiveMode] = useState('vocabulary');
  const handleNavigate = (path) => {
    if (onNavigate) onNavigate(path);
  };
  return (
    <main className="landing-page">
      <section className="landing-hero container" id="hero">
        <div className="landing-hero__content">
          <span className="landing-hero__eyebrow glass-capsule">
            <i />
            Học tiếng Anh mỗi ngày
          </span>
          <h1 className="landing-hero__title">
            Học tiếng Anh tự nhiên, ghi nhớ sâu & tự tin giao tiếp cùng <span>ELINGO</span>
          </h1>
          <p className="landing-hero__description">
            Nền tảng kết hợp phương pháp Spaced Repetition khoa học, luyện Shadowing và đấu
            trường từ vựng 1vs1 sôi động.
          </p>
          <div className="landing-hero__actions">
            <button className="btn-primary" onClick={() => handleNavigate('/signup')}>
              Bắt đầu học
            </button>
            <a className="btn-secondary" href="#features">
              Khám phá tính năng
            </a>
          </div>
          <div className="landing-hero__stats">
            {[
              ['45K+', 'Người học tích cực'],
              ['4.9 / 5', 'Đánh giá hài lòng'],
              ['92%', 'Tăng phản xạ 30 ngày'],
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
                Bài học hôm nay
              </span>
              <button className="btn-icon" aria-label="Lưu bài học">
                <span className="material-symbols-outlined">bookmark</span>
              </button>
            </div>
            <div className="landing-hero__word glass-well">
              <div className="landing-hero__word-title">
                <h3>Resilient</h3>
                <span>/rɪˈzɪl.jənt/ • adj</span>
              </div>
              <p>Có khả năng phục hồi nhanh chóng, kiên cường vượt qua khó khăn.</p>
              <div className="landing-hero__word-actions">
                <button className="landing-page__small-button glass-pill">
                  <span className="material-symbols-outlined">volume_up</span>Nghe phát âm
                </button>
                <button className="landing-page__small-button glass-thumb">Ví dụ câu mẫu</button>
              </div>
            </div>
            <div className="landing-hero__metrics">
              {[
                ['Streak học tập', '14 ngày'],
                ['Từ vựng nhớ', '320 từ'],
                ['Arena 1vs1', 'Top 5%'],
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
      <section className="landing-mode container" aria-label="Chế độ học tập">
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
      <section className="landing-section container">
        <SectionHeading
          badge="Giới thiệu ELINGO"
          title="Tất cả những gì bạn cần để làm chủ tiếng Anh"
          description="ELINGO đồng hành cùng bạn trong suốt quá trình học: học từ vựng, luyện nghe và cải thiện phát âm"
        />
        <div className="landing-overview">
          {OVERVIEW_ITEMS.map((item) => (
            <article className="landing-overview__card glass-card" key={item.title}>
              <div className="landing-overview__icon glass-thumb">
                <span className="material-symbols-outlined">{item.icon}</span>
              </div>
              <h3>{item.title}</h3>
              <p>{item.description}</p>
            </article>
          ))}
        </div>
      </section>
      <section className="landing-section container" id="features">
        <SectionHeading
          badge="Đặc quyền phương pháp"
          title="Tính năng cốt lõi được thiết kế cho sự tiến bộ bền vững"
          description="Mỗi công cụ được tinh chỉnh để giải quyết triệt để rào cản sợ nói, nhanh quên từ và thiếu kiên trì."
        />
        <div className="landing-feature-grid">
          {FEATURE_ITEMS.map(([icon, title, description, action]) => (
            <article className="landing-feature-card glass-card" key={title}>
              <div>
                <div className="landing-feature-card__icon glass-thumb">
                  <span className="material-symbols-outlined">{icon}</span>
                </div>
                <h3>{title}</h3>
                <p>{description}</p>
              </div>
              <button
                className="landing-feature-card__action"
                onClick={() => handleNavigate('/signup')}
              >
                {action}
                <span className="material-symbols-outlined">arrow_forward</span>
              </button>
            </article>
          ))}
        </div>
      </section>
      <section className="landing-section container" id="learning">
        <SectionHeading
          badge="Trải nghiệm trực quan"
          title="Lộ trình học tập 4 bước tinh gọn"
          description="Học → Luyện tập → Ôn tập → Tiến bộ: Một lộ trình đơn giản, giúp bạn dễ dàng duy trì thói quen học và tiến bộ mỗi ngày."
        />
        <div className="landing-steps">
          <StepCard
            step="Bước 1: Học"
            icon="menu_book"
            title="Học từ qua Flashcard"
            kind="flashcard"
            description="Học từ vựng, ngữ pháp dễ nhớ hơn với định nghĩa rõ ràng và cách phát âm chuẩn."
          />
          <StepCard
            step="Bước 2: Luyện"
            icon="edit_note"
            title="Nghe & Điền Chính Tả"
            kind="dictation"
            description="Luyện nghe qua những tình huống thực tế, giúp bạn nghe và hiểu tiếng Anh tốt hơn."
          />
          <StepCard
            step="Bước 3: Ôn tập"
            icon="cached"
            title="Lặp Lại Ngắt Quãng"
            kind="review"
            description="Ôn tập đúng lúc, giúp từ vựng được ghi nhớ lâu hơn."
          />
          <StepCard
            step="Bước 4: Tiến bộ"
            icon="insights"
            title="Theo Dõi Kết Quả"
            kind="progress"
            description="Theo dõi tiến độ và nhìn thấy sự tiến bộ của bạn qua từng ngày."
          />
        </div>
      </section>
      <section className="landing-section container" id="community">
        <SectionHeading
          badge="Đấu trường & Cộng đồng"
          title="Học tập không cô đơn - Kết nối & Đua tài"
          description="Thực chiến với hàng nghìn học viên cùng trình độ qua trận đấu 1vs1 và thảo luận nhóm bài học sôi động."
        />
        <div className="landing-community">
          <ArenaPreview />
          <article className="landing-feed glass-capsule">
            <div className="landing-panel__top">
              <span className="landing-page__chip glass-thumb">CỘNG ĐỒNG HỌC VIÊN</span>
              <small>1.4k thành viên online</small>
            </div>
            <div>
              <h3>Chia sẻ & Thảo luận</h3>
              <p>
                Học hỏi kinh nghiệm thực tế, tra cứu thắc mắc ngữ pháp và tìm bạn cùng luyện giọng.
              </p>
            </div>
            <CommunityPost
              initials="LA"
              name="Lan Anh"
              time="15 phút trước • #IELTS_Shadowing"
              likes="24"
              comments="9"
              text="Hôm nay hoàn thành 15 bài Dictation về chủ đề Environment. Ai muốn luyện nói Shadowing cặp đôi tối nay lúc 20:00 không ạ?"
            />
            <CommunityPost
              initials="HN"
              name="Hoàng Nam"
              time="1 giờ trước • #Vocabulary_Tips"
              likes="58"
              comments="18"
              text="Tip nhớ từ vựng lâu: kết hợp Spaced Repetition + tự viết lại câu mẫu đời sống của chính mình. Sau 2 tuần thuộc 180 từ tự nhiên!"
            />
          </article>
        </div>
      </section>
      <section className="landing-section container" id="pricing">
        <SectionHeading
          badge="Bảng giá minh bạch"
          title="Lựa chọn gói học phù hợp với mục tiêu của bạn"
          description="Bắt đầu miễn phí hoặc nâng cấp để tiếp cận toàn bộ tính năng cao cấp không giới hạn."
        />
        <div className="landing-pricing">
          <PlanCard
            name="Gói Cơ Bản"
            price="0đ"
            period="/ Vĩnh viễn"
            description="Phù hợp cho người mới bắt đầu làm quen với phương pháp học phản xạ tự nhiên."
            features={PLAN_FEATURES.basic}
            buttonLabel="Sử dụng miễn phí"
            onClick={() => handleNavigate('/signup')}
          />
          <PlanCard
            isPro
            name="Gói ELINGO Pro"
            price="89.000đ"
            period="/ tháng"
            description="Hoặc tiết kiệm hơn với 799.000đ / năm (chỉ ~66.000đ / tháng)."
            features={PLAN_FEATURES.pro}
            buttonLabel="Nâng cấp Premium"
            onClick={() => handleNavigate('/signup')}
          />
        </div>
      </section>
      <section className="landing-final-cta container">
        <div className="landing-final-cta__panel glass-capsule glass-sheen">
          <div className="landing-final-cta__icon glass-thumb">
            <span className="material-symbols-outlined">auto_stories</span>
          </div>
          <h2 className="text-headline-lg">Sẵn sàng bắt đầu hành trình học tiếng Anh?</h2>
          <p className="text-body-lg">
            Học một chút mỗi ngày và tiến bộ từng bước cùng phương pháp của ELINGO.
          </p>
          <button className="btn-primary" onClick={() => handleNavigate('/signup')}>
            Bắt đầu học ngay miễn phí
          </button>
          <div className="landing-final-cta__benefits">
            <span className="glass-thumb">
              <i className="material-symbols-outlined">verified</i>Miễn phí trải nghiệm
            </span>
            <span className="glass-thumb">
              <i className="material-symbols-outlined">credit_card_off</i>Không yêu cầu thẻ tín dụng
            </span>
            <span className="glass-thumb">
              <i className="material-symbols-outlined">lock_reset</i>Hủy bất kỳ lúc nào
            </span>
          </div>
        </div>
      </section>
    </main>
  );
}

function StepCard({ step, icon, title, kind, description }) {
  const previews = {
    flashcard: (
      <>
        <strong>Eloquent</strong>
        <span>/ˈel.ə.kwənt/ • adj</span>
        <p>Có tài hùng biện, diễn đạt lưu loát gãy gọn.</p>
        <b>
          <span className="material-symbols-outlined">graphic_eq</span>Nghe phát âm chuẩn
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
          "She delivered an <b>[ _____ ]</b> speech."
        </p>
        <b>Bấm nghe lại câu mẫu (1.0x)</b>
      </>
    ),
    review: (
      <>
        <div className="landing-step__memory">
          <span>Độ bền trí nhớ</span>
          <b>96%</b>
        </div>
        <div className="landing-step__bar">
          <i />
        </div>
        <span>
          <span className="material-symbols-outlined">alarm</span>Nhắc lại sau 3 ngày
        </span>
      </>
    ),
    progress: (
      <>
        <div>
          <strong>Level 14</strong>
          <span>85% Đạt mục tiêu tháng</span>
        </div>
        <div className="landing-step__ring">85%</div>
      </>
    ),
  };
  return (
    <article className={`landing-step landing-step--${kind} glass-card`}>
      <div className="landing-step__top">
        <span className="landing-page__chip glass-thumb">{step}</span>
        <span className="material-symbols-outlined">{icon}</span>
      </div>
      <h3>{title}</h3>
      <div className="landing-step__well glass-well">{previews[kind]}</div>
      <p>{description}</p>
    </article>
  );
}
function ArenaPreview() {
  return (
    <article className="landing-arena glass-capsule">
      <div className="landing-panel__top">
        <span className="landing-page__chip glass-thumb">ARENA 1VS1</span>
        <span className="landing-arena__timer glass-thumb">08s</span>
      </div>
      <div>
        <h3>Đấu trường từ vựng 1vs1</h3>
        <p>Thử thách phản xạ từ vựng trực tiếp cùng bạn bè với thời gian đếm ngược kịch tính.</p>
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
        <span>Từ đồng nghĩa với "Persistent"?</span>
        <strong>"Never giving up despite difficulties"</strong>
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
      <small>Thời gian trả lời trung bình: 3.2s • Thắng +30 XP</small>
    </article>
  );
}
function CommunityPost({ initials, name, time, text, likes, comments }) {
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
          {comments} bình luận
        </button>
      </div>
    </article>
  );
}
function PlanCard({
  name,
  price,
  period,
  description,
  features,
  buttonLabel,
  isPro = false,
  onClick,
}) {
  return (
    <article
      className={`landing-plan ${isPro ? 'landing-plan--pro glass-pro glass-sheen' : 'glass-card'}`}
    >
      <span className="landing-plan__popular glass-thumb">
        {isPro ? 'Được yêu thích nhất' : 'Miễn phí'}
      </span>
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
          {features.map((feature) => (
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
