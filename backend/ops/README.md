# Elingo Ops: backup, giám sát, đẩy log

Bộ script cho VPS chạy Docker Compose tại `/opt/elingo` (MySQL, Redis, backend, nginx), chạy bằng user `deploy` trên hệ điều hành Ubuntu 24.04.

## 1. Các file

| File | Đặt tại VPS | Chức năng |
|---|---|---|
| `backup.sh` | `/opt/elingo/backup.sh` | mysqldump -> `.sql.gz` -> đẩy lên R2 `mysql/<yyyy-mm-dd>/`. Lỗi thì báo Telegram. |
| `notify.sh` | `/opt/elingo/notify.sh` | Gửi tin nhắn Telegram qua curl stdin (không lộ token trong ps). |
| `monitor.py` | `/opt/elingo/bot/monitor.py` | Quét log backend (ERROR kèm requestId), CPU, RAM, DISK, trạng thái container, độ tươi của backup. |
| `ship_logs.sh` | `/opt/elingo/ship_logs.sh` | Xuất log từng service theo giờ, nén, đẩy lên R2 `logs/<service>/<yyyy-mm-dd>/`. Chạy bù 6 giờ gần nhất. |
| `logrotate.conf` | `/opt/elingo/logrotate.conf` | Cấu hình logrotate hằng ngày cho log backup và monitor. |
| `install.sh` | `/opt/elingo/ops/install.sh` | Kiểm tra phụ thuộc, sao lưu crontab, đồng bộ script và cập nhật crontab. |
| `alert.env.example` | `/opt/elingo/.alert.env` | Mẫu cấu hình Telegram alert (quyền 600, không commit token vào git). |
| `crontab.txt` | nạp tự động qua `install.sh` | Lịch chạy các job, sử dụng `flock` chống chạy chồng. |

Lịch chạy định kỳ trong crontab:

| Job | Khi nào | Lệnh |
|---|---|---|
| `backup.sh` | Mỗi giờ, phút 0 | `flock -n /opt/elingo/.backup.lock /opt/elingo/backup.sh >> /opt/elingo/backups/backup.log 2>&1` |
| `monitor.py` | Mỗi phút | `flock -n /opt/elingo/bot/.lock python3 /opt/elingo/bot/monitor.py >> /opt/elingo/bot/monitor.log 2>&1` |
| `ship_logs.sh` | Mỗi giờ, phút 5 | `flock -n /opt/elingo/.ship_logs.lock /opt/elingo/ship_logs.sh >> /opt/elingo/backups/ship_logs.log 2>&1` |
| `logrotate` | Mỗi ngày, lúc 03:00 | `logrotate -s /opt/elingo/.logrotate.state /opt/elingo/logrotate.conf >> /opt/elingo/backups/logrotate.log 2>&1` |

---

## 2. Quy ước múi giờ và cấu trúc lưu trữ

### Quy ước múi giờ
- **Tên thư mục và tên file log/backup**: Sử dụng múi giờ Việt Nam **UTC+7** (`Asia/Ho_Chi_Minh`), định dạng bằng `TZ=Asia/Ho_Chi_Minh date`.
- **Bộ lọc log Docker**: Lệnh `docker compose logs --since/--until` vẫn sử dụng định dạng chuẩn thời gian **UTC** (`%Y-%m-%dT%H:00:00Z`).

### Cấu trúc thư mục lưu trữ trên Cloudflare R2
Trên bucket `elingo-db-prod-backups`, dữ liệu log và backup được tổ chức vào các thư mục ngày theo UTC+7:
```text
logs/<service>/<yyyy-mm-dd>/<yyyy-mm-dd_HH>.log.gz
mysql/<yyyy-mm-dd>/<dbname>_<yyyy-mm-dd_HHMM>.sql.gz
```
Ví dụ:
```text
logs/backend/2026-11-06/2026-11-06_03.log.gz
logs/nginx/2026-11-06/2026-11-06_03.log.gz
mysql/2026-11-06/elingo_2026-11-06_0300.sql.gz
```

> **Lưu ý về lưu trữ local trên VPS**:
> - File backup MySQL local được **giữ phẳng** trực tiếp trong thư mục `/opt/elingo/backups/` (`/opt/elingo/backups/<dbname>_<yyyy-mm-dd_HHMM>.sql.gz`).
> - File log local được lưu theo cây thư mục service và ngày: `/opt/elingo/logs/<service>/<yyyy-mm-dd>/<yyyy-mm-dd_HH>.log.gz`.

---

## 3. Quy tắc Retention (Local vs Cloudflare R2)

Cấu hình Lifecycle Rules trên Cloudflare R2 bucket `elingo-db-prod-backups`:
- Prefix `logs/`: Tự động xóa file sau **14 ngày**.
- Prefix `mysql/`: Tự động xóa bản backup sau **30 ngày**.

> **QUY TẮC BẮT BUỘC: Thời gian lưu trữ (retention) trên local VPS phải luôn NGẮN HƠN lifecycle của Cloudflare R2.**
> 
> - Log local: Giữ **3 ngày** (`KEEP_DAYS=3`, dọn bằng `find -mmin +4320`).
> - Backup MySQL local: Giữ **2 ngày** (`KEEP_DAYS=2`, dọn bằng `find -mmin +2880`).
>
> **Lý do**: Lệnh `rclone copy` đồng bộ từ VPS lên Cloudflare R2 sử dụng cờ `--ignore-existing`. Nếu thời gian giữ file trên local dài hơn thời gian sống trên R2, những file đã hết hạn bị R2 xóa sẽ tiếp tục bị `rclone` quét thấy trên VPS và upload ngược trở lại R2, làm vô hiệu hóa chính sách lifecycle.

---

## 4. Cài đặt và cập nhật trên VPS

Script cài đặt bắt buộc phải được thực thi bởi user `deploy`:

```bash
# Đăng nhập vào VPS với user deploy
su - deploy
cd /opt/elingo/ops

# Chạy script cài đặt
bash install.sh
```

Quy trình `install.sh` tự động thực hiện:
1. Xác minh user hiện tại là `deploy`.
2. Kiểm tra các công cụ hệ thống bắt buộc: `docker`, `rclone`, `flock`, `python3`, `curl`.
3. Tạo các thư mục vận hành: `/opt/elingo/bot`, `/opt/elingo/logs`, `/opt/elingo/backups`.
4. Đồng bộ các script thực thi vào `/opt/elingo` với quyền `755` và `logrotate.conf` với quyền `644`.
5. Tạo hoặc đảm bảo file cấu hình `/opt/elingo/.alert.env` có quyền nghiêm ngặt `600`.
6. Tự động sao lưu crontab hiện tại vào `/opt/elingo/crontab.bak`. Nếu việc đọc crontab gặp lỗi hệ thống (ngoại trừ trường hợp chưa có crontab), script sẽ dừng ngay lập tức để tránh làm mất cron jobs khác.
7. Cập nhật khối cron job vào crontab của user `deploy`.

---

## 5. Kiểm tra thủ công từng script

### 5.1. Kiểm tra thông báo Telegram (`notify.sh`)
```bash
/opt/elingo/notify.sh "Thử nghiệm gửi tin nhắn cảnh báo thủ công"
```

### 5.2. Kiểm tra bot giám sát (`monitor.py`)
```bash
# Kiểm tra gửi tin nhắn thử nghiệm
python3 /opt/elingo/bot/monitor.py --test

# Chạy chu kỳ quét bình thường
python3 /opt/elingo/bot/monitor.py
```

### 5.3. Kiểm tra backup MySQL (`backup.sh`)
```bash
# Chạy backup thủ công
/opt/elingo/backup.sh

# Kiểm tra file backup mới tạo trên local (lưu phẳng trong backups/)
ls -lh /opt/elingo/backups/*.sql.gz

# Kiểm tra file trên Cloudflare R2 trong thư mục ngày
DAY=$(TZ=Asia/Ho_Chi_Minh date +%F)
rclone lsf "r2:elingo-db-prod-backups/mysql/$DAY/" | tail -5
```

### 5.4. Kiểm tra nén và đẩy log (`ship_logs.sh`)
```bash
# Chạy xuất và đẩy log thủ công
/opt/elingo/ship_logs.sh

# Kiểm tra cây thư mục log local
ls -R /opt/elingo/logs

# Kiểm tra danh sách log trên Cloudflare R2
rclone lsf r2:elingo-db-prod-backups/logs/backend/ | tail -5
```

### 5.5. Kiểm tra logrotate
```bash
# Kiểm tra cú pháp và mô phỏng việc rotate log (dry-run, không làm thay đổi file)
logrotate -d -s /opt/elingo/.logrotate.state /opt/elingo/logrotate.conf

# Ép buộc xoay vòng log ngay lập tức để kiểm tra thực tế
logrotate -f -s /opt/elingo/.logrotate.state /opt/elingo/logrotate.conf
```

---

## 6. Khôi phục dữ liệu (Restore) MySQL từ R2

Khi cần khôi phục cơ sở dữ liệu trên VPS từ Cloudflare R2:

```bash
cd /opt/elingo

# 1. Tìm bản backup mới nhất trên Cloudflare R2 (duyệt đệ quy qua các thư mục ngày)
LATEST=$(rclone lsf -R r2:elingo-db-prod-backups/mysql/ | grep '\.sql\.gz$' | sort | tail -1)
echo "Bản backup cần restore: $LATEST"

# 2. Tải bản backup về thư mục tạm /tmp
rclone copy "r2:elingo-db-prod-backups/mysql/$LATEST" /tmp/ --s3-no-check-bucket
FILENAME=$(basename "$LATEST")

# 3. Đảm bảo container MySQL đang chạy
docker compose up -d mysql

# 4. Khởi tạo database nếu chưa tồn tại
docker compose exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "CREATE DATABASE IF NOT EXISTS elingo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"'

# 5. Giải nén và nạp dữ liệu vào MySQL
gunzip -c "/tmp/$FILENAME" | docker compose exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot elingo'

# 6. Dọn dẹp file tạm sau khi restore thành công
rm -f "/tmp/$FILENAME"
```

---

## 7. Xử lý sự cố thường gặp

| Hiện tượng | Nguyên nhân & Cách xử lý |
|---|---|
| Không nhận được tin Telegram | Chạy `python3 /opt/elingo/bot/monitor.py --test`; kiểm tra giá trị `TG_TOKEN` và `TG_CHAT_ID` trong `/opt/elingo/.alert.env`; kiểm tra file log tại `/opt/elingo/bot/monitor.log`. |
| Backup báo lỗi quá nhỏ | Kiểm tra dung lượng database và câu lệnh dump; xem chi tiết lỗi tại `/opt/elingo/backups/backup.log`. File backup bị lỗi sẽ tự động bị xóa để không để lại dữ liệu hỏng. |
| Upload R2 thất bại | Giữ nguyên cờ `--s3-no-check-bucket`; kiểm tra cấu hình rclone remote `r2`; file backup trên VPS vẫn được bảo lưu để lần chạy sau tiếp tục đồng bộ. |
| Cron không kích hoạt | Kiểm tra crontab của user deploy bằng `crontab -l`; kiểm tra nội dung file `/opt/elingo/crontab.bak` nếu cần khôi phục lại cấu hình trước đó. |
| Dung lượng ổ đĩa tăng cao | Kiểm tra với lệnh `df -h`; dọn dẹp Docker images không sử dụng với `docker image prune -f`; hệ thống tự động dọn log local quá 3 ngày và backup quá 2 ngày. |
