# Thiết kế CSDL MySQL — Elingo

Quy ước chung: khóa chính `BIGINT UNSIGNED AUTO_INCREMENT`, timestamp `created_at`/`updated_at` (`DATETIME DEFAULT CURRENT_TIMESTAMP` / `ON UPDATE CURRENT_TIMESTAMP`), charset `utf8mb4`, engine `InnoDB` để dùng được FK + transaction.

---

## 1. Users

```sql
CREATE TABLE users (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  username VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(150) NOT NULL,
  avatar_url VARCHAR(500),
  role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  is_verified BOOLEAN NOT NULL DEFAULT FALSE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  ban_reason VARCHAR(500),
  password_changed_at DATETIME,
  username_changed_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 2. Vocabulary (Deck / Topic / Card)

```sql
CREATE TABLE tags (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(100) NOT NULL UNIQUE,
  label VARCHAR(150) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cefr_levels (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(10) NOT NULL UNIQUE,     -- A1, A2, B1, B2, C1, C2
  label VARCHAR(100) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE decks (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  slug VARCHAR(255) NOT NULL UNIQUE,
  description TEXT,
  is_premium BOOLEAN NOT NULL DEFAULT FALSE,
  cover_image_url VARCHAR(500),
  status ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
  owner_type ENUM('SYSTEM','USER') NOT NULL DEFAULT 'SYSTEM',
  owner_id BIGINT UNSIGNED NULL,        -- NULL nếu owner_type = SYSTEM
  topic_count INT UNSIGNED NOT NULL DEFAULT 0,
  card_count INT UNSIGNED NOT NULL DEFAULT 0,
  published_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_deck_owner (owner_type, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng junction thay cho mảng tagIds[] / cefrLevelIds[] nhúng trong deck
CREATE TABLE deck_tags (
  deck_id BIGINT UNSIGNED NOT NULL,
  tag_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (deck_id, tag_id),
  FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE deck_cefr_levels (
  deck_id BIGINT UNSIGNED NOT NULL,
  cefr_level_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (deck_id, cefr_level_id),
  FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE,
  FOREIGN KEY (cefr_level_id) REFERENCES cefr_levels(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE topics (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  deck_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(255) NOT NULL,
  slug VARCHAR(255) NOT NULL,
  `order` INT UNSIGNED NOT NULL DEFAULT 0,
  card_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE,
  UNIQUE KEY uq_topic_slug_per_deck (deck_id, slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cards (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  deck_id BIGINT UNSIGNED NOT NULL,
  topic_id BIGINT UNSIGNED NOT NULL,
  `order` INT UNSIGNED NOT NULL DEFAULT 0,
  term VARCHAR(255) NOT NULL,
  pos VARCHAR(50),                       -- part of speech
  translation VARCHAR(500),
  explanation_vi TEXT,
  explanation_en TEXT,
  examples_vi TEXT,
  examples_en TEXT,
  image_url VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE,
  FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL,
  INDEX idx_card_deck (deck_id),
  INDEX idx_card_topic (topic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tách mảng nhúng phonetics[] thành bảng con
CREATE TABLE card_phonetics (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  card_id BIGINT UNSIGNED NOT NULL,
  text VARCHAR(255) NOT NULL,
  audio_url VARCHAR(500),
  locale VARCHAR(10) NOT NULL DEFAULT 'en-US',
  FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_card_states (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  card_id BIGINT UNSIGNED NOT NULL,
  deck_id BIGINT UNSIGNED NOT NULL,
  topic_id BIGINT UNSIGNED NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  srs_ease_factor DECIMAL(4,2) NOT NULL DEFAULT 2.50,
  srs_interval INT UNSIGNED NOT NULL DEFAULT 0,
  srs_last_grade TINYINT UNSIGNED NULL,   -- 0..3 (Again/Hard/Good/Easy)
  srs_next_review_at DATETIME NULL,
  flags_starred BOOLEAN NOT NULL DEFAULT FALSE,
  flags_hidden BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
  FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE,
  FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_user_card (user_id, card_id),
  INDEX idx_due_review (user_id, srs_next_review_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Ghi chú tối ưu:** thêm `INDEX idx_due_review (user_id, srs_next_review_at)`

---

## 3. Learning — Lesson (video, dictation, shadowing)

```sql
CREATE TABLE lessons (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  slug VARCHAR(255) NOT NULL UNIQUE,
  description TEXT,
  is_premium BOOLEAN NOT NULL DEFAULT FALSE,
  status ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
  published_at DATETIME NULL,
  duration_ms INT UNSIGNED,
  source_url VARCHAR(500) NOT NULL,      -- link YouTube
  thumbnail_url VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lesson_tags (
  lesson_id BIGINT UNSIGNED NOT NULL,
  tag_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (lesson_id, tag_id),
  FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lesson_cefr_levels (
  lesson_id BIGINT UNSIGNED NOT NULL,
  cefr_level_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (lesson_id, cefr_level_id),
  FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
  FOREIGN KEY (cefr_level_id) REFERENCES cefr_levels(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lesson_segments (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  lesson_id BIGINT UNSIGNED NOT NULL,
  start_ms INT UNSIGNED NOT NULL,
  end_ms INT UNSIGNED NOT NULL,
  transcript_original TEXT NOT NULL,
  transcript_normalized TEXT NOT NULL,   -- dùng để so khớp dictation, đã bỏ dấu câu/chuẩn hoá
  translation TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
  INDEX idx_segment_lesson (lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_lesson_progress (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  lesson_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  dictation_status ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'NOT_STARTED',
  dictation_progress_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
  dictation_last_start_ms INT UNSIGNED,
  shadowing_status ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'NOT_STARTED',
  shadowing_progress_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
  shadowing_last_start_ms INT UNSIGNED,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_user_lesson (user_id, lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_segment_progress (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  lesson_id BIGINT UNSIGNED NOT NULL,
  segment_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  dictation_attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
  dictation_best_score DECIMAL(5,2) NOT NULL DEFAULT 0,
  dictation_hint_used_count INT UNSIGNED NOT NULL DEFAULT 0,
  shadowing_attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
  shadowing_best_score DECIMAL(5,2) NOT NULL DEFAULT 0,
  shadowing_latest_audio_url VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
  FOREIGN KEY (segment_id) REFERENCES lesson_segments(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_user_segment (user_id, segment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE segment_word_accuracy (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_segment_progress_id BIGINT UNSIGNED NOT NULL,
  word VARCHAR(100) NOT NULL,
  accuracy DECIMAL(5,2) NOT NULL,
  FOREIGN KEY (user_segment_progress_id) REFERENCES user_segment_progress(id) ON DELETE CASCADE,
  INDEX idx_word (user_segment_progress_id, word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 4. Battle

```sql
CREATE TABLE battle_matches (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  mode ENUM('MULTIPLE_CHOICE','TYPING') NOT NULL,
  match_type ENUM('RANDOM','ROOM') NOT NULL,
  room_code VARCHAR(20) NULL UNIQUE,
  status ENUM('WAITING','IN_PROGRESS','FINISHED','CANCELLED') NOT NULL DEFAULT 'WAITING',
  winner_id BIGINT UNSIGNED NULL,
  total_rounds TINYINT UNSIGNED NOT NULL DEFAULT 10,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tách players[] nhúng thành bảng con
CREATE TABLE battle_participants (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  match_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  score INT UNSIGNED NOT NULL DEFAULT 0,
  correct_count INT UNSIGNED NOT NULL DEFAULT 0,
  is_winner BOOLEAN NOT NULL,
  FOREIGN KEY (match_id) REFERENCES battle_matches(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_match_user (match_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tách questions[] nhúng thành bảng con
CREATE TABLE battle_questions (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  match_id BIGINT UNSIGNED NOT NULL,
  card_id BIGINT UNSIGNED NULL,          -- câu hỏi sinh từ card nào (để thống kê độ khó theo từ vựng)
  round_no TINYINT UNSIGNED NOT NULL,
  term VARCHAR(255) NOT NULL,
  correct_answer VARCHAR(255) NOT NULL,
  time_limit_ms INT UNSIGNED NOT NULL DEFAULT 15000,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (match_id) REFERENCES battle_matches(id) ON DELETE CASCADE,
  FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE SET NULL,
  UNIQUE KEY uq_match_round (match_id, round_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Mới: tách options[] của câu hỏi trắc nghiệm thành bảng riêng thay vì JSON string[]
CREATE TABLE battle_question_options (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT UNSIGNED NOT NULL,
  option_text VARCHAR(255) NOT NULL,
  is_correct BOOLEAN NOT NULL DEFAULT FALSE,
  FOREIGN KEY (question_id) REFERENCES battle_questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Mới: bảng hoàn toàn chưa có ở thiết kế cũ — lưu lịch sử trả lời từng câu của từng người chơi
CREATE TABLE battle_answers (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT UNSIGNED NOT NULL,
  participant_id BIGINT UNSIGNED NOT NULL,
  selected_option_id BIGINT UNSIGNED NULL,   -- dùng khi mode = MULTIPLE_CHOICE
  typed_answer VARCHAR(255) NULL,            -- dùng khi mode = TYPING
  is_correct BOOLEAN NOT NULL,
  response_time_ms INT UNSIGNED NOT NULL,
  answered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (question_id) REFERENCES battle_questions(id) ON DELETE CASCADE,
  FOREIGN KEY (participant_id) REFERENCES battle_participants(id) ON DELETE CASCADE,
  FOREIGN KEY (selected_option_id) REFERENCES battle_question_options(id) ON DELETE SET NULL,
  UNIQUE KEY uq_question_participant (question_id, participant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Tóm tắt tối ưu battle:**
- `card_id` trên `battle_questions` giúp thống kê "từ vựng nào hay bị sai nhất trong battle" — dữ liệu hữu ích cho gợi ý ôn tập.
- `battle_answers` là bảng **mới hoàn toàn**, cho phép replay trận đấu, chấm điểm minh bạch, và làm nguồn sự kiện chi tiết cho `gamification` (thay vì chỉ nghe sự kiện "thắng/thua" chung chung, có thể cộng thêm XP nhỏ cho mỗi câu trả lời đúng).
- `final_rank` thay cho việc chỉ có `winnerId` ở cấp match — hỗ trợ mở rộng lên trận nhiều người (>2) sau này mà không phải đổi schema.

---

## 5. Gamification

```sql
-- Bảng cấu hình mốc XP cho từng cấp — admin chỉnh được, không hardcode trong code
CREATE TABLE levels (
  level_no TINYINT UNSIGNED PRIMARY KEY,
  required_xp INT UNSIGNED NOT NULL,
  title VARCHAR(100),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_xp (
  user_id BIGINT UNSIGNED PRIMARY KEY,
  total_xp INT UNSIGNED NOT NULL DEFAULT 0,
  level_no TINYINT UNSIGNED NOT NULL DEFAULT 1,
  last_xp_at DATETIME NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (level_no) REFERENCES levels(level_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE xp_events (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  source ENUM('FLASHCARD_REVIEW','LESSON_DICTATION','LESSON_SHADOWING',
              'BATTLE','DAILY_CHECKIN','ADMIN_ADJUST') NOT NULL,
  ref_id BIGINT UNSIGNED,
  amount INT NOT NULL,       -- cho phép âm nếu admin trừ XP
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_xp_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tách streak ra riêng khỏi user_gamification
CREATE TABLE learning_streaks (
  user_id BIGINT UNSIGNED PRIMARY KEY,
  current_streak INT UNSIGNED NOT NULL DEFAULT 0,
  longest_streak INT UNSIGNED NOT NULL DEFAULT 0,
  last_active_date DATE NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Mới: điểm danh hằng ngày (chưa có ở thiết kế cũ)
CREATE TABLE daily_checkins (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  checkin_date DATE NOT NULL,
  streak_day_no INT UNSIGNED NOT NULL,
  xp_rewarded INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_user_checkin_date (user_id, checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Mới: hệ thống huy hiệu/thành tựu (chưa có ở thiết kế cũ)
CREATE TABLE badges (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(100) NOT NULL UNIQUE,
  name VARCHAR(150) NOT NULL,
  description VARCHAR(500),
  icon_url VARCHAR(500),
  condition_type ENUM('STREAK_DAYS','TOTAL_XP','BATTLE_WINS','LESSONS_COMPLETED') NOT NULL,
  condition_value INT UNSIGNED NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_badges (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  badge_id BIGINT UNSIGNED NOT NULL,
  earned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE,
  UNIQUE KEY uq_user_badge (user_id, badge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Mới: cache bảng xếp hạng theo chu kỳ, tránh tính lại từ xp_events mỗi lần xem
CREATE TABLE leaderboard_snapshots (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  period_type ENUM('WEEKLY','MONTHLY') NOT NULL,
  period_key VARCHAR(20) NOT NULL,      -- ví dụ '2026-W35' hoặc '2026-08'
  user_id BIGINT UNSIGNED NOT NULL,
  `rank` INT UNSIGNED NOT NULL,
  total_xp INT UNSIGNED NOT NULL,
  generated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_period_user (period_type, period_key, user_id),
  INDEX idx_period_rank (period_type, period_key, `rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Tóm tắt tối ưu gamification:**
- Tách `learning_streaks` khỏi bảng XP/level — 2 khái niệm độc lập, dễ audit riêng.
- Thêm `levels` (bảng cấu hình) thay vì hardcode ngưỡng XP trong code → admin điều chỉnh độ khó lên cấp mà không cần deploy lại.
- Thêm `daily_checkins`, `badges`, `user_badges`, `leaderboard_snapshots` — đúng theo các tính năng XP/streak/check-in đã thống nhất ở phần kiến trúc backend, nhưng ERD cũ hoàn toàn chưa có.
- `leaderboard_snapshots` là **cache**, không phải nguồn sự thật — sinh định kỳ bằng scheduled job (`@Scheduled` trong `gamification/`) từ `xp_events`, giúp tránh aggregate nặng mỗi lần người dùng mở bảng xếp hạng.

---

## 6. Community / Social (mới)

```sql
CREATE TABLE post_groups (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  group_name VARCHAR(255) NOT NULL,
  description VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE posts (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  group_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  status ENUM('PENDING','NEEDS_REVIEW','PUBLISHED','HIDDEN','REJECTED') NOT NULL DEFAULT 'PENDING',
  like_count INT UNSIGNED NOT NULL DEFAULT 0,
  comment_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (group_id) REFERENCES post_groups(id) ON DELETE RESTRICT,
  INDEX idx_post_status_time (status, created_at) -- cần tối ưu lại
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_moderation_logs (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT UNSIGNED NOT NULL,
  actor_type ENUM('AI','ADMIN') NOT NULL,
  actor_id BIGINT UNSIGNED NULL,              -- id admin nếu actor_type = ADMIN, NULL nếu AI
  action ENUM('AUTO_APPROVE','AUTO_REJECT','FLAG_FOR_REVIEW',
              'MANUAL_APPROVE','MANUAL_REJECT','MANUAL_HIDE') NOT NULL,
  ai_label VARCHAR(100) NULL,                 -- ví dụ 'SPAM', 'HATE_SPEECH', 'CLEAN' (chỉ có khi actor_type = AI)
  ai_score DECIMAL(5,4) NULL,                 -- độ tin cậy AI trả về (chỉ có khi actor_type = AI)
  note VARCHAR(500) NULL,                     -- ghi chú của admin khi duyệt tay
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_log_post (post_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_media (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT UNSIGNED NOT NULL,
  media_type ENUM('IMAGE','VIDEO') NOT NULL,
  media_url VARCHAR(500) NOT NULL,
  `order` INT UNSIGNED NOT NULL DEFAULT 0,
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE comments (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  parent_comment_id BIGINT UNSIGNED NULL,   -- hỗ trợ reply lồng 1 cấp
  content TEXT NOT NULL,
  image_url VARCHAR(500) NULL,
  like_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE,
  INDEX idx_comment_post (post_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Like cho comment
CREATE TABLE comment_likes (
  comment_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (comment_id, user_id),
  FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_likes (
  post_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (post_id, user_id),
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 7. Report (mới)

```sql
CREATE TABLE reports (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  reporter_id BIGINT UNSIGNED NOT NULL,
  target_type ENUM('POST','COMMENT','USER','DECK') NOT NULL,
  target_id BIGINT UNSIGNED NOT NULL,    -- validate ở tầng service
  description VARCHAR(1000),
  status ENUM('PENDING','RESOLVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  resolved_by BIGINT UNSIGNED NULL,
  resolved_at DATETIME NULL,
  resolution_note VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_report_target (target_type, target_id),
  INDEX idx_report_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE report_images (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  report_id BIGINT UNSIGNED NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 8. Premium & Payment

```sql
CREATE TABLE payment_providers (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,     -- 'SEPAY', 'VNPAY', 'MOMO'...
  name VARCHAR(150) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE premium_plans (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(10) NOT NULL DEFAULT 'VND',
  duration_days INT UNSIGNED NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_subscriptions (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  plan_id BIGINT UNSIGNED NOT NULL,
  status ENUM('ACTIVE','EXPIRED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (plan_id) REFERENCES premium_plans(id),
  INDEX idx_active_sub (user_id, status, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_transactions (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  subscription_id BIGINT UNSIGNED NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  plan_id BIGINT UNSIGNED NOT NULL,
  provider_id BIGINT UNSIGNED NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  currency VARCHAR(10) NOT NULL DEFAULT 'VND',
  provider_ref_code VARCHAR(100) NOT NULL UNIQUE,  -- mã đối soát: transfer_content (SePay), vnp_TxnRef (VNPay), orderId (Momo)...
  metadata JSON NULL,                              -- field đặc thù riêng của từng cổng (số TK, bank gateway, v.v.) không cần cột cứng
  status ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
  paid_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (subscription_id) REFERENCES user_subscriptions(id) ON DELETE SET NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (plan_id) REFERENCES premium_plans(id),
  FOREIGN KEY (provider_id) REFERENCES payment_providers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Log kỹ thuật: lưu MỌI lần webhook gọi tới, kể cả trùng lặp/lỗi — phục vụ tra soát, không phải nguồn tính tiền
CREATE TABLE payment_webhook_logs (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  provider_id BIGINT UNSIGNED NOT NULL,
  transaction_id BIGINT UNSIGNED NULL,    -- khớp được với giao dịch nào (NULL nếu không match được provider_ref_code)
  event_id VARCHAR(150) NULL,              -- id sự kiện phía cổng gửi (nếu có) — dùng để chống xử lý trùng (idempotency)
  payload JSON NOT NULL,                   -- nguyên văn body webhook nhận được
  signature_valid BOOLEAN NULL,            -- có xác thực chữ ký/token webhook hợp lệ không
  status ENUM('RECEIVED','PROCESSED','IGNORED','FAILED') NOT NULL DEFAULT 'RECEIVED',
  received_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  processed_at DATETIME NULL,
  FOREIGN KEY (provider_id) REFERENCES payment_providers(id),
  FOREIGN KEY (transaction_id) REFERENCES payment_transactions(id) ON DELETE SET NULL,
  INDEX idx_webhook_transaction (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 9. Speaking Room (mới)

```sql
CREATE TABLE speaking_rooms (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  room_code VARCHAR(20) NOT NULL UNIQUE,
  host_id BIGINT UNSIGNED NOT NULL,
  topic VARCHAR(255),
  max_participants TINYINT UNSIGNED NOT NULL DEFAULT 4,
  status ENUM('WAITING','ACTIVE','CLOSED') NOT NULL DEFAULT 'WAITING',
  started_at DATETIME NULL,
  ended_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (host_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE speaking_participants (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  role ENUM('HOST','GUEST') NOT NULL DEFAULT 'GUEST',
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  left_at DATETIME NULL,
  FOREIGN KEY (room_id) REFERENCES speaking_rooms(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_room_active (room_id, left_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Lưu ý:** không đặt `UNIQUE(room_id, user_id)` vì 1 người có thể rời rồi vào lại cùng 1 phòng (mỗi lần vào là 1 dòng mới, `left_at IS NULL` xác định đang có mặt hay không) — phù hợp để tính thời lượng luyện nói tích lũy sau này.

---

## 10. Notification

```sql
CREATE TABLE notifications (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  type VARCHAR(100) NOT NULL,           -- ví dụ: 'REPORT_RESOLVED', 'BADGE_EARNED', 'SUBSCRIPTION_EXPIRING'
  title VARCHAR(255) NOT NULL,
  content VARCHAR(500),
  ref_type VARCHAR(50),
  ref_id BIGINT UNSIGNED,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_notification_user (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---