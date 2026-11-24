# Elingo Admin Operations Center — Module Documentation

Tài liệu kỹ thuật tổng quan về kiến trúc, cấu trúc thư mục, quy tắc thiết kế và lập trình cho phân hệ quản trị **Elingo Admin** (`src/modules/admin/`).

---

## 1. Kiến trúc tổng thể & Cô lập phạm vi (Scoped Isolation)

- **Cùng codebase, tách biệt trải nghiệm**: Phân hệ Admin nằm chung dự án Frontend với giao diện học viên nhưng cô lập hoàn toàn về giao diện, định tuyến, CSS design tokens và chunk bundle.
- **Tách Chunk độc lập (Code-splitting)**: Toàn bộ nhánh `/admin/*` được lazy-load trong `App.jsx` thông qua `lazy(() => import('./modules/admin/routes'))`. Người dùng thông thường không bao giờ tải mã nguồn của module Admin.
- **Cô lập Scoped CSS**: Toàn bộ CSS tokens và reset được bọc trong class `.admin-scope` tại `admin-theme.css`. Tuyệt đối không để CSS admin ảnh hưởng đến giao diện học viên và ngược lại.

---

## 2. Cấu trúc thư mục (`src/modules/admin/`)

```
src/modules/admin/
├── components/                    # Các thành phần UI dùng chung trong admin
│   ├── AdminHeader/               # Header: Breadcrumb động, chuyển ngôn ngữ, đổi theme, Avatar Chip dropdown
│   ├── AdminSidebar/              # Sidebar: Phân nhóm backend, dropdown accordion, hỗ trợ thu gọn/mobile drawer
│   ├── DataTable/                 # Bảng dữ liệu chuẩn semantic, loading skeleton, empty state
│   ├── DateRangeFilter/           # Bộ lọc ngày: Presets (7/30/90 ngày), date pickers, granularity, Apply + Refresh
│   ├── Feedback/                  # Feedback UI: AdminSkeletonCard, AdminEmptyState, AdminErrorState
│   ├── MetricCard/                # Thẻ thống kê KPI: hỗ trợ variants, trend delta, href/onClick điều hướng
│   ├── VisualCharts/              # Biểu đồ SVG thuần nhẹ: AreaTimeSeriesChart, StackedBarChart, CategoryBreakdown
│   └── icons/AdminIcons.jsx       # Bộ biểu tượng SVG Lucide đồng nhất nét mảnh 1.75px
├── layouts/
│   ├── AdminLayout.jsx            # Khung điều phối: state theme (light/dark), state sidebar collapsed
│   └── AdminLayout.module.css
├── pages/
│   ├── DashboardPage/             # Bảng điều khiển tổng quan với 4 tab (User, Revenue, Learning, Community)
│   │   ├── tabs/                  # 4 tab component kết nối trực tiếp với 4 controller backend
│   │   ├── DashboardPage.jsx
│   │   └── DashboardPage.module.css
│   └── PlaceholderAdminPage/      # Trang thông báo cho các module đang trong lộ trình phát triển
├── services/
│   └── adminStatsApi.js           # API client gọi đến /admin/stats/* (User, Revenue, Learning, Community)
├── styles/
│   └── admin-theme.css            # Tập trung 100% CSS variables design tokens (Light & Dark mode)
└── routes.jsx                     # Điều hướng các nhánh con của /admin/*
```

---

## 3. Quy tắc thiết kế

Dự án tuân thủ nghiêm ngặt định hướng thẩm mỹ **Precision Operations Minimalist** (Trung tâm điều hành chính xác, sắc nét, chuyên nghiệp):

### Typography
- **Font Display (Tiêu đề, con số lớn)**: `Space Grotesk` — phong cách kỹ thuật, góc cạnh, dứt khoát.
- **Font Body (Văn bản, nhãn, dữ liệu bảng)**: `Be Vietnam Pro` — dễ đọc, tối ưu hiển thị 100% tiếng Việt có dấu.
- **Phân cấp thị giác**: Kích thước và độ đậm chênh lệch có chủ đích giữa số liệu KPI (`26px`, bold 700) và nhãn phụ (`12px`, 500).

### Màu sắc & Design Tokens (Đồng nhất Tuyệt đối 100%)
- **Tập trung duy nhất tại `admin-theme.css` (Single Source of Truth)**:
  Toàn bộ biến màu sắc, background, border, shadow cho cả 2 theme (Light & Dark) BẮT BUỘC phải được định nghĩa tập trung duy nhất tại `src/modules/admin/styles/admin-theme.css`.
- **TUYỆT ĐỐI KHÔNG hard-code fallback mã màu trong các file CSS Module con**:
  - **CẤM**: `color: var(--adm-text-primary, #0f172a);`, `background: var(--adm-bg-surface, #ffffff);`
    *(Lý do: Khi biến không tồn tại hoặc sai tên, trình duyệt sẽ dùng giá trị fallback màu tĩnh của Light mode. Ở Dark mode, nền đổi sang màu đen nhưng chữ lại ăn fallback đen `#0f172a` -> Chữ biến mất hoàn toàn!)*
  - **ĐÚNG**: `color: var(--adm-text-main);`, `background: var(--adm-bg-surface);`
    *(Chỉ gọi biến thuần túy qua `var(--adm-*)`, không kèm bất kỳ mã màu tĩnh nào phía sau)*.
- **Bảng tra cứu Design Tokens chuẩn trong `admin-theme.css`**:

| Danh mục | Tên biến CSS Token | Giá trị Light Mode | Giá trị Dark Mode | Mục đích sử dụng |
|---|---|---|---|---|
| **Văn bản** | `--adm-text-main` | `#0f172a` (Slate 900) | `#f8fafc` (Slate 50) | Tiêu đề chính, văn bản dữ liệu quan trọng, input value |
| | `--adm-text-muted` | `#475569` (Slate 600) | `#cbd5e1` (Slate 300) | Nhãn form, mô tả phụ, text bảng, IP, thời gian |
| | `--adm-text-subtle` | `#64748b` (Slate 500) | `#94a3b8` (Slate 400) | Placeholder, icon mờ, chú thích nhỏ, trạng thái rỗng |
| | `--adm-text-disabled`| `#94a3b8` (Slate 400) | `#64748b` (Slate 500) | Phần tử bị vô hiệu hóa, watermark |
| | `--adm-text-inverse` | `#ffffff` | `#0f172a` | Chữ tương phản nghịch đảo |
| **Nền (Surfaces)**| `--adm-bg-canvas` | `#f6f8fb` | `#090d16` | Nền canvas toàn bộ viewport admin |
| | `--adm-bg-surface` | `#ffffff` | `#111827` | Nền card, bảng, form lọc, modal, ô input |
| | `--adm-bg-surface-subtle` | `#f1f4f9` | `#172033` | Nền hover của hàng, nút bấm, dropdown |
| | `--adm-bg-surface-muted` | `#e8ecf3` | `#1e293b` | Nền chip, badge, code block, info card |
| | `--adm-bg-overlay` | `rgba(15, 23, 42, 0.45)` | `rgba(0, 0, 0, 0.7)` | Backdrop mờ khi mở modal dialog |
| **Viền (Borders)**| `--adm-border` | `#e2e8f0` | `#1f293d` | Đường viền ngăn cách card, ô nhập liệu, bảng |
| | `--adm-border-subtle`| `#edf2f7` | `#172033` | Đường viền mờ giữa các hàng trong bảng |
| | `--adm-border-strong`| `#cbd5e1` | `#334155` | Đường viền khi hover hoặc nhấn mạnh |
| | `--adm-border-focus` | `#0284c7` | `#38bdf8` | Vòng sáng focus accessibility cho input/button |
| **Thương hiệu & Nhấn** | `--adm-primary` | `#0f172a` | `#38bdf8` | Màu thương hiệu quản trị, indicator sidebar |
| | `--adm-accent` | `#0284c7` | `#38bdf8` | Nút hành động chính (Apply, Save), link điều hướng |
| | `--adm-accent-contrast` | `#ffffff` | `#0f172a` | Màu chữ trên nền accent (đảm bảo độ tương phản cao) |
| **Huy hiệu ngữ nghĩa** | `--adm-badge-green-bg` / `text` | `#dcfce7` / `#15803d` | `rgba(16, 185, 129, 0.14)` / `#10b981` | Trạng thái CREATE, Success, Active |
| | `--adm-badge-amber-bg` / `text` | `#fef3c7` / `#b45309` | `rgba(245, 158, 11, 0.14)` / `#f59e0b` | Trạng thái UPDATE, Pending, Warning, Secret |
| | `--adm-badge-red-bg` / `text` | `#fee2e2` / `#b91c1c` | `rgba(239, 68, 68, 0.14)` / `#ef4444` | Trạng thái DELETE, Danger, Error, Inactive |
| | `--adm-badge-blue-bg` / `text` | `#e0f2fe` / `#0369a1` | `rgba(56, 189, 248, 0.15)` / `#38bdf8` | Trạng thái Thông tin, Phân hệ mặc định |

- **Cấm tự ý bịa tên biến**: Tuyệt đối không dùng các tên biến không thuộc bảng kê trên (như `--adm-text-primary`, `--adm-text-secondary`, `--adm-bg-hover`, `--adm-bg-subtle`). Nếu có nhu cầu bổ sung token mới, BẮT BUỘC phải khai báo cả hai phiên bản Light và Dark tại `admin-theme.css` trước khi sử dụng.
- **Đạt chuẩn tương phản WCAG AA**: Không dùng màu xám thuần generic; bảng màu Slate Navy đảm bảo chữ luôn tương phản rõ nét với nền ở cả 2 chế độ sáng và tối.

### Bố cục & Responsive (Mobile-First)
- **Bố cục nhịp điệu (Asymmetric Layout)**: Tránh lặp lại thẻ giống hệt nhau; dùng tỷ lệ phân bổ bất đối xứng (ví dụ 65% biểu đồ : 35% cơ cấu danh mục).
- **Header linh hoạt**: Trên màn hình <= 768px, tự động ẩn thanh Breadcrumb rườm rà để nhường chỗ cho nút Menu và nút tài khoản.
- **Form lọc ngày trên Mobile**:
  - Nhóm Granularity (`Daily`, `Weekly`, `Monthly`) chia đều 33.3% chiều rộng (`flex: 1`) như iOS Segmented Control, không để khoảng xám thừa.
  - Nút Áp dụng (`.applyBtn`) và nút Làm mới (`.refreshBtn`) luôn nằm chung một hàng, không rớt nút lẻ loi.
- **Xử lý Flexbox Form & Search Box trên Mobile (Tránh vỡ chiều cao `flex-basis`)**:
  - Khi container chuyển từ `flex-direction: row` sang `flex-direction: column` trên màn hình nhỏ (<= 768px), các phần tử con dùng `flex: 1 1 <px>` BẮT BUỘC phải được reset thành `flex: none; width: 100%;` (kèm `height: 38px` hoặc `min-height: 0`). Tránh để trình duyệt hiểu `flex-basis` là chiều cao trong trục dọc, làm ô nhập bị kéo giãn khổng lồ và icon kính lúp bị đẩy lệch vị trí.
  - Ô tìm kiếm (`.searchBox`) luôn dùng wrapper có `height` cố định khớp input để icon kính lúp (`SearchIcon`) luôn căn giữa chuẩn xác (`top: 50%; transform: translateY(-50%)`).
- **Tương tác Sidebar thu nhỏ**: Khi Sidebar đang thu nhỏ (72px), click vào bất kỳ nhóm dropdown nào sẽ tự động mở rộng Sidebar sang 260px và xổ menu con xuống ngay lập tức.

---

## 4. Quy tắc Lập trình (Coding Standards)

1. **Đa ngôn ngữ 100% (Không hard-code text)**:
   - Toàn bộ chuỗi hiển thị phải lấy qua hook `useTranslation('admin')`.
   - Cung cấp từ điển đối xứng đầy đủ ở cả hai tệp: `src/locales/vi/admin.json` và `src/locales/en/admin.json`.
2. **CSS Specificity Zero-Reset**:
   - Sử dụng cú pháp `:where(button, input, select, textarea)` để reset tương tác mà không vô tình ghi đè màu chữ của các lớp CSS Module.
   - Thêm `!important` cho các màu chữ trên nền tối của nút Primary/Sidebar để tránh xung đột kế thừa màu chữ ở theme sáng.
3. **Thành phần độc lập & Tái sử dụng**:
   - Component `MetricCard`: Hỗ trợ đầy đủ biến thể màu sắc, khả năng nhận `href` và `onClick` để điều hướng nhanh đến trang chức năng tương ứng kèm hiệu ứng hover chuyên nghiệp.
   - Các biểu đồ SVG thuần trong `VisualCharts/` không phụ thuộc vào thư viện bên ngoài nặng nề, tự động đáp ứng kích thước khung chứa.
4. **Luồng phân quyền và điều hướng**:
   - Admin được phép truy cập tự do vào cả phân hệ quản trị (`/admin/*`) lẫn các trang của học viên (`/dashboard`, `/decks`, ...). Không tự động cưỡng chế chuyển hướng khi admin chủ động sang trang học viên.
   - Tại menu tài khoản phía học viên, khi tài khoản có role `ADMIN`, tự động hiển thị lối tắt *Quản trị hệ thống* (`/admin`) để chuyển đổi nhanh chóng.
5. **Quy chuẩn Phân trang 1-Indexed & Component `AdminPagination`**:
   - Backend Spring Boot cấu hình `spring.data.web.pageable.one-indexed-parameters: true`. Số thứ tự trang trên toàn hệ thống bắt đầu từ **1** (không bắt đầu từ 0).
   - **Bắt buộc dùng Component dùng chung `AdminPagination`** (`src/modules/admin/components/AdminPagination/`):
     Tuyệt đối không viết lại khối HTML/CSS phân trang thủ công ở từng trang quản trị.
     ```jsx
     <AdminPagination
       page={page}
       totalPages={data.totalPages}
       totalElements={data.totalElements}
       isLast={Boolean(data.isLast ?? data.last)}
       onPageChange={setPage}
       unitLabel={t('users.usersCountSuffix')}
       isLoading={loading}
     />
     ```
   - **State mặc định**: `const [page, setPage] = useState(1);` (khởi tạo luôn là `1`).
   - **Tham số API**: Truyền trực tiếp `params: { page, size: 20, ... }`, TUYỆT ĐỐI KHÔNG trừ 1 (`page - 1`).
   - **Hiển thị tổng số bản ghi ngữ cảnh**: Thanh phân trang BẮT BUỘC hiển thị rõ tổng số bản ghi kèm tên đơn vị và trang hiện tại: `{totalElements} {unitLabel} • Trang {page}/{totalPages}`.
   - **Hành vi reset trang**: Khi người dùng nhấn Tìm kiếm, thay đổi bộ lọc hoặc bấm Đặt lại (Reset), BẮT BUỘC phải gọi `setPage(1)` để đưa kết quả về trang đầu tiên.
   - **Responsive trên Mobile**: Trên màn hình nhỏ (<= 600px), component tự động xếp dọc, căn giữa và tăng kích thước nút (36px) để tối ưu thao tác chạm.
6. **Kiểm tra độ tương thích Theme bắt buộc (Theme Verification Checklist)**:
   - Trước khi hoàn tất bất kỳ giao diện nào trong phân hệ admin, bắt buộc phải bật chuyển đổi theme qua lại giữa **Light Mode** và **Dark Mode** trên `AdminHeader` để kiểm tra trực quan.
   - Kiểm tra các phần tử dễ lỗi: nhãn form, văn bản trong input/select, chữ trong bảng, thời gian, địa chỉ IP, action badges, modal backdrop và các nút bấm.
   - Tuyệt đối không để tồn tại bất kỳ đoạn mã màu cứng `#...` nào trong các file CSS Module con.

---

## 5. Quy chuẩn Tổ chức Kiểm thử (Unit Testing Architecture)

Nhằm đảm bảo khả năng bảo trì khi hệ thống mở rộng, bộ kiểm thử đơn vị của Admin được phân bổ theo cấu trúc thư mục tương ứng 1:1 với mã nguồn `src/modules/admin/`:

```text
tests/unit/modules/admin/
├── components/           # Kiểm thử các thành phần UI dùng chung
│   ├── AdminHeader.test.jsx
│   ├── AdminSidebar.test.jsx
│   ├── DateRangeFilter.test.jsx
│   └── MetricCard.test.jsx
├── layouts/              # Kiểm thử bộ khung điều hướng và theme
│   └── AdminLayout.test.jsx
├── pages/                # Kiểm thử các trang quản trị và từng tab nghiệp vụ
│   ├── CommunityStatsTab.test.jsx
│   ├── DashboardPage.test.jsx
│   └── PlaceholderAdminPage.test.jsx
└── services/             # Kiểm thử tầng giao tiếp API và tham số truy vấn
    └── adminStatsApi.test.js
```

- **Quy chuẩn i18n trong kiểm thử**: Tất cả test case giao diện đều chạy đồng thời trên cả 2 ngôn ngữ `['vi', 'en']` thông qua `describe.each(LANGS)` và helper `useLang(lng)`.
- **Phân lập Component**: Dùng `@/` alias cho các import nội bộ, mock `apiClient` và các sub-tabs để đảm bảo tốc độ thực thi dưới 6 giây cho toàn bộ module.
