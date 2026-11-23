---
name: bug-report-writing
description: Hướng dẫn tạo bug report bằng tiếng Việt theo mẫu bên dưới. Dùng khi người dùng yêu cầu viết hoặc chuẩn hóa báo cáo lỗi.
---

# Bug Report Writting Guidelines

Viết bug report bằng **tiếng Việt**, theo đúng mẫu bên dưới. Mục tiêu: người đọc (dev, QA, leader) hiểu lỗi, tái hiện được trong 1 lần thử và biết hành vi đúng là gì mà không phải hỏi lại.

## Cấu trúc Report bắt buộc

Giữ nguyên thứ tự, tên mục song ngữ `Tiếng Việt (English)`, heading dùng `###`.

```
### Mô tả (Description)
<ở đâu + làm gì + kết quả sai>

### Các bước tái hiện lỗi (Steps to Reproduce)
1. ...
2. ...
3. ...

### Hành vi hiện tại (Current Behavior)
<điều thực sự xảy ra>

### Screenshot
<ảnh/GIF/log, nếu có>

### Hành vi mong đợi (Expected Behavior)
<điều lẽ ra phải xảy ra>

### Mức độ ảnh hưởng (Impact)
- ...
- ...

### Môi trường kiểm thử (Environment)
- Hệ điều hành: ...
- Trình duyệt: ...

### Thông tin bổ sung (Additional Information)
URL: ...
<ghi chú thêm, nếu có>
```

## Quy trình

1. **Thu thập thông tin** từ tin nhắn, ảnh, log người dùng đưa. Tự suy ra những gì có thể (vd: URL từ ảnh chụp, tên màn hình).
2. **Chỉ hỏi lại khi thiếu thứ không thể đoán** và hỏi gộp một lần: bước tái hiện, môi trường (OS/trình duyệt/thiết bị), hành vi mong đợi. Không bịa phiên bản trình duyệt, OS hay URL.
3. **Viết báo cáo** theo mẫu. Mục nào thật sự không có thông tin thì ghi "Chưa có" thay vì bỏ mục hoặc bịa nội dung (riêng Screenshot có thể để trống để người dùng tự dán ảnh).
4. **Trả kết quả** trong khối code (markdown) để người dùng copy thẳng vào GitHub. Không thêm lời giải thích dài ngoài báo cáo; nếu có điểm giả định, nêu tối đa 1-2 dòng ở cuối.

## Quy tắc viết từng mục

**Mô tả**: một đến hai câu, theo cấu trúc *"Tại [màn hình/trang], khi [hành động], nếu [điều kiện], [kết quả sai]."* Không đưa nguyên nhân phỏng đoán hay giải pháp vào đây.

**Các bước tái hiện lỗi**: đánh số, mỗi bước một hành động người dùng thực hiện được, bắt đầu bằng động từ (Nhấn, Nhập, Xóa trắng, Chọn). Gồm cả điều kiện tiên quyết nếu cần (đăng nhập quyền admin, dữ liệu có sẵn). Tên nút/trường đặt trong dấu " " đúng như giao diện hiển thị. Đủ ngắn để làm theo, không bỏ bước quan trọng.

**Hành vi hiện tại**: mô tả khách quan điều quan sát được, kể cả thông báo lỗi hiển thị nguyên văn nếu có. Không dùng từ cảm xúc ("rất tệ", "lỗi nghiêm trọng").

**Screenshot**: giữ nguyên thẻ `<img ...>` hoặc link ảnh người dùng cung cấp. Nếu có nhiều ảnh, đặt theo thứ tự các bước. Không tự tạo ảnh thay thế.

**Hành vi mong đợi**: đối lập trực tiếp với hành vi hiện tại, cụ thể và kiểm chứng được (hệ thống *báo lỗi X* và *giữ lại Y*).

**Mức độ ảnh hưởng**: dạng gạch đầu dòng, nêu hậu quả thực tế: ai bị ảnh hưởng, công việc nào bị chậm/chặn, dữ liệu có bị mất hay sai không, có workaround không (vd: "phải refresh lại trang"). Không cần gán nhãn Severity trừ khi người dùng yêu cầu.

**Môi trường kiểm thử**: hai dòng cố định *Hệ điều hành / Trình duyệt*. Với lỗi mobile thay bằng *Phiên bản Android hoặc iOS / Trình duyệt*. Với lỗi API/backend thêm *Môi trường (dev/prod)*.

**Thông tin bổ sung**: dòng `URL` đặt đầu tiên, sau đó là ghi chú liên quan: tần suất lỗi, log, quan sát phụ, đề xuất cải thiện nhỏ. Các quan sát khác không thuộc lỗi chính (vd: đề xuất autofocus) đặt ở đây, không trộn vào các mục trên.

## Văn phong

- Ngắn gọn, dùng thuật ngữ nhất quán với giao diện ứng dụng.
- Mỗi bug report chỉ mô tả **một lỗi**. Nếu người dùng đưa nhiều lỗi, tách thành nhiều báo cáo và nói rõ.
- Không đổ lỗi cho người; không đoán nguyên nhân gốc trừ khi người dùng cung cấp.
- Giữ nguyên thuật ngữ kỹ thuật tiếng Anh đã quen dùng (refresh, autofocus, URL, API).

## Ví dụ hoàn chỉnh
```
### Mô tả (Description)
Tại trang bài học, khi chỉnh sửa một bài học, nếu bỏ trống tiêu đề nhấn lưu, trang có báo lỗi nhưng mất biểu mẫu nhập liệu.

### Các bước tái hiện lỗi (Steps to Reproduce)
1. Nhấn biểu tượng cây bút tại một bài học cần chỉnh sửa
2. Xóa trắng tiêu đề
3. Nhấn “Lưu”

### Hành vi hiện tại (Current Behavior)
Có báo lỗi thiếu tiêu đề nhưng mất biểu mẫu nhập liệu

### Screenshot
<img width="952" height="507" alt="Image" src="https://github.com/user-attachments/assets/90c23390-40ec-4f24-9310-897459862eac" />

### Hành vi mong đợi (Expected Behavior)
Báo lỗi thiếu tiêu đề và giữ lại biểu mẫu nhập liệu cho người dùng chỉnh sửa và gửi lại

### Mức độ ảnh hưởng (Impact)
- Làm chậm quy trình nhập liệu của quản trị viên, phải refresh lại trang.
- Giảm trải nghiệm người dùng

### Môi trường kiểm thử (Environment)
- Hệ điều hành: Windows 11
- Trình duyệt: Chrome 148

### Thông tin bổ sung (Additional Information)
URL: admin/lessons/{lessonId}/edit

Cần bổ sung autofocus vào ô nhập liệu tiêu đề bài học trên trang tạo mới sau khi trang được tải.
```
