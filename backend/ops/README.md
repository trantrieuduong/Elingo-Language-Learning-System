# Elingo Ops: backup, giám sát, đẩy log

Bộ script cho VPS chạy Docker Compose tại `/opt/elingo` (MySQL, Redis, backend, nginx), chạy bằng user `deploy`.

## 1. Các file

| File | Đặt tại VPS | Chức năng |
|---|---|---|
| `backup.sh` | `/opt/elingo/backup.sh` | mysqldump → `.sql.gz` → đẩy lên R2 `mysql/`. Lỗi thì báo Telegram. |
| `notify.sh` | `/opt/elingo/notify.sh` | Gửi tin nhắn Telegram. Các script khác gọi tới. |
| `monitor.py` | `/opt/elingo/bot/monitor.py` | Quét log backend: ERROR kèm requestId. Cảnh báo CPU/RAM. |
| `ship_logs.sh` | `/opt/elingo/ship_logs.sh` | Xuất log từng service theo giờ, nén, đẩy lên R2 `logs/`. |
| `install.sh` | `/opt/elingo/ops/install.sh` | Tự động đồng bộ các script, tạo thư mục và cài đặt crontab. |
| `alert.env.example` | `/opt/elingo/.alert.env` | Mẫu cấu hình Telegram alert (không commit token vào git). |
| `crontab.txt` | nạp tự động qua `install.sh` | Lịch chạy các job. |

Lịch chạy định kỳ:

| Job | Khi nào |
|---|---|
| backup.sh | mỗi giờ, phút 0 |
| monitor.py | mỗi phút (có `flock` chống chạy chồng) |
| ship_logs.sh | mỗi giờ, phút 5 (xuất log của giờ trước) |

## 2. Cấu hình GitHub Secrets

Tất cả các thông số bảo mật được cấu hình tập trung trong GitHub Repository (Settings -> Secrets and variables -> Actions). CI/CD sẽ tự động đưa các biến này lên VPS, không cần đăng nhập VPS để thao tác thủ công.

**Telegram Bot:**
1. Nhắn `@BotFather` trên Telegram để tạo bot và lấy `token`.
2. Mở chat với bot vừa tạo và gửi một tin bất kỳ.
3. Mở `https://api.telegram.org/bot<TOKEN>/getUpdates`, tìm `"chat":{"id":...}` để lấy `chat_id`.
4. Thêm 2 secret trên GitHub:
   - `PROD_TG_TOKEN`: Token của bot Telegram.
   - `PROD_TG_CHAT_ID`: Chat ID nhận thông báo.

**Cloudflare R2:**
Cấu hình lifecycle rules trên Cloudflare R2 bucket `elingo-db-prod-backups`:
- Prefix `mysql/`: xóa bản backup sau 30 ngày.
- Prefix `logs/`: xóa log lưu trữ sau 14 đến 30 ngày.

## 3. Quy trình triển khai tự động qua CI/CD

Khi có commit push lên nhánh `main`, GitHub Actions sẽ tự động thực hiện:
1. Đồng bộ toàn bộ script trong `backend/ops/` lên `/opt/elingo/ops/` trên VPS.
2. Tự động khởi tạo hoặc cập nhật file `/opt/elingo/.alert.env` từ `PROD_TG_TOKEN` và `PROD_TG_CHAT_ID` với quyền `600`.
3. Tự động chạy `/opt/elingo/ops/install.sh`:
   - Tạo sẵn các thư mục: `/opt/elingo/bot`, `/opt/elingo/logs`, `/opt/elingo/backups`.
   - Cập nhật các script thực thi và cấp quyền `755`.
   - Kiểm tra và cài đặt khối cron jobs vào crontab của VPS.

## 4. Thử cảnh báo

**Lỗi trong log (kèm requestId):**
```bash
cd /opt/elingo
python3 /opt/elingo/bot/monitor.py     # lần đầu chỉ đặt mốc
docker compose exec backend sh -c 'echo "{\"@timestamp\":\"2026-11-01T17:00:00.000+07:00\",\"message\":\"Lỗi thử nghiệm\",\"logger_name\":\"TEST-LOGGER\",\"level\":\"ERROR\",\"requestId\":\"test123456ab\"}" > /proc/1/fd/1'
python3 /opt/elingo/bot/monitor.py     # nhận tin báo có "req: test123456ab"
```

**CPU/RAM:** tạm sửa `RAM_LIMIT = 10` trong `monitor.py`, đợi 3 phút để nhận cảnh báo, sau đó khôi phục lại ngưỡng ban đầu.

## 5. Tùy chỉnh

Đầu file `monitor.py`:

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `CPU_LIMIT` / `RAM_LIMIT` | 85 | ngưỡng % (RAM tính theo MemAvailable) |
| `SUSTAIN` | 3 | số phút liên tiếp vượt ngưỡng mới báo |
| `REMIND_SEC` | 1800 | nhắc lại nếu chưa hết |
| `ERR_COOLDOWN` | 600 | cùng một lỗi không báo lại trong khoảng này |
| `MAX_GROUPS` | 5 | số nhóm lỗi tối đa mỗi tin |

Trong `backup.sh`: `DB_NAME`, `KEEP_DAYS` (số ngày giữ bản local), `R2_DEST`.

Bot nhận diện lỗi bằng trường `level` = `ERROR` trong log JSON, đọc `requestId`, `logger_name`, `message`, `@timestamp` và `stack_trace`. Nếu tên trường của bạn khác, sửa trong hàm `parse_event`.

## 6. Khôi phục (restore) từ R2

```bash
cd /opt/elingo
LATEST=$(rclone lsf r2:elingo-db-prod-backups/mysql/ | sort | tail -1)
rclone copy "r2:elingo-db-prod-backups/mysql/$LATEST" /tmp/

docker compose up -d mysql
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS elingo CHARACTER SET utf8mb4"'
gunzip -c "/tmp/$LATEST" | docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" elingo'
```

Khi VPS mất hẳn: thuê VPS mới → cài Docker → dựng lại `/opt/elingo` (deploy qua CI) → restore như trên → `docker compose up -d`. Dữ liệu tối đa mất khoảng 1 giờ. Nên diễn tập restore vào DB tạm ít nhất một lần.

## 7. Xử lý sự cố

| Triệu chứng | Cách xử lý |
|---|---|
| Không nhận tin Telegram | `python3 /opt/elingo/bot/monitor.py --test`; kiểm tra `PROD_TG_TOKEN` và `PROD_TG_CHAT_ID` trong GitHub Secrets; kiểm tra bot đã được gửi tin nhắn đầu tiên chưa; xem log tại `/opt/elingo/bot/monitor.log` |
| Có ERROR trong log mà bot im lặng | xem `monitor.log`; chạy tay `docker compose logs --since 10m backend` kiểm tra có dòng JSON `"level":"ERROR"`; nhớ rằng cùng một lỗi sẽ được gộp trong 10 phút |
| Backup lỗi `AccessDenied`/`501` | giữ cờ `--s3-no-check-bucket`; kiểm tra token R2 còn hạn và đúng bucket |
| Cron không chạy | `grep CRON /var/log/syslog` (cần quyền admin) hoặc xem file log trong `/opt/elingo/backups/`; kiểm tra dòng `PATH=` đầu crontab |
| Đĩa đầy | `df -h`, `docker system df`; dọn image cũ `docker image prune -f`; giới hạn log Docker bằng `max-size`/`max-file` trong compose |
