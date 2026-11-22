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

### Màu sắc & Design Tokens
- **Tuyệt đối không hard-code mã màu**: Tất cả thành phần bắt buộc sử dụng CSS variables `var(--adm-*)`.
- **Bảng màu trung tính có sắc độ**: Dùng Slate Navy (`#0f172a`, `#1e293b`), không dùng màu xám thuần generic.
- **Màu chủ đạo & nhấn**: Authority Navy (`#0f172a`), Technical Accent Cerulean (`#0284c7`), Semantic Emerald / Warm Amber / Crimson.
- **Tuyệt đối không dùng gradient tím-xanh generic** hay glassmorphism tràn lan.
- **Đạt chuẩn tương phản WCAG AA**: Màu chữ trên nền tối luôn có độ sáng rõ nét (`--adm-sidebar-text: #cbd5e1`, hover `#ffffff`).

### Bố cục & Responsive (Mobile-First)
- **Bố cục nhịp điệu (Asymmetric Layout)**: Tránh lặp lại thẻ giống hệt nhau; dùng tỷ lệ phân bổ bất đối xứng (ví dụ 65% biểu đồ : 35% cơ cấu danh mục).
- **Header linh hoạt**: Trên màn hình <= 768px, tự động ẩn thanh Breadcrumb rườm rà để nhường chỗ cho nút Menu và nút tài khoản.
- **Form lọc ngày trên Mobile**:
  - Nhóm Granularity (`Daily`, `Weekly`, `Monthly`) chia đều 33.3% chiều rộng (`flex: 1`) như iOS Segmented Control, không để khoảng xám thừa.
  - Nút Áp dụng (`.applyBtn`) và nút Làm mới (`.refreshBtn`) luôn nằm chung một hàng, không rớt nút lẻ loi.
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
