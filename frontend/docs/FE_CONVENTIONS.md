# FE_CONVENTIONS.md — Elingo Frontend Conventions

> Tài liệu này là **single source of truth** về architecture và code conventions cho frontend của Elingo.
>
> **Vị trí tài liệu:** `frontend/docs/`. Khi bắt đầu task UI, phải đọc đủ bốn file trong thư mục này: `FE_CONVENTIONS.md`, `FE_DESIGN_SYSTEM.md`, `FE_ROUTES.md`, `FE_COMPONENT_GUIDELINES.md`, sau đó đọc component/page liên quan trước khi tạo file mới.

---

## 1. Stack & Tooling

| Hạng mục | Công nghệ |
|---|---|
| Framework | React 19 + Vite 8 |
| Ngôn ngữ | **JavaScript** (`.js` / `.jsx`) — **KHÔNG dùng TypeScript** |
| Styling | **Vanilla CSS thuần** — KHÔNG dùng Tailwind, MUI, Bootstrap |
| Routing | **Custom router tự viết** trong `App.jsx` (KHÔNG dùng React Router) |
| State | React local state (`useState`) + Context API |
| HTTP | **Axios** qua wrapper `apiClient.js` (file duy nhất, KHÔNG tạo thêm instance) |
| Linting | `oxlint` (không phải ESLint) |
| Formatter | `prettier` |
| Test | `vitest` + `@testing-library/react` _(chưa cài, thêm khi cần)_ |
| Icons | **Material Symbols Outlined** (Google) — KHÔNG dùng heroicons, lucide |

---

## 2. Folder & File Structure

```
frontend/src/
├── App.jsx               # Root component — custom router + toàn bộ routes
├── App.css               # CSS cho App shell (main wrapper, placeholder)
├── main.jsx              # Entry point — mount React, wrap Providers
├── index.css             # Design tokens (CSS variables) + CSS reset + utility classes
│
├── assets/               # Hình ảnh, icons tĩnh
│
├── components/           # Shared UI components — không liên quan nghiệp vụ cụ thể
│   ├── Header/
│   │   ├── Header.jsx
│   │   └── Header.css
│   ├── Footer/
│   │   ├── Footer.jsx
│   │   └── Footer.css
│   ├── Input/
│   │   ├── Input.jsx
│   │   └── Input.css
│   ├── Modal/
│   │   ├── Modal.jsx
│   │   └── Modal.css
│   ├── Pagination/
│   │   ├── Pagination.jsx
│   │   └── Pagination.css
│   └── Filter/
│       ├── Filter.jsx
│       └── Filter.css
│
├── context/              # Global React Contexts
│   ├── AuthContext.jsx
│   └── ThemeContext.jsx
│
├── modules/              # Feature modules — MỖI folder = 1 module nghiệp vụ
│   ├── admin/
│   │   ├── adminApi.js
│   │   ├── layout/       # AdminLayout riêng, không dùng Header/Footer public
│   │   ├── components/
│   │   └── pages/
│   ├── auth/
│   │   ├── authApi.js
│   │   ├── components/
│   │   └── pages/
│   ├── battle/
│   │   ├── battleApi.js
│   │   ├── context/      # BattleSocketContext — feature-level context
│   │   ├── components/
│   │   └── pages/
│   ├── community/
│   │   ├── postApi.js
│   │   ├── components/
│   │   └── pages/
│   ├── gamification/
│   │   ├── gamificationApi.js
│   │   ├── components/
│   │   └── pages/
│   ├── learning/         # Nested sub-modules
│   │   ├── dictation/    # → dictationApi.js, components/, pages/
│   │   ├── flashcard/    # → flashcardApi.js, components/, pages/
│   │   ├── lesson/       # → lessonApi.js, components/, pages/
│   │   └── shadowing/    # → shadowingApi.js, components/, pages/
│   ├── premium/
│   │   ├── premiumApi.js
│   │   ├── components/
│   │   └── pages/
│   ├── progress/
│   │   ├── progressApi.js
│   │   ├── components/
│   │   └── pages/
│   ├── report/
│   │   ├── reportApi.js
│   │   ├── components/
│   │   └── pages/
│   ├── speaking/
│   │   ├── speakingRoomApi.js
│   │   ├── context/      # SpeakingRoomContext (WebRTC) — nếu cần
│   │   ├── components/
│   │   └── pages/
│   ├── user/
│   │   ├── profileApi.js
│   │   ├── components/
│   │   └── pages/
│   └── vocabulary/
│       ├── vocabularyApi.js    # Cần tạo (chưa có trong skeleton)
│       ├── components/
│       └── pages/
│
├── services/
│   └── apiClient.js      # Axios instance duy nhất — KHÔNG tạo thêm
│
├── utils/
│   └── utils.js          # Pure utility functions
│
└── tests/                # Unit / Integration tests
    └── setupTests.js
```

> **Lưu ý quan trọng**: Project này dùng `modules/` thay vì `features/`. Quy tắc bên trong module giống nhau.

---

## 3. Naming Conventions

### 3.1 File & Folder

| Loại | Convention | Ví dụ |
|---|---|---|
| Component file | `PascalCase.jsx` | `LoginPage.jsx`, `LessonCard.jsx` |
| Component folder | `PascalCase/` | `Header/`, `ConfirmModal/` |
| CSS của component | `ComponentName.css` (cùng tên file jsx) | `LoginPage.css`, `Header.css` |
| API file của module | `camelCase + Api` suffix | `authApi.js`, `flashcardApi.js`, `adminApi.js` |
| Context file | `PascalCase + Context` | `AuthContext.jsx`, `ThemeContext.jsx` |
| Utility file | `camelCase` | `utils.js` |
| CSS class | `kebab-case` | `login-wrapper`, `lesson-card`, `filter-pill` |
| Env variable | `VITE_` prefix + `UPPER_SNAKE_CASE` | `VITE_API_URL`, `VITE_OTP_RESEND_COOLDOWN` |

### 3.2 Component Naming

| Loại | Convention | Ví dụ |
|---|---|---|
| Page component | `PascalCase + Page` suffix | `LoginPage`, `LessonListPage` |
| Layout component | `PascalCase + Layout` suffix | `AdminLayout` |
| Shared component | `PascalCase` không suffix | `Input`, `Modal`, `Pagination` |
| Feature component | `PascalCase` mô tả vai trò | `LessonCard`, `FlashCard`, `BattleScoreBoard` |

### 3.3 Function & Variable

| Loại | Convention | Ví dụ |
|---|---|---|
| Handler function | `handle + PascalCase` | `handleSubmit`, `handleLogout` |
| Toggle / bool setter | `toggle + PascalCase` | `toggleTheme`, `toggleDropdown` |
| Fetch function | `fetch + PascalCase` | `fetchLessons`, `fetchMetadata` |
| Load function | `load + PascalCase` | `loadProfileData` |
| API function (export) | `camelCase + Api` suffix hoặc `verb + Entity + Api` | `loginApi`, `getLessonsApi`, `createAdminDeckApi` |
| Boolean state | `is/has + PascalCase` | `isSubmitting`, `isOpen`, `hasError` |
| State biến dữ liệu | `camelCase` | `lessons`, `searchQuery`, `currentPage` |
| Setter state | `set + PascalCase` | `setLoading`, `setError` |

### 3.4 CSS Class Prefix Convention

- **Shared components**: Prefix bằng tên component dạng kebab-case → `input-*`, `modal-*`, `pagination-*`
- **Feature pages**: Prefix bằng tên page → `login-*`, `lesson-list-*`, `battle-*`
- **Admin components**: Prefix `admin-*`
- **Global utilities**: Từ `index.css` — `.glass-*`, `.btn-*`, `.text-*`, `.badge-*`, `.container`

### 3.5 Assets và biến môi trường

- Asset tĩnh dùng chung (logo, ảnh, SVG) đặt trong `src/assets/` và import từ component; không nhúng data URL hoặc SVG dài vào JSX.
- Logo/brand asset phải dùng nhất quán ở mọi bề mặt có brand.
- Biến môi trường Vite phải có tiền tố `VITE_` và đặt ở `frontend/.env` hoặc `frontend/.env.local`, **không** đặt trong `src/`. Khởi động lại Vite sau khi thay đổi env.
- Giá trị trong `VITE_*` được đưa vào client bundle; không đặt secret, private key, hoặc credential nhạy cảm ở đây.

---

## 4. Routing & Navigation

### 4.1 Custom Router — KHÔNG dùng React Router

```jsx
// App.jsx — navigate function được truyền xuống qua prop
const navigate = (path) => {
  window.history.pushState({}, '', path)
  setCurrentPath(path)
}

// Tất cả pages và components nhận onNavigate prop
function LoginPage({ onNavigate }) { ... }
```

### 4.2 Route Pattern

- **Simple routes**: `switch(currentPath)` với `case '/path'`
- **Dynamic routes**: `currentPath.match(/regex/)` → extract params
- **Pattern ID trong URL**: UUID hoặc MongoDB-style → `/[a-zA-Z0-9-]+/`

```jsx
// Ví dụ dynamic route
const flashcardMatch = currentPath.match(/^\/flashcard\/([a-zA-Z0-9-]+)$/)
if (flashcardMatch) {
  const deckId = flashcardMatch[1]
  return <FlashcardPage onNavigate={navigate} deckId={deckId} />
}
```

### 4.3 Navigation trong component

```jsx
// Luôn check onNavigate trước khi gọi
if (onNavigate) onNavigate('/learning')

// Với links trong JSX: href + onClick ngăn default
<a href="/lessons" onClick={(e) => { e.preventDefault(); navigate('/lessons') }}>
```

### 4.4 Layout hiện tại

- **Public + User routes**: `<Header>` + `<main>content</main>` + `<Footer>`
- **Admin routes** (`/admin/*`): hiện render placeholder trực tiếp trong `App.jsx`, không có public Header/Footer. Khi module admin được implement, dùng `AdminLayout` riêng.

### 4.5 Route Guard (trong App.jsx)

```
Chưa đăng nhập + private route → redirect /login
Admin + non-admin path → redirect /admin
Non-admin + /admin/* → redirect /dashboard
```

### 4.6 Auth routes hiện tại

Các route auth/public đã được implement trực tiếp trong `App.jsx`:

- `/` → `LandingPage`
- `/login` → `LoginPage`
- `/signup` → `SignupPage`
- `/account-verification` → `AccountVerificationPage`

Khi cần truyền email sang trang verification, dùng custom router state theo pattern:

```jsx
onNavigate('/account-verification', { email })
```

---

## 5. API & Service Layer

### 5.1 apiClient (Singleton)

`src/services/apiClient.js` là **file DUY NHẤT** tạo Axios instance:
- Base URL từ `import.meta.env.VITE_API_URL`
- **Request Interceptor**: Gắn `Bearer token` từ `localStorage`
- **Response Interceptor**: Tự refresh khi 401, xử lý failedQueue
- Khi refresh thất bại: xóa localStorage → dispatch `auth:logout` → redirect `/login`

### 5.2 Module API Files

Mỗi module có **1 file API riêng** (`moduleNameApi.js`):

```js
// Pattern chuẩn
import apiClient from '../../services/apiClient'   // hoặc '../../../services/apiClient'

// Named exports — KHÔNG dùng default export cho API functions
export const loginApi = async (username, password) => {
  const response = await apiClient.post('/auth/login', { username, password }, { withCredentials: true })
  return response.data  // ← Luôn return response.data (không return response)
}
```

### 5.3 API Response Pattern

```js
// Backend trả về { success, data, message, code }
const res = await someApi(params)
if (res.success && res.data) {
  // xử lý data
} else {
  setError(res.message ?? 'Đã có lỗi xảy ra')
}
```

### 5.4 JSDoc cho API có params

```js
/**
 * Lấy danh sách bài học với filter và phân trang.
 * @param {Object} params - { cefrLevelId, tagId, q, page, limit }
 */
export const getLessonsApi = async (params = {}) => { ... }
```

### 5.5 Admin API Naming Pattern

```
listAdmin{Entity}Api     → getAdminLessonsApi
createAdmin{Entity}Api   → createAdminLessonApi
getAdmin{Entity}ByIdApi  → getAdminLessonByIdApi
updateAdmin{Entity}Api   → updateAdminLessonApi
deleteAdmin{Entity}Api   → deleteAdminLessonApi
```

---

## 6. Context & State Management

### 6.1 Context Pattern (bắt buộc)

```jsx
const SomeContext = createContext(null)

export const SomeProvider = ({ children }) => { ... }

export const useSome = () => {
  const context = useContext(SomeContext)
  if (!context) {
    throw new Error('useSome phải được sử dụng trong SomeProvider')
  }
  return context
}
```

### 6.2 Global Contexts (wrap ở main.jsx)

- **`ThemeProvider`** → `useTheme()`: theme state + toggle + persist localStorage
  - Dark mode: CSS selector `[data-theme='dark']` trên `<html>` — KHÔNG dùng `prefers-color-scheme`
- **`AuthProvider`** → `useAuth()`: user, accessToken, loading, login, signup, verifyAccount, resendVerificationOtp, logout, updateUser
  - Auth pages gọi qua `useAuth()` thay vì gọi `authApi` trực tiếp.
  - `AuthContext` chịu trách nhiệm lưu `accessToken`, load `/users/me`, persist `user`, và trả `{ success, message }` cho page.

### 6.3 Feature-level Context

Đặt trong `modules/moduleName/context/` (ví dụ: `BattleSocketContext.jsx`, `SpeakingRoomContext.jsx`)

### 6.4 Local State Conventions

```js
const [loading, setLoading] = useState(true)         // init loading = true
const [error, setError] = useState(null)              // null hoặc '' tùy context
const [isSubmitting, setIsSubmitting] = useState(false)
const [searchQuery, setSearchQuery] = useState('')
const [debouncedSearchQuery, setDebouncedSearchQuery] = useState('')
const [page, setPage] = useState(1)
```

---

## 7. Component Patterns

### 7.1 Props chuẩn

```jsx
// Navigation: luôn nhận onNavigate prop — KHÔNG navigate trực tiếp
// CSS: import tương đối, cùng thư mục
// Props: destructuring ngay trong signature

function LessonCard({ lesson, onClick, onNavigate }) { ... }
```

### 7.2 Inline Style — chỉ khi dynamic

```jsx
// OK — giá trị dynamic không encode được vào class
style={{ backgroundColor: color }}

// KHÔNG OK — dùng CSS class thay
style={{ fontWeight: 'bold' }}
```

### 7.3 Icons — Material Symbols Outlined

```jsx
// KHÔNG dùng icon library ngoài (heroicons, lucide...)
<span className="material-symbols-outlined">icon_name</span>
```

### 7.4 Conditional Rendering

```jsx
{error && <div className="error-message">{error}</div>}
{isLoading ? <div className="page-loading">...</div> : <Content />}
if (!isOpen) return null
```

### 7.5 Constants ngoài component

```js
// Đặt NGOÀI component, ở đầu file
const LIMIT = 8
const CEFR_LEVELS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']
const NAV_ITEMS = [{ key: '...', label: '...', path: '...' }]
```

---

## 8. Form & Validation Pattern

```jsx
const [form, setForm] = useState({ email: '', password: '' })
const [errors, setErrors] = useState({})
const [generalError, setGeneralError] = useState('')
const [isSubmitting, setIsSubmitting] = useState(false)

const validate = () => {
  const newErrors = {}
  if (!form.email) newErrors.email = 'Email không được để trống'
  setErrors(newErrors)
  return Object.keys(newErrors).length === 0
}

const handleSubmit = async (e) => {
  e.preventDefault()
  setGeneralError('')
  if (!validate()) return
  setIsSubmitting(true)
  try {
    const res = await someApi(form)
    if (res.success) onNavigate('/next')
    else setGeneralError(res.message)
  } catch {
    setGeneralError('Đã có lỗi xảy ra. Vui lòng thử lại.')
  } finally {
    setIsSubmitting(false)
  }
}

// Clear error khi user gõ
onChange={(e) => {
  setForm(prev => ({ ...prev, field: e.target.value }))
  if (errors.field) setErrors(prev => ({ ...prev, field: '' }))
}}
```

---

### 8.1 Form UX bắt buộc

- Mỗi input dùng shared `<Input>` khi component này đáp ứng yêu cầu; có `id`, `label`, `value`, `onChange`, `disabled` và `error` phù hợp.
- Hiển thị lỗi cụ thể gần trường dữ liệu; lỗi API không map được vào trường nào thì dùng thông báo tổng quát trong form.
- Xóa lỗi của trường khi người dùng sửa trường đó. Nút submit phải phản ánh trạng thái khi request đang chạy.
- Với quy tắc mật khẩu, trạng thái ban đầu là neutral; chỉ hiện success/error sau khi người dùng bắt đầu nhập.
- Không dựa hoàn toàn vào native validation nếu cần thông báo tiếng Việt hoặc UX nhất quán: dùng `noValidate` và validate ở React.

---

## 9. Patterns Đặc thù

### 9.1 Debounce Search (2-state pattern)

```jsx
const [searchQuery, setSearchQuery] = useState('')
const [debouncedQuery, setDebouncedQuery] = useState('')

useEffect(() => {
  const timer = setTimeout(() => setDebouncedQuery(searchQuery), 500)
  return () => clearTimeout(timer)
}, [searchQuery])
```

### 9.2 Dropdown đóng khi click outside

```jsx
const dropdownRef = useRef(null)
useEffect(() => {
  const handle = (e) => {
    if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
      setIsOpen(false)
    }
  }
  document.addEventListener('mousedown', handle)
  return () => document.removeEventListener('mousedown', handle)
}, [])
```

### 9.3 Promise.all cho parallel fetches

```jsx
const [dataA, dataB] = await Promise.all([fetchA(), fetchB()])
```

### 9.4 Refresh trigger pattern

```jsx
const [refreshTrigger, setRefreshTrigger] = useState(0)
setRefreshTrigger(prev => prev + 1)    // trigger refetch
useEffect(() => { fetchData() }, [refreshTrigger])
```

### 9.5 Success message auto-dismiss

```jsx
useEffect(() => {
  if (!successMsg) return
  const timer = setTimeout(() => setSuccessMsg(''), 3000)
  return () => clearTimeout(timer)
}, [successMsg])
```

---

### 9.6 Third-party browser UI

- Với UI do SDK bên thứ ba render (Google Sign-In, payment widget...), host element phải có width/height ổn định và responsive.
- Không dùng width cứng theo desktop. Nếu SDK nhận width lúc render, đọc width thực tế của host và render lại khi host đổi kích thước.
- Chỉ tải SDK khi cần, cleanup observer/listener khi unmount, và hiển thị lỗi cấu hình thân thiện nếu thiếu env.

---

## 10. Comment Convention

- Comment **tiếng Việt** trong business/feature code
- Comment **tiếng Anh** trong utility/helper code
- Block comment nhóm states:

```jsx
// States dữ liệu
const [lessons, setLessons] = useState([])

// States filter và tìm kiếm
const [searchQuery, setSearchQuery] = useState('')

// States phân trang
const [page, setPage] = useState(1)
const [totalPages, setTotalPages] = useState(1)
```

---

## 11. Quy tắc ưu tiên

### BẮT BUỘC

1. JavaScript + JSX — KHÔNG TypeScript
2. Vanilla CSS — file `.css` riêng cùng thư mục với `.jsx`
3. Custom router — KHÔNG React Router
4. API qua `apiClient.js` — KHÔNG tạo Axios instance mới
5. Naming: Component `PascalCase.jsx`, CSS `PascalCase.css`, API `moduleApi.js`
6. CSS class: `kebab-case`
7. Context hook: `throw new Error` nếu dùng ngoài Provider
8. Navigation: nhận `onNavigate` prop — KHÔNG navigate trực tiếp
9. Design tokens: dùng `var(--token-name)` — KHÔNG hardcode màu
10. Icons: Material Symbols Outlined — KHÔNG dùng icon library khác
11. Đọc `frontend/docs/` và code liên quan trước khi tạo UI/file mới
12. Mọi surface, input, alert và trạng thái interactive mới phải có dark-theme counterpart và responsive behavior

### CẦN TRÁNH

1. Dùng React Router
2. Dùng CSS framework (Tailwind, MUI, Bootstrap...)
3. Tạo thêm Axios instance
4. Import navigate trực tiếp — phải nhận qua `onNavigate` prop
5. Dùng `#000000` hay `#ffffff` thuần — follow design tokens
6. Dùng icon library ngoài (heroicons, lucide...)
7. Quên cleanup trong useEffect (event listener, timer, socket)
8. Đặt CSS class không có prefix cho component-specific styles — dễ conflict
9. Hardcode text user-facing — dùng i18n keys khi project có i18n
10. Đặt Vite env trong `src/` hoặc đưa secret vào `VITE_*`

### LINH HOẠT

1. Comment language: VI hoặc EN, nhưng nhất quán trong 1 file
2. Debounce delay: 400ms (admin) hoặc 500ms (user-facing) — OK tùy context
3. Error state: `null` hoặc `''` — null cho object, string cho message
4. Object state vs individual states cho form phức tạp
