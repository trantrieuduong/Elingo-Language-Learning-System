---
name: ai-log-writer
description: Xuất nhật ký sử dụng AI cho một phiên làm việc. CHỈ chạy khi user chủ động gọi (ví dụ sau khi hoàn thành task, "viết log", "xuất log AI", /ai-log-writer). Duyệt lại toàn bộ phiên trong context rồi xuất danh sách log, mỗi dòng theo định dạng phân tách bằng dấu |.
---

# AI Log Writer

## Khi nào chạy

Chỉ chạy khi user gọi. Không tự ghi log sau mỗi lượt hỏi đáp. Lý do: phần {SV sửa} chỉ chính xác khi nhìn lại cả phiên, sau khi user đã chỉnh sửa và phản hồi.

## Định dạng (mỗi dòng một log)

{Công cụ}|{Phiên bản}|{Prompt}|{AI tạo ra}|{SV sửa}|{Ngày thực hiện}

## Quy trình

1. Đọc lại toàn bộ phiên trong context, từ đầu đến cuối.
2. Gom các lượt thành các đơn vị công việc. Mỗi yêu cầu chính tạo ra sản phẩm = 1 dòng. Gộp các lượt tinh chỉnh nhỏ (sửa lỗi, đổi tên, hỏi lại) vào dòng của yêu cầu chính, không tách riêng.
3. Bỏ qua các lượt không tạo sản phẩm (chào hỏi, xác nhận, hỏi nhanh).
4. Với mỗi đơn vị công việc, điền 6 trường theo quy tắc dưới đây.
5. Xuất danh sách theo thứ tự thời gian.

## Quy tắc từng trường

1. **Công cụ**: nơi chạy, ví dụ `Claude Chat`, `Antigravity IDE`, ...
2. **Phiên bản**: tên model thực tế đã chạy, ví dụ `Claude Sonnet 5.5`. Lấy từ thông tin hệ thống, không đoán. Nếu giữa phiên đổi model, ghi đúng model của từng lượt.
3. **Prompt**: promt của user, tóm tắt nếu quá dài, khoảng 3-4 câu. Nếu có nhiều lượt tinh chỉnh, gộp lại thành 1 câu. Không ghi prompt của AI.
4. **AI tạo ra**: liệt kê cụ thể sản phẩm: số lượng và loại file, package/class/module chính, cơ chế quan trọng (ngưỡng, scheduler, timeout...). Khoảng 3-5 câu, không mô tả chung chung.
5. **SV sửa**: chỉ ghi những gì có bằng chứng trong context: user yêu cầu đổi, dán lại bản đã sửa, báo đã bỏ/gộp/tách/đổi tên/đổi cấu hình, hoặc nêu tỷ lệ hoàn chỉnh. Khoảng 2-4 câu. Không tự suy đoán hay bịa. Nếu không thấy dấu hiệu chỉnh sửa, ghi `Chưa ghi nhận chỉnh sửa trong phiên`.
6. **Ngày thực hiện**: `dd/mm/yyyy`, ngày của lượt làm việc đó nếu biết, nếu không thì dùng ngày hiện tại.

## Ràng buộc

- Mỗi log là một dòng, không xuống dòng bên trong.
- Không dùng ký tự `|` trong nội dung các trường (thay bằng `/` hoặc `,`).
- Không bịa file, class hay kết quả chưa thực sự tạo ra.
- Ngắn gọn, đủ ý chính, không liệt kê từng dòng code.

## Cách xuất

- Có công cụ ghi file: append các dòng mới vào cuối `ai-logs.txt` ở thư mục gốc dự án, không ghi đè dòng cũ. Sau đó in lại các dòng vừa ghi.
- Không có công cụ ghi file: in toàn bộ danh sách trong một code block để user copy.
- Sau danh sách, nếu có dòng nào ở `SV sửa` chưa có dữ liệu, hỏi user đúng một câu để bổ sung, rồi xuất lại dòng đó.

## Ví dụ

Claude Chat|Claude Sonnet 5.5|Triển khai phần Spring Boot của moderation.|31 file Java trong com.elingo.community.moderation cùng 2 file test và file yaml mẫu: Pipeline chạy stage theo @Order, DecisionPolicy theo ngưỡng, Scheduler (fixedDelay, tạm dừng khi provider lỗi), Repository bằng JdbcClient, ViSoBertTextModerator, OpenAiModerator, FfmpegFrameExtractor (chỉ giải mã keyframe, Semaphore, timeout).|SV hoàn chỉnh khoảng 80%: bỏ ModerationContext/Decision/Finding, gộp thành ModerationResult và PostForModeration; tách DTO OpenAI; đổi ModerationAction sang package entity; rút gọn ModerationProperties.|07/11/2026