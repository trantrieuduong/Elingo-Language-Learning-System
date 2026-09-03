# Frontend Architecture - ReactJS
Dự án frontend được xây dựng bằng ReactJS với cấu trúc phân chia theo tính năng và các thành phần dùng chung. Cấu trúc chính nằm trong thư mục `frontend`:

```text
frontend/
├── src/                    # Chứa mã nguồn chính của ứng dụng
│   ├── assets/             # Hình ảnh, fonts, icons, stylesheets chung
│   ├── components/         # Các UI components dùng chung (Button, Input, Header, Footer,...)
│   ├── context/            # React Context (AuthContext, ThemeContext,...)
│   ├── modules/            # Đóng gói các tính năng (nghiệp vụ chính) của ứng dụng
│   │   ├── admin/          # Module Quản trị viên
│   │   ├── auth/           # Module đăng nhập, đăng ký, quên mật khẩu
│   │   ├── battle/         # Module phòng thi đấu
│   │   ├── community/      # Module cộng đồng (Bài viết, bình luận,...)
│   │   ├── gamification/   # Module huy hiệu, bảng xếp hạng, điểm thưởng
│   │   ├── learning/       # Module nội dung học tập
│   │   │   ├── flashcard/  # Module học qua thẻ ghi nhớ
│   │   │   ├── videolesson/# Module bài học video
│   │   │   ├── dictation/  # Module nghe chép chính tả
│   │   │   └── shadowing/  # Module luyện nói đuổi
│   │   ├── notification/   # Module thông báo hệ thống
│   │   ├── premium/        # Module quản lý gói Premium
│   │   ├── progress/       # Module tiến độ học tập
│   │   ├── report/         # Module báo cáo vi phạm
│   │   ├── speaking/       # Module phòng luyện nói
│   │   ├── user/           # Module hồ sơ người dùng
│   │   └── vocabulary/     # Module quản lý từ vựng
│   ├── services/           # Cấu hình API client (Axios)
│   ├── utils/              # Các hàm hỗ trợ, constants, formatters
│   ├── tests/              # Thư mục chứa Unit/Integration Tests
│   ├── App.jsx             # Component gốc của ứng dụng
│   ├── index.css           # CSS toàn cục
│   └── main.jsx            # Điểm entry của React
├── .env                    # Biến môi trường
├── package.json            # Thông tin dependencies và scripts
└── vite.config.js          # Cấu hình Vite
```

## 1. `assets/`:
- Chứa các tài nguyên tĩnh không bị thay đổi khi biên dịch như hình ảnh, icons, font chữ, hoặc global styles.

## 2. `components/`:
- Chứa các thành phần UI cơ bản, độc lập với nghiệp vụ nhằm tái sử dụng thành phần UI ở nhiều nơi, đảm bảo tính nhất quán của giao diện. (VD: `Button`, `Modal`, `Sidebar`, `Navbar`, `Table`,...).

## 3. `context/`:
- Quản lý trạng thái toàn cục thông qua React Context API (Giữ các trạng thái như thông tin user đang đăng nhập `AuthContext`, cấu hình giao diện Sáng/Tối `ThemeContext`).

## 4. `services/`:
- Cấu hình instance của Axios nhằm tự động đính kèm Token, xử lý lỗi tập trung.

## 5. `utils/`:
- Chứa các hàm tiện ích nhằm xử lý các logic thuần túy không liên quan trực tiếp đến UI (VD: format ngày giờ `dateUtils.js`, format tiền tệ, validation regex).

## 6. `tests/`:
- Chứa các file kiểm thử tự động (Unit Test / Integration Test), đảm bảo tính đúng đắn của logic và components trước khi deploy.

## 7. `modules/`
Thư mục `modules/` áp dụng kiến trúc phân chia theo tính năng. Mỗi thư mục con đại diện cho một mảng nghiệp vụ độc lập và đồng bộ với backend. Mỗi module con chia tiếp thành `components/`, `pages/`, `api.js` dành riêng cho module đó

* **`admin/`**: Giao diện và logic dành cho quản trị viên (Quản lý người dùng, nội dung học, gói Premium, xử lý báo cáo).
* **`auth/`**: Giao diện và logic toàn bộ luồng xác thực (Trang Đăng nhập, Đăng ký, Xác thực OTP, Quên mật khẩu).
* **`battle/`**: Giao diện và logic tính năng thi đấu đối kháng (Tạo phòng, nhập mã phòng, tham gia trận đấu).
* **`community/`**: Giao diện và logic tính năng mạng xã hội (bài đăng, bình luận, tương tác).
* **`gamification/`**: Giao diện và logic trò chơi hóa (Huy hiệu, bảng xếp hạng, chuỗi ngày học).
* **`learning/`**: Giao diện và logic xử lý các phương pháp học tập chính. Chứa các module con:
    - `flashcard/`: Giao diện và logic học qua thẻ ghi nhớ (Học, lưu từ mới, ôn tập từ đã lưu).
    - `dictation/`: Giao diện và logic nghe chép chính tả.
    - `shadowing/`: Giao diện và logic luyện nói đuổi.
    - `videolesson/`: Giao diện và logic quản lý bài học video (CRUD bài học).
* **`notification/`**: Giao diện và logic xử lý việc hiển thị thông báo.
* **`premium/`**: Giao diện và logic chức năng quản lý các gói Premium, thanh toán, trạng thái thành viên Premium.
* **`progress/`**: Giao diện và logic hiển thị biểu đồ, thống kê tiến độ cá nhân của người học (Thời gian học, số từ vựng thuộc, Phần trăm hoàn thành các bài học).
* **`report/`**: Giao diện và logic báo cáo (Báo cáo nội dung / người dùng vi phạm).
* **`speaking/`**: Giao diện và logic phòng luyện nói với giữa các người dùng (Tạo phòng, nhập mã phòng, tham gia phòng).
* **`user/`**: Giao diện và logic trang cá nhân của người dùng (Cập nhật avatar, đổi tên, đổi mật khẩu).
* **`vocabulary/`**: Giao diện và logic quản lý bộ từ vựng (CRUD bộ từ vựng).
