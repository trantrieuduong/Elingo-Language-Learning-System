# FE_DESIGN_SYSTEM.md — Elingo Visual Design System

> Tài liệu này định nghĩa **toàn bộ visual system** cho frontend Elingo.
> Triết lý: **Liquid Glass Minimalism** — frosted glass surfaces, ambient pastel orbs, organic softness.
> Tất cả tokens đều được implement trong `src/index.css`. Không tự định nghĩa lại trong từng component.

---

## 1. Triết Lý Thiết Kế

**"Liquid Glass Minimalism"** — 3 trụ cột:

1. **Frosted Glass Layering**: Bề mặt là kính mờ bán trong suốt, tạo chiều sâu thay vì màu đặc
2. **Ambient Pastel Orbs**: Nền là gradient nhẹ + các vùng ánh sáng màu pastel mờ (blur 120–130px) tạo refraction cho glass
3. **Organic Softness**: Bo tròn nhiều — pill (9999px) cho interactive, apple-radius (26px) cho cards

---

## 2. Color Palette

### 2.1 Brand & Accent Colors

| CSS Variable | Hex | Dùng cho |
|---|---|---|
| `--color-primary` | `#435f8b` | CTA, active states, links, icons |
| `--color-primary-container` | `#9bb7e8` | Backgrounds, selected states, focus ring |
| `--color-on-primary` | `#ffffff` | Text trên primary button |
| `--color-on-primary-container` | `#2b4872` | Text trên primary-container |
| `--color-secondary` | `#53606e` | Secondary icons, text |
| `--color-secondary-container` | `#d7e4f5` | Soft Blue — secondary highlights, active tab |
| `--color-error` | `#ba1a1a` | Error states |
| `--color-outline` | `#74777f` | Borders, dividers |
| `--color-outline-variant` | `#c4c6d0` | Subtle borders |

### 2.2 Surface Colors

| CSS Variable | Hex | Dùng cho |
|---|---|---|
| `--color-surface` | `#f7f9fc` | Primary background surface |
| `--color-surface-container-lowest` | `#ffffff` | Lightest container |
| `--color-surface-container-low` | `#f2f4f7` | Slightly elevated |
| `--color-surface-container` | `#eceef1` | Standard container |
| `--color-surface-container-high` | `#e6e8eb` | Elevated container |
| `--color-on-surface` | `#1e293b` | Primary text (headings) |
| `--color-on-surface-variant` | `#475569` | Secondary text, placeholders |

### 2.3 Pastel Accents (cho chips, badges, ambient orbs)

| CSS Variable | Hex | Text Pair | Dùng cho |
|---|---|---|---|
| `--color-pastel-blue` | `#9bb7e8` | `#2b4872` | Primary brand, interactive |
| `--color-pastel-soft-blue` | `#dce9fa` | `#003670` | Secondary highlights |
| `--color-pastel-mint` | `#ddf2ec` | `#1f5e4b` | Correct answers, success |
| `--color-pastel-lavender` | `#e8e3f5` | `#4c3775` | Grammar, notes |
| `--color-pastel-pink` | `#f5e5ed` | `#6e2e4b` | Alerts, review |
| `--color-pastel-ambient` | `#abc8f9` | — | Ambient orb only |

### 2.4 Text Hierarchy

| CSS Variable | Hex | Dùng cho |
|---|---|---|
| `--color-text-high` | `#1e293b` | Headings, card titles |
| `--color-text-mid` | `#334155` | Subheadings, UI labels |
| `--color-text-body` | `#4b5563` | Body text, descriptions |
| `--color-text-disabled` | `#94a3b8` | Placeholder, muted |

> **Quy tắc cứng**: KHÔNG dùng `#000000` hay `#ffffff` thuần. KHÔNG dùng màu neon/bão hòa cao.

### 2.5 Background Canvas

```css
background: linear-gradient(135deg, #f0f4fc 0%, #e8edf8 50%, #edeaf7 100%);
```

---

## 3. Typography

### 3.1 Font Families

| CSS Variable | Font | Dùng cho |
|---|---|---|
| `--font-display` | Plus Jakarta Sans | Headings, labels, buttons, navigation, badges |
| `--font-body` | Inter | Body text, descriptions, multi-line reading |

> Cả hai font được load từ Google Fonts trong `index.css`. KHÔNG dùng system-ui hay Roboto.

### 3.2 Type Scale

| CSS Variable | Size | Line Height | Weight | Tracking | Font |
|---|---|---|---|---|---|
| `--font-size-display-lg` | 44px (32px mobile) | 52px | 800 | -0.02em | Plus Jakarta Sans |
| `--font-size-headline-lg` | 32px (26px mobile) | 40px | 700 | -0.015em | Plus Jakarta Sans |
| `--font-size-headline-md` | 24px | 32px | 600 | -0.01em | Plus Jakarta Sans |
| `--font-size-headline-sm` | 20px | 28px | 600 | — | Plus Jakarta Sans |
| `--font-size-title-md` | 18px | 26px | 600 | — | Plus Jakarta Sans |
| `--font-size-label-lg` | 14px | 20px | 600 | +0.01em | Plus Jakarta Sans |
| `--font-size-label-md` | 12px | 16px | 600 | +0.02em | Plus Jakarta Sans |
| `--font-size-label-sm` | 11px | 14px | 600 | +0.03em | Plus Jakarta Sans |
| `--font-size-body-lg` | 16px | 26px | 400 | — | Inter |
| `--font-size-body-md` | 14px | 22px | 400 | — | Inter |
| `--font-size-body-sm` | 12px | 18px | 400 | — | Inter |

### 3.3 Typography Rules

- Headings: `font-display`, 700–800 weight, `color: var(--color-text-high)`
- Body: `font-body`, 400 weight, `color: var(--color-text-body)`, `line-height: 1.625`
- Labels/Chips: `font-display`, 600 weight, positive letter-spacing
- KHÔNG hardcode font-family — dùng `var(--font-display)` hoặc `var(--font-body)`

---

## 4. Spacing System

| CSS Variable | Giá trị | Dùng cho |
|---|---|---|
| `--spacing-xs` | 4px | Micro spacing |
| `--spacing-sm` | 8px | Gap giữa inline elements |
| `--spacing-md` | 16px | Margin mobile, gap thông thường |
| `--spacing-lg` | 24px | Card padding tối thiểu, section gap |
| `--spacing-xl` | 32px | Large gap |
| `--spacing-2xl` | 40px | Section padding |
| `--spacing-3xl` | 48px | Large section padding |

> **Quy tắc**: Card padding tối thiểu `var(--spacing-lg)` (24px). Tất cả spacing là bội số 4px.

---

## 5. Border Radius System

| CSS Variable | Giá trị | Dùng cho |
|---|---|---|
| `--radius-sm` | 8px | Inner containers, small elements |
| `--radius-md` / `--radius-input` | 16px | Form inputs |
| `--radius-apple` / `--radius-card` | 26px | Standard cards, sub-cards |
| `--radius-apple-lg` / `--radius-card-lg` | 36px | Large panels, hero wrapper |
| `--radius-pill` / `--radius-badge` | 9999px | Buttons, pills, badges, chips, avatar |

> **Shape language**: Interactive = pill. Cards = apple (26px). Panels = apple-lg (36px). KHÔNG dùng góc vuông.

---

## 6. Glass / Backdrop System

### 6.1 Glass Classes (từ thấp → cao elevation)

| Class | Background | Blur | Dùng cho |
|---|---|---|---|
| `.glass-well` | rgba(255,255,255,0.35) | 16px | Inner containers, inset areas |
| `.glass-thumb` | rgba(255,255,255,0.72) | 16px | Avatar, icon buttons, chips, badges |
| `.glass-pill` | rgba(255,255,255,0.52) | 24px | Secondary buttons, tag pills |
| `.glass-card` | rgba(255,255,255,0.48) | 28px | Feature cards, content cards |
| `.glass-capsule` | rgba(255,255,255,0.42) | 32px | Navbar, large panels, hero wrapper |
| `.glass-pro` | gradient (white → #e1eeff) | 34px | Premium/featured cards |

### 6.2 Glass Layering Rules

- **KHÔNG** đặt `.glass-capsule` bên trong `.glass-capsule` — dùng `.glass-well` cho inner content
- Hover trên card: `transform: translateY(-3px)` + spring easing `cubic-bezier(0.16, 1, 0.3, 1)`
- `.glass-sheen` thêm `::after` pseudo-element tạo ánh sáng chạy ngang — dùng cho capsule + pro

### 6.3 Ambient Orbs (QUAN TRỌNG)

Đặt `<div class="ambient-bg">` với 5 orbs cố định ở App level:
```jsx
<div className="ambient-bg" aria-hidden="true">
  <div className="ambient-bg__orb ambient-bg__orb--blue" />
  <div className="ambient-bg__orb ambient-bg__orb--lavender" />
  <div className="ambient-bg__orb ambient-bg__orb--mint" />
  <div className="ambient-bg__orb ambient-bg__orb--pink" />
  <div className="ambient-bg__orb ambient-bg__orb--ambient" />
</div>
```
> Blur 120–130px, opacity 55–80%. **Bắt buộc phải có orbs** — glass effect mất chiều sâu nếu thiếu.

---

## 7. Button System

### 7.1 Primary Button (`.btn-primary`)

```
Shape: rounded-full (pill)
Padding: 0.875rem 2rem
Background: gradient #4d6d9e → #3a5680
Text: white, Plus Jakarta Sans, 14px, 600
Hover: translateY(-1px), shadow tăng
```

### 7.2 Secondary Button (`.btn-secondary`)

```
Shape: rounded-full
Padding: 0.875rem 1.75rem
Background: glass-pill (rgba white 52%, blur 24px)
Text: on-surface, Plus Jakarta Sans, 14px, 600
```

### 7.3 Icon Button (`.btn-icon`)

```
Shape: rounded-full
Size: 36×36px
Background: glass-thumb (rgba white 72%)
Icon: Material Symbols Outlined, 18–22px
```

> **Quy tắc**: Primary action = `.btn-primary`. Secondary/ghost = `.btn-secondary`. Icon only = `.btn-icon`. KHÔNG tự định nghĩa nút mới.

---

## 8. Cards

### 8.1 Feature Card

```css
.glass-card + border-radius: var(--radius-card-lg) /* 36px */
padding: var(--spacing-3xl) /* 48px */
icon container: 56×56px, rounded-full, glass-thumb
icon: Material Symbols, 28px
hover: translateY(-3px), spring transition
```

### 8.2 Standard Card

```css
.glass-card + border-radius: var(--radius-card) /* 26px */
padding: var(--spacing-lg) /* 24px */
```

### 8.3 Sub-Card / Content Well

```css
.glass-well + border-radius: var(--radius-card) /* 26px */
padding: var(--spacing-xl) /* 32px */
Dùng bên trong card lớn (inset look)
```

### 8.4 Premium Card

```css
.glass-pro + .glass-sheen
border-radius: var(--radius-card-lg)
Gradient nhẹ hướng xanh
```

---

## 9. Chips & Badges

```css
Shape: rounded-full (pill)
Padding: px-3.5 py-1 (standard)
Font: Plus Jakarta Sans, label-sm (11px), 600, +0.03em tracking
Border: 1px solid rgba(255,255,255,0.6)
```

| Variant | Class | Background | Text |
|---|---|---|---|
| Default | `.badge--default` | glass-thumb | on-surface |
| Primary | `.badge--primary` | secondary-container | primary |
| Mint (Success) | `.badge--mint` | `#ddf2ec` | `#1f5e4b` |
| Lavender (Info) | `.badge--lavender` | `#e8e3f5` | `#4c3775` |
| Pink (Alert) | `.badge--pink` | `#f5e5ed` | `#6e2e4b` |
| Soft Blue | `.badge--soft-blue` | `#dce9fa` | `#003670` |
| Error | `.badge--error` | error-container | error |

> **Quy tắc**: 1 accent màu mỗi card — KHÔNG dùng nhiều pastel cùng lúc.

---

## 10. Inputs

```css
Background: rgba(255,255,255,0.6) + blur(12px)
Border: 1px solid rgba(255,255,255,0.8)
Border-radius: 16px (--radius-input)
Color: --color-text-high
Placeholder: --color-text-disabled

Focus:
  background: rgba(255,255,255,0.95)
  border-color: --color-primary-container (#9bb7e8)
  box-shadow: 0 0 0 4px rgba(155,183,232,0.2)
```

---

## 11. Icons

**Material Symbols Outlined** — Google Icons.

```html
<!-- Load từ index.css (đã có) -->
<span class="material-symbols-outlined">icon_name</span>
```

| Context | Size |
|---|---|
| Card icon (trong container) | 28px |
| Action button icon | 18–20px |
| Chip/badge icon | 16px |
| Small inline icon | 14px |

> KHÔNG dùng heroicons, lucide, react-icons hay bất kỳ icon library nào khác.

---

## 12. Responsive

Breakpoint chính là **`max-width: 1024px`** để chuyển layout desktop sang một cột. Được dùng thêm breakpoint hẹp khi nội dung thực sự cần: **`max-width: 600px`** cho điện thoại và **`max-width: 360px`** cho màn hình rất hẹp. Không tạo breakpoint tùy tiện ngoài ba mốc này nếu chưa có lý do cụ thể.

| Breakpoint | Columns | Margin | Max Width |
|---|---|---|---|
| Desktop > 1024px | 12 cols | 48px | 1200px |
| Mobile ≤ 1024px | 1 col | 16px | — |

> `.container` class: `max-width: 1200px`, `margin: 0 auto`, `padding-inline: 48px` (desktop) / `16px` (mobile).

Quy tắc kiểm tra:
- Không ẩn primary workflow trên mobile chỉ để vừa layout; thu gọn padding, đổi grid sang 1 cột hoặc dùng menu phù hợp.
- Text, button, input, iframe/widget bên thứ ba phải vừa viewport, không gây horizontal scroll hoặc bị cắt.
- Header fixed phải chừa khoảng đệm đầu trang cho nội dung. Kiểm tra ít nhất desktop, `600px` và `360px` trước khi hoàn tất.

---

## 13. Micro-Interactions & Transitions

| Element | Transition |
|---|---|
| Cards (hover) | `0.3s cubic-bezier(0.16, 1, 0.3, 1)` (spring) |
| Buttons, pills | `0.25s ease` |
| Color/link changes | `0.15s ease` |
| Scale | `0.15s ease` |

Card hover: `transform: translateY(-3px)` + shadow tăng + background opacity tăng.

---

## 14. Dark Mode

- Toggle qua CSS selector `[data-theme='dark']` trên `<html>`
- **KHÔNG** dùng `@media (prefers-color-scheme: dark)` trong CSS
- Quản lý qua `ThemeContext` — persist `localStorage.getItem('theme')`

- Dark tokens đã có trong `src/index.css`. Component mới phải dùng token thay vì hardcode nền/chữ để tự nhận palette này.
- Nếu phải dùng màu ngữ nghĩa riêng cho component (ví dụ error/success alert), bắt buộc thêm override `[data-theme='dark']` có contrast rõ cho background, border và text.
- Không dùng `opacity` thấp của pastel light trên nền tối cho thông báo hoặc input; kiểm tra cả error, success, disabled, hover và focus state.

---

## 15. Checklist Cho Màn Hình Mới

- [ ] Ambient orbs có ở layout level chưa? (đã có ở App.jsx)
- [ ] Cards dùng đúng `.glass-*` class chưa?
- [ ] Shape: Interactive = pill, Cards = apple/apple-lg?
- [ ] Typography: Headings = Plus Jakarta Sans bold, Body = Inter regular?
- [ ] Colors: Dùng CSS variable (`var(--color-primary)`) hay hardcode?
- [ ] Spacing: Card padding ≥ 24px?
- [ ] Hover: Cards có `translateY(-3px)` + spring transition chưa?
- [ ] Icons: Material Symbols Outlined? Trong container `rounded-full glass-thumb`?
- [ ] Chips/badges: Pill-shaped + đúng pastel color pair?
- [ ] Primary action: `.btn-primary` + `rounded-full`?
- [ ] Dark theme: surface, text, input, alert, hover/focus có đủ contrast?
- [ ] Responsive: kiểm tra tại desktop, 600px, 360px; không overflow/cắt nội dung?
- [ ] Header/footer/layout dùng shared component hiện có, không tạo bản sao trong page?

---

## 16. Quy Tắc Tuyệt Đối

###  BẮT BUỘC

1. Ambient Orbs luôn phải có — không có orbs = mất glass effect
2. Glass class phải kèm `border-radius` phù hợp
3. **KHÔNG** dùng solid background màu đặc cho container/card
4. Primary button luôn pill-shaped + `.btn-primary`
5. Icon container: `rounded-full glass-thumb` với kích thước cố định
6. Màu text dùng token, KHÔNG hardcode hex
7. Chip/badge: luôn pill + đúng pastel pair

###  CẦN TRÁNH

1. Solid background (`background: #ffffff`) cho cards — mất glass
2. Heavy shadow opacity > 0.4 — phá cảm giác nhẹ nhàng
3. Màu bão hòa cao (neon)
4. Border-radius = 0 — mọi thứ phải bo tròn tối thiểu `--radius-sm` (8px)
5. Nhiều pastel accent trên cùng 1 card
6. `blur()` < 12px — không đủ frosted glass
