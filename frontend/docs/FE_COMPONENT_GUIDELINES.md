# FE_COMPONENT_GUIDELINES.md — Elingo Component Guidelines

> Tài liệu này định nghĩa **khi nào và cách nào** tạo component trong Elingo Frontend.
> Mục tiêu: nhất quán, tránh duplicate, dễ maintain.

> Trước khi tạo component, tìm trong `src/components/`, module hiện tại, và component/page gần nhất để ưu tiên tái sử dụng pattern đang có.

---

## 1. Shared vs Module-specific Component

### Câu hỏi quyết định

> *Component này có được dùng ở **2+ module khác nhau** không?*

- **Có** → Đặt trong `src/components/ComponentName/`
- **Không** → Đặt trong `src/modules/moduleName/components/ComponentName/`

### Ví dụ thực tế

| Component | Vị trí | Lý do |
|---|---|---|
| `Input` | `src/components/Input/` | Dùng ở auth, profile, admin, community... |
| `Modal` | `src/components/Modal/` | Dùng ở mọi nơi cần confirm |
| `Pagination` | `src/components/Pagination/` | Dùng ở mọi trang list |
| `Filter` | `src/components/Filter/` | Dùng ở lesson list, flashcard list, vocabulary... |
| `Header` | `src/components/Header/` | App-level, dùng ở tất cả public routes |
| `Footer` | `src/components/Footer/` | App-level |
| `LessonCard` | `src/modules/learning/lesson/components/` | Chỉ dùng trong lesson module |
| `FlashCard` | `src/modules/learning/flashcard/components/` | Chỉ dùng trong flashcard module |
| `BattleScoreBoard` | `src/modules/battle/components/` | Chỉ dùng trong battle module |

### Khi nghi ngờ

Bắt đầu đặt trong module. Nếu module khác cần dùng → chuyển lên `src/components/`.

---

## 2. Cấu Trúc Component

```
ComponentName/
├── ComponentName.jsx   # Logic + JSX
└── ComponentName.css   # Styles — KHÔNG dùng inline styles (trừ dynamic values)
```

### Sub-components

Nếu component lớn cần chia nhỏ:

```
LessonCard/
├── LessonCard.jsx
├── LessonCard.css
└── LessonCardBadge/
    ├── LessonCardBadge.jsx
    └── LessonCardBadge.css
```

---

## 3. Component Naming

| Loại | Convention | Ví dụ |
|---|---|---|
| Page | `PascalCase + Page` | `LoginPage`, `LessonListPage` |
| Layout | `PascalCase + Layout` | `AdminLayout` |
| Shared (no business) | `PascalCase` | `Input`, `Modal`, `Pagination` |
| Feature component | `PascalCase` mô tả rõ vai trò | `LessonCard`, `FlashCard`, `BattleTimer` |
| Context | `PascalCase + Context` | `BattleSocketContext` |

---

## 4. Component Template

```jsx
import './ComponentName.css'

/**
 * ComponentName — mô tả ngắn gọn bằng tiếng Việt.
 *
 * @param {string} propA - mô tả
 * @param {Function} onSomething - mô tả
 * @param {Function} onNavigate - navigate function từ App.jsx
 */
function ComponentName({ propA, onSomething, onNavigate }) {
  // States
  // ...

  // Handlers
  const handleSomething = () => {
    // ...
    if (onNavigate) onNavigate('/some-path')
  }

  return (
    <div className="component-name-wrapper">
      {/* ... */}
    </div>
  )
}

export default ComponentName
```

---

## 5. Props Convention

### 5.1 Props bắt buộc

| Prop | Kiểu | Dùng khi |
|---|---|---|
| `onNavigate` | `Function` | Component cần navigate đến trang khác |
| `id` | `string` | Tất cả `<Input>` — bắt buộc cho a11y |

### 5.2 Default values

```jsx
// Luôn cung cấp default cho optional props
function FilterBar({ cefrLevels = [], tags = [], selectedCefrLevelId = null }) { ... }
```

### 5.3 Event handlers

```jsx
// Luôn check trước khi gọi callback
if (onConfirm) onConfirm()
if (onNavigate) onNavigate('/path')
```

### 5.4 Không pass props thừa xuống DOM

```jsx
// BAD — aria-* và data-* spread xuống DOM element có thể gây warning
function Input({ id, label, error, ...props }) {
  return <input {...props} />  // props có thể chứa thứ không phải DOM attribute
}

// GOOD — chỉ spread những gì DOM cần
function Input({ id, label, type, value, onChange, error, disabled, autoFocus, ...rest }) {
  return <input id={id} type={type} value={value} onChange={onChange} {...rest} />
}
```

---

## 6. Sử Dụng Design System

### 6.1 Glass classes

```jsx
// Dùng utility class từ index.css — KHÔNG tự viết glass CSS
<div className="glass-card" style={{ borderRadius: 'var(--radius-card)' }}>
```

### 6.2 Button

```jsx
// KHÔNG tạo button custom — dùng utility classes
<button className="btn-primary">Xác nhận</button>
<button className="btn-secondary">Hủy</button>
<button className="btn-icon">
  <span className="material-symbols-outlined">close</span>
</button>
```

### 6.3 Typography

```jsx
// Dùng utility class HOẶC CSS variable — KHÔNG hardcode
<h1 className="text-headline-lg">Tiêu đề</h1>
<p className="text-body-md">Nội dung</p>
// Hoặc trong CSS:
.my-title { font-size: var(--font-size-headline-md); font-family: var(--font-display); }
```

### 6.4 Colors

```jsx
// CSS — KHÔNG hardcode hex
.my-component { color: var(--color-primary); background: var(--color-secondary-container); }
// Inline — chỉ khi giá trị dynamic
<div style={{ color: isActive ? 'var(--color-primary)' : 'var(--color-on-surface-variant)' }}>
```

### 6.5 Icons

```jsx
// Material Symbols Outlined — KHÔNG dùng icon library khác
<span className="material-symbols-outlined">check_circle</span>

// Icon trong card — wrap bằng container glass-thumb
<div className="card-icon-container glass-thumb btn-icon" style={{ width: 56, height: 56 }}>
  <span className="material-symbols-outlined" style={{ fontSize: 28 }}>school</span>
</div>
```

---

## 7. Shared Components Hiện Tại

### 7.1 `<Input>` — `src/components/Input/`

```jsx
<Input
  id="email"               // BẮT BUỘC
  label="Email"
  type="email"             // hoặc 'password', 'text', 'number'...
  placeholder="name@example.com"
  value={email}
  onChange={(e) => setEmail(e.target.value)}
  error={errors.email}     // string hoặc null/undefined
  autoFocus={true}
  disabled={isSubmitting}
/>
```

### 7.2 `<Modal>` — `src/components/Modal/`

```jsx
<Modal
  isOpen={isDeleteModalOpen}
  title="Xác nhận xóa"
  message="Bạn có chắc muốn xóa bài học này không?"
  confirmText="Xóa"
  cancelText="Hủy"
  onConfirm={handleDelete}
  onCancel={() => setIsDeleteModalOpen(false)}
  isDanger={true}           // nút confirm màu đỏ
  isLoading={isDeleting}
/>
```

### 7.3 `<Pagination>` — `src/components/Pagination/`

```jsx
<Pagination
  currentPage={page}          // 1-indexed
  totalPages={totalPages}     // tự ẩn nếu <= 1
  onPageChange={(p) => setPage(p)}
/>
```

### 7.4 `<Filter>` — `src/components/Filter/`

```jsx
<Filter
  cefrLevels={cefrLevels}           // [{_id, name}]
  tags={tags}                        // [{_id, name}]
  selectedCefrLevelId={selectedCefr}
  selectedTagId={selectedTag}
  onCefrChange={(id) => setSelectedCefr(id)}
  onTagChange={(id) => setSelectedTag(id)}
/>
```

### 7.5 `<Header>` — `src/components/Header/`

```jsx
<Header onNavigate={navigate} currentPath={currentPath} />
```

### 7.6 `<Footer>` — `src/components/Footer/`

```jsx
<Footer onNavigate={navigate} />
```

### 7.7 Header, footer và active state

- `Header` và `Footer` được render từ `App.jsx` cho public/user routes; page không tự render thêm bản sao.
- `Header` nhận `currentPath` để thể hiện route hiện tại. Với cặp action đăng nhập/đăng ký, action của route đang mở dùng `.btn-primary`, action còn lại dùng `.btn-secondary`.
- Landing navigation dùng anchor section và active state theo vị trí scroll; luôn giữ underline mounted rồi animate opacity/transform, không add/remove underline làm layout giật.

---

## 8. Khi Nào KHÔNG Tạo Component Mới

- Đoạn JSX chỉ dùng 1 lần và không có logic phức tạp → để thẳng trong page
- Chỉ là wrapper div với 1–2 style → dùng CSS class thay
- Component chỉ render data, không có state hay handler → cân nhắc có cần thiết không

---

## 9. Loading & Error Patterns

### 9.1 Page-level loading

```jsx
if (loading) return <div className="page-loading"><div className="app-loading-spinner" /></div>
```

### 9.2 Error state

```jsx
if (error) return (
  <div className="page-loading">
    <p className="error-message">{error}</p>
  </div>
)
```

### 9.3 Empty state

```jsx
{data.length === 0 && !loading && (
  <div className="empty-state">
    <span className="material-symbols-outlined">inbox</span>
    <p>Không có dữ liệu</p>
  </div>
)}
```

### 9.4 Inline alerts

Với lỗi/thành công gắn với form, dùng alert inline phía trên form thay vì chỉ dùng `alert()` hoặc console:

```jsx
{error && <div className="auth-alert auth-alert--error">{error}</div>}
{successMsg && <div className="auth-alert auth-alert--success">{successMsg}</div>}
```

Alert cần có text cụ thể, không che nội dung, và có style dark mode tương ứng. Các module khác cần alert tương tự nên tạo component/shared pattern thay vì copy CSS auth.

---

## 10. Lỗi Team Cần Tránh

| Lỗi | Đúng |
|---|---|
| Tự viết glass CSS thay vì dùng utility class | Dùng `.glass-card`, `.glass-thumb`, v.v. |
| Hardcode màu hex trong CSS | Dùng `var(--color-primary)` |
| Tạo nút button custom | Dùng `.btn-primary`, `.btn-secondary`, `.btn-icon` |
| Navigate trực tiếp bằng `window.location.href` | Dùng `onNavigate(path)` prop |
| Dùng `<Link>` từ React Router | Dùng `<a href onClick>` pattern |
| Quên `return null` khi component không nên render | Luôn handle edge case |
| Quên cleanup trong useEffect | `return () => cleanup()` |
| Tạo thêm Axios instance | Chỉ dùng `apiClient.js` |
| Đặt CSS class không prefix | Prefix theo tên component |
| Dùng icon library (lucide, heroicons) | Dùng Material Symbols Outlined |
| Tạo shared component cho 1 feature duy nhất | Đặt trong module đó |
| Tạo module-specific component ở `src/components/` | Chỉ shared components mới vào đó |
| Tạo header/footer riêng trong page | Dùng shared component từ `App.jsx` |
| Chỉ kiểm tra giao diện light hoặc desktop | Kiểm tra light/dark và desktop/mobile trước khi hoàn tất |
