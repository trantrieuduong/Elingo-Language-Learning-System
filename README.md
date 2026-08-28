# Elingo - Spring Boot & ReactJS Project

Dự án Elingo được chia thành hai phần chính:
- `backend/`: Mã nguồn Spring Boot cho server API.
- `frontend/`: Mã nguồn ReactJS cho giao diện người dùng.

---

## 1. Cấu trúc Backend (`backend/src/main/java/com/elingo`)

Dự án backend được tổ chức theo kiến trúc **hướng tính năng (Feature-based / Domain-driven)**. Dưới đây là ý nghĩa và nội dung của từng package:

* **`admin/`**: Chứa các API, logic xử lý và tính năng dành riêng cho phân hệ quản trị viên (quản lý người dùng, quản lý nội dung bài học, quản lý gói Premium, xử lý vi phạm, thống kê sử dụng hệ thống).
* **`ai/`**: Chứa các tính năng liên quan đến Trí tuệ nhân tạo như chấm điểm phát âm.
* **`auth/`**: Chứa các xử lý liên quan đến xác thực và phân quyền như Đăng nhập, Đăng ký, cấp phát và xác thực JWT tokens, cấu hình Spring Security.
* **`battle/`**: Chứa logic cho tính năng thi đấu giữa các người dùng với nhau để tăng tính cạnh tranh trong hệ thống.
* **`common/`**: Chứa các lớp dùng chung cho toàn bộ dự án như các hằng số, các kiểu Enum, hoặc các lớp cấu trúc Response chuẩn trả về cho frontend.
* **`config/`**: Chứa các lớp cấu hình của Spring Boot (Cấu hình Database, Security, CORS, Swagger/OpenAPI, WebSocket).
* **`exception/`**: Chứa các lớp xử lý lỗi ngoại lệ toàn cục và định nghĩa các Custom Exceptions riêng của dự án.
* **`file/`**: Chứa logic xử lý việc upload, download và quản lý file (ảnh avatar, âm thanh bài học, video) lưu trữ trên server hoặc qua các dịch vụ Cloud.
* **`gamification/`**: Chứa logic liên quan đến game hóa ứng dụng để tạo động lực cho người dùng (Ví dụ: hệ thống điểm kinh nghiệm XP, chuỗi ngày học).
* **`learning/`**: Chứa tính năng học từ vựng qua Flashcards, bài học video.
* **`mapper/`**: Chứa các class/interface (dùng MapStruct) chứa logic chuyển đổi dữ liệu qua lại giữa Entity và DTO.
* **`post/`**: Chứa logic về bài viết như diễn đàn thảo luận, tính năng đăng bài của cộng đồng người dùng.
* **`premium/`**: Chứa logic xử lý các tính năng dành cho tài khoản trả phí (Premium), tích hợp các cổng thanh toán.
* **`profile/`**: Chứa logic quản lý thông tin cá nhân của người dùng: xem và cập nhật hồ sơ, đổi avatar, thay đổi mật khẩu, email.
* **`progress/`**: Chứa các class để theo dõi, lưu trữ, cập nhật và thống kê tiến trình học tập của người dùng.
* **`report/`**: Chứa logic về báo cáo của người dùng về nội dung học / bài đăng / người dùng khác.
* **`speakingRoom/`**: Chứa tính năng phòng luyện nói, nơi người dùng có thể kết nối để luyện giao tiếp tiếng Anh với nhau.
* **`utils/`**: Chứa các hàm tiện ích hỗ trợ dùng chung như: hàm mã hóa/giải mã, xử lý chuỗi, định dạng ngày tháng, thao tác với JWT, sinh mã ngẫu nhiên.

> **Ghi chú:** Mỗi package tính năng được chia thành các layer chuẩn của Spring Boot:
> - `controller/`: Nhận request HTTP từ client và trả về response.
> - `service/`: Chứa logic nghiệp vụ chính.
> - `repository/`: Chứa các interface tương tác với cơ sở dữ liệu (kế thừa JpaRepository).
> - `dto/`: Cấu trúc dữ liệu dùng để truyền tải (Request DTO, Response DTO).
> - `entity/`: Các lớp ánh xạ (ORM) trực tiếp với các bảng trong cơ sở dữ liệu.

---

## 2. Cấu trúc Frontend (`frontend/src`)

* **`assets/`**: Chứa các tài nguyên tĩnh không bị biên dịch thay đổi tên như hình ảnh, icons, font chữ.
* **`components/`**: Chứa các thành phần giao diện dùng chung và có thể tái sử dụng ở nhiều nơi như Button, Modal, Sidebar, Navbar, Header, Footer.
* **`context/`**: Chứa các React Context API để quản lý trạng thái toàn cục cho ứng dụng như trạng thái người dùng đang đăng nhập, cấu hình Theme sáng/tối.
* **`modules/`**: Chứa các trang, component và logic giao diện thuộc chia theo từng tính năng: Admin, AI, Auth, Battle, Gamification, Learning, Post, Premium, Profile, Progress, Report, SpeakingRoom.
* **`services/`**: Chứa các file khai báo hàm để gọi API đến backend.
* **`utils/`**: Chứa các hàm tiện ích dùng chung trên frontend như format ngày giờ, format tiền tệ, xử lý tính toán logic.
* **`tests/`**: Chứa các file kiểm thử tự động (Unit Test / Integration Test) cho frontend.
