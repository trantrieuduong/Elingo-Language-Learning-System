# Hướng dẫn Deploy & Vận hành Elingo trên VPS Ubuntu

Tài liệu hướng dẫn thiết lập VPS Ubuntu (24.04) từ đầu, cấu hình SSH key cho user `deploy`, thiết lập Cloudflare R2 / `rclone`, thứ tự chạy CI/CD và toàn bộ quy trình vận hành ops (backup, giám sát, đẩy log, khôi phục dữ liệu) tại `/opt/elingo`.

> **Lưu ý cấu hình phần cứng:** Cấu hình Docker Compose hiện tại (`backend/docker-compose.yml`) đang được tối ưu cho **VPS 2 CPU / 8 GB RAM / 40 GB SSD**. Nếu triển khai trên VPS có thông số khác, hãy điều chỉnh lại các giới hạn tài nguyên (`mem_limit`, `cpus`, `JAVA_OPTS`, `--innodb-buffer-pool-size`, `--maxmemory`, `MODERATION_TORCH_THREADS`) và chính sách lưu giữ log/backup cho phù hợp.

---

## 1. Thiết lập ban đầu trên VPS (User `ubuntu` / `root`)

### 1.1. Cập nhật hệ thống và cài đặt công cụ bắt buộc

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl unzip flock python3 logrotate

# Cài đặt Docker
curl -fsSL https://get.docker.com | sudo sh

# Cài đặt rclone bản mới nhất (khuyên dùng thay cho apt để hỗ trợ đầy đủ R2/S3)
curl https://rclone.org/install.sh | sudo bash
```

### 1.2. Tạo user `deploy` và thư mục dự án

```bash
# Tạo user deploy phục vụ CI/CD và vận hành
sudo adduser --disabled-password --gecos "" deploy
sudo usermod -aG docker deploy

# Tạo thư mục dự án và phân quyền cho deploy
sudo mkdir -p /opt/elingo/certs /opt/elingo/ops
sudo chown -R deploy:deploy /opt/elingo
```

### 1.3. Tạo Public Key và Private Key cho user `deploy`

Thực hiện trên **máy cá nhân** (hoặc tạo tạm trên VPS rồi xóa private key khỏi VPS sau khi copy):

```bash
# Tạo cặp khóa SSH Ed25519 (không đặt passphrase để GitHub Actions SSH tự động)
ssh-keygen -t ed25519 -C "elingo-deploy" -f ~/.ssh/elingo_deploy -N ""
```

Lệnh trên tạo ra 2 file:
- **Public key**: `~/.ssh/elingo_deploy.pub` -> dán vào VPS.
- **Private key**: `~/.ssh/elingo_deploy` -> lưu vào GitHub Secrets (`PROD_SERVER_SSH_KEY`).

Xem nội dung 2 file:

```bash
cat ~/.ssh/elingo_deploy.pub
cat ~/.ssh/elingo_deploy
```

Cài đặt **public key** lên VPS cho user `deploy`:

```bash
sudo mkdir -p /home/deploy/.ssh
sudo nano /home/deploy/.ssh/authorized_keys   # Dán nội dung file elingo_deploy.pub vào đây
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys
```

Kiểm tra kết nối SSH từ máy cá nhân bằng private key:

```bash
ssh -i ~/.ssh/elingo_deploy deploy@<IP_VPS>
```

---

## 2. Cấu hình Cloudflare R2 và `rclone`

### 2.1. Chuẩn bị trên Cloudflare (làm trước)

1. Vào **R2 Object Storage** -> **Create bucket** (ví dụ: `elingo-db-prod-backups`). Để **private**, không gắn domain public.
2. Cấu hình **Lifecycle Rules** cho bucket:
   - Prefix `logs/`: tự động xóa file sau **14 ngày**.
   - Prefix `mysql/`: tự động xóa bản backup sau **30 ngày**.
3. Vào **R2** -> **Manage API Tokens** -> **Create API token**:
   - **Permission**: `Object Read & Write`
   - **Scope**: chỉ áp dụng cho bucket vừa tạo (`elingo-db-prod-backups`)
4. Lưu lại 3 giá trị sau khi tạo (Secret chỉ hiện một lần):
   - **Access Key ID**
   - **Secret Access Key**
   - **Endpoint**: dạng `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`

### 2.2. Cấu hình `rclone` trên VPS bằng user `deploy`

> **Quan trọng:** Cron job và `install.sh` chạy dưới user `deploy`, vì vậy phải chạy `rclone config` bằng chính user `deploy` để file cấu hình nằm đúng tại `/home/deploy/.config/rclone/rclone.conf`.

```bash
su - deploy
rclone config
```

Trả lời lần lượt các câu hỏi của `rclone`:

| Câu hỏi | Giá trị nhập |
|---|---|
| `name>` | `r2` |
| `Storage>` | `s3` |
| `provider>` | `Cloudflare` |
| `env_auth>` | Enter (`false`) |
| `access_key_id>` | Dán Access Key ID |
| `secret_access_key>` | Dán Secret Access Key |
| `region>` | `auto` |
| `endpoint>` | Dán endpoint ở trên |
| `location_constraint>` | Enter |
| `acl>` | Enter |
| `Edit advanced config?` | `n` |
| `Keep this "r2" remote?` | `y` |
| Menu cuối | `q` |

Kiểm tra nhanh kết nối R2:

```bash
rclone lsf r2:elingo-db-prod-backups --s3-no-check-bucket
```

---

## 3. Cấu hình GitHub Secrets & Thứ tự chạy CI/CD

### 3.1. Các Secrets / Variables cần thiết trên GitHub

- **Kết nối VPS:**
  - `PROD_SERVER_HOST`: IP hoặc domain của VPS
  - `PROD_SERVER_USER`: `deploy`
  - `PROD_SERVER_SSH_KEY`: toàn bộ nội dung private key `elingo_deploy`
  - `PROD_SERVER_PORT`: cổng SSH (mặc định `22`)
- **Database & Application:**
  - `PROD_CONFIG_ENCRYPTION_KEY`
  - `PROD_MYSQL_ROOT_PASSWORD`, `PROD_MYSQL_DATABASE`, `PROD_MYSQL_USER`, `PROD_MYSQL_PASSWORD`
  - `PROD_JWT_SIGNER_KEY`, `PROD_GOOGLE_CLIENT_ID`
  - `PROD_MAIL_HOST`, `PROD_MAIL_PORT`, `PROD_MAIL_USERNAME`, `PROD_MAIL_PASSWORD`
  - `PROD_R2_ACCOUNT_ID`, `PROD_R2_ACCESS_KEY`, `PROD_R2_SECRET_KEY`, `PROD_R2_BUCKET`, `PROD_R2_PUBLIC_URL`
  - `PROD_MODERATION_SERVICE_TOKEN`
  - `PROD_TG_TOKEN`, `PROD_TG_CHAT_ID` (cảnh báo Telegram)
- **Variables (tùy chọn):**
  - `PROD_DDL_AUTO` (mặc định `validate`)
  - `PROD_MODERATION_SCHEDULER_ENABLED` (mặc định `true`)

### 3.2. Thứ tự build & deploy (Lưu ý bắt buộc)

> **LƯU Ý QUAN TRỌNG:** Khi deploy lần đầu lên VPS mới (hoặc sau khi xóa image trên GHCR), **bắt buộc phải build và push image `moderation-service` trước, sau đó mới chạy CI deploy `backend`.**
>
> **Lý do:** Workflow deploy của `backend` sẽ đồng bộ `docker-compose.yml` và chạy lệnh `docker compose pull backend moderation-service`. Nếu `moderation-service` chưa được build & push lên GHCR (`ghcr.io/<repo>/moderation-service:latest`), quá trình deploy `backend` sẽ thất bại ở bước kéo image.

Thứ tự thực hiện:
1. Chạy workflow của **Moderation Service** (`Moderation Service CI`) để build và push image lên GHCR (lần đầu trên VPS chưa có `docker-compose.yml`, job deploy của moderation-service sẽ tự bỏ qua bước `up` và kết thúc thành công).
2. Chạy workflow của **Backend** (`Backend CI`) để đồng bộ `docker-compose.yml`, `nginx/default.conf`, `ops/*`, tạo `.env`, kéo cả 2 image `backend` và `moderation-service`, khởi động toàn bộ stack và cài đặt ops cron jobs.
3. Nếu cần đặt chứng chỉ SSL cho Nginx, copy chứng chỉ vào `/opt/elingo/certs` trước hoặc sau khi deploy rồi reload Nginx:
   ```bash
   docker compose -f /opt/elingo/docker-compose.yml exec -T nginx nginx -s reload
   ```

---

## 4. Elingo Ops: Backup, Giám sát, Đẩy log

Bộ script trong `backend/ops/` chạy trên VPS với Docker Compose tại `/opt/elingo` (MySQL, Redis, `moderation-service`, `backend`, Nginx), thực thi bởi user `deploy`.

### 4.1. Danh sách các file và lịch trình Crontab

| File | Đặt tại VPS | Chức năng |
|---|---|---|
| `backup.sh` | `/opt/elingo/backup.sh` | `mysqldump` -> `.sql.gz` -> đẩy lên R2 `mysql/<yyyy-mm-dd>/`. Lỗi thì báo Telegram. |
| `notify.sh` | `/opt/elingo/notify.sh` | Gửi tin nhắn Telegram qua `curl` stdin (không lộ token trong `ps`). |
| `monitor.py` | `/opt/elingo/bot/monitor.py` | Quét log backend (`ERROR` kèm `requestId`), CPU, RAM, DISK, trạng thái container, độ tươi của backup. |
| `ship_logs.sh` | `/opt/elingo/ship_logs.sh` | Xuất log từng service theo giờ, nén, đẩy lên R2 `logs/<service>/<yyyy-mm-dd>/`. Chạy bù 6 giờ gần nhất. |
| `logrotate.conf` | `/opt/elingo/logrotate.conf` | Cấu hình logrotate hằng ngày cho log backup và monitor. |
| `install.sh` | `/opt/elingo/ops/install.sh` | Kiểm tra phụ thuộc, sao lưu crontab, đồng bộ script và cập nhật crontab. |
| `alert.env.example` | `/opt/elingo/.alert.env` | Mẫu cấu hình Telegram alert (quyền `600`, không commit token vào git). |
| `crontab.txt` | Nạp tự động qua `install.sh` | Lịch chạy các job, sử dụng `flock` chống chạy chồng. |

Lịch chạy định kỳ trong crontab:

| Job | Khi nào | Lệnh |
|---|---|---|
| `backup.sh` | Mỗi giờ, phút 0 | `flock -n /opt/elingo/.backup.lock /opt/elingo/backup.sh >> /opt/elingo/backups/backup.log 2>&1` |
| `monitor.py` | Mỗi phút | `flock -n /opt/elingo/bot/.lock python3 /opt/elingo/bot/monitor.py >> /opt/elingo/bot/monitor.log 2>&1` |
| `ship_logs.sh` | Mỗi giờ, phút 5 | `flock -n /opt/elingo/.ship_logs.lock /opt/elingo/ship_logs.sh >> /opt/elingo/backups/ship_logs.log 2>&1` |
| `logrotate` | Mỗi ngày, lúc 03:00 | `logrotate -s /opt/elingo/.logrotate.state /opt/elingo/logrotate.conf >> /opt/elingo/backups/logrotate.log 2>&1` |

### 4.2. Quy ước múi giờ và cấu trúc lưu trữ

- **Tên thư mục và tên file log/backup**: Sử dụng múi giờ Việt Nam **UTC+7** (`Asia/Ho_Chi_Minh`), định dạng bằng `TZ=Asia/Ho_Chi_Minh date`.
- **Bộ lọc log Docker**: Lệnh `docker compose logs --since/--until` vẫn sử dụng định dạng chuẩn thời gian **UTC** (`%Y-%m-%dT%H:00:00Z`).

Cấu trúc thư mục lưu trữ trên Cloudflare R2 bucket `elingo-db-prod-backups`:

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

> **Lưu ý về lưu trữ local trên VPS:**
> - File backup MySQL local được **giữ phẳng** trực tiếp trong `/opt/elingo/backups/` (`/opt/elingo/backups/<dbname>_<yyyy-mm-dd_HHMM>.sql.gz`).
> - File log local được lưu theo cây thư mục service và ngày: `/opt/elingo/logs/<service>/<yyyy-mm-dd>/<yyyy-mm-dd_HH>.log.gz`.

### 4.3. Quy tắc Retention (Local vs Cloudflare R2)

> **QUY TẮC BẮT BUỘC: Thời gian lưu trữ (retention) trên local VPS phải luôn NGẮN HƠN lifecycle của Cloudflare R2.**
>
> - Log local: Giữ **3 ngày** (`KEEP_DAYS=3`, dọn bằng `find -mmin +4320`).
> - Backup MySQL local: Giữ **2 ngày** (`KEEP_DAYS=2`, dọn bằng `find -mmin +2880`).
>
> **Lý do:** Lệnh `rclone copy` đồng bộ từ VPS lên Cloudflare R2 sử dụng cờ `--ignore-existing`. Nếu thời gian giữ file trên local dài hơn thời gian sống trên R2, những file đã hết hạn bị R2 xóa sẽ tiếp tục bị `rclone` quét thấy trên VPS và upload ngược trở lại R2, làm vô hiệu hóa chính sách lifecycle.

### 4.4. Cài đặt và cập nhật ops scripts thủ công trên VPS

CI của Backend tự động chạy `install.sh` mỗi lần deploy. Nếu cần chạy thủ công, phải thực thi bằng user `deploy`:

```bash
su - deploy
cd /opt/elingo/ops
bash install.sh
```

Quy trình `install.sh` tự động thực hiện:
1. Xác minh user hiện tại là `deploy`.
2. Kiểm tra các công cụ hệ thống bắt buộc: `docker`, `rclone`, `flock`, `python3`, `curl`.
3. Tạo các thư mục vận hành: `/opt/elingo/bot`, `/opt/elingo/logs`, `/opt/elingo/backups`.
4. Đồng bộ các script thực thi vào `/opt/elingo` với quyền `755` và `logrotate.conf` với quyền `644`.
5. Tạo hoặc đảm bảo file cấu hình `/opt/elingo/.alert.env` có quyền `600`.
6. Sao lưu crontab hiện tại vào `/opt/elingo/crontab.bak` (nếu đọc crontab lỗi sẽ dừng để tránh mất cron job cũ).
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

### 5.5. Kiểm tra `logrotate`

```bash
# Kiểm tra cú pháp và mô phỏng việc rotate log (dry-run, không làm thay đổi file)
logrotate -d -s /opt/elingo/.logrotate.state /opt/elingo/logrotate.conf

# Ép buộc xoay vòng log ngay lập tức để kiểm tra thực tế
logrotate -f -s /opt/elingo/.logrotate.state /opt/elingo/logrotate.conf
```

---

## 6. Khôi phục dữ liệu (Restore) MySQL từ R2

Khi cần khôi phục cơ sở dữ liệu trên VPS từ Cloudflare R2 (thực hiện với user `deploy`):

```bash
cd /opt/elingo

# 1. Tìm bản backup mới nhất trên Cloudflare R2 (duyệt đệ quy qua các thư mục ngày)
LATEST=$(rclone lsf -R r2:elingo-db-prod-backups/mysql/ | grep '\.sql\.gz$' | sort | tail -1)
echo "Bản backup cần restore: $LATEST"

# 2. Tải bản backup về thư mục tạm /tmp
rclone copy "r2:elingo-db-prod-backups/mysql/$LATEST" /tmp/ --s3-no-check-bucket
FILENAME=$(basename "$LATEST")

# 3. Tạm dừng backend (nếu đang chạy) để tránh xung đột kết nối khi nạp dữ liệu
docker compose stop backend

# 4. Đảm bảo container MySQL đang chạy
docker compose up -d mysql

# 5. Khởi tạo database nếu chưa tồn tại (thay 'elingo' nếu MYSQL_DATABASE trong .env khác)
docker compose exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "CREATE DATABASE IF NOT EXISTS elingo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"'

# 6. Giải nén và nạp dữ liệu vào MySQL
gunzip -c "/tmp/$FILENAME" | docker compose exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot elingo'

# 7. Khởi động lại backend
docker compose up -d backend

# 8. Dọn dẹp file tạm sau khi restore thành công
rm -f "/tmp/$FILENAME"
```

---

## 7. Xử lý sự cố thường gặp

| Hiện tượng | Nguyên nhân & Cách xử lý |
|---|---|
| `install.sh` báo thiếu `rclone` | VPS mới chưa cài `rclone`. Cài bằng `curl https://rclone.org/install.sh \| sudo bash` rồi chạy lại deploy hoặc `bash /opt/elingo/ops/install.sh`. |
| Backend deploy lỗi không pull được `moderation-service` | Chưa build/push image `moderation-service` trước. Hãy chạy CI của `moderation-service` trước rồi chạy lại deploy Backend. |
| Không nhận được tin Telegram | Chạy `python3 /opt/elingo/bot/monitor.py --test`; kiểm tra `TG_TOKEN` và `TG_CHAT_ID` trong `/opt/elingo/.alert.env`; xem log tại `/opt/elingo/bot/monitor.log`. |
| Backup báo lỗi quá nhỏ | Kiểm tra dung lượng database và câu lệnh dump; xem chi tiết lỗi tại `/opt/elingo/backups/backup.log`. File backup lỗi tự động bị xóa. |
| Upload R2 thất bại | Giữ nguyên cờ `--s3-no-check-bucket`; kiểm tra cấu hình `rclone` remote `r2` dưới user `deploy`; file backup local vẫn được giữ để lần sau đồng bộ lại. |
| Cron không kích hoạt | Kiểm tra crontab của user `deploy` bằng `crontab -l`; kiểm tra `/opt/elingo/crontab.bak` nếu cần khôi phục cấu hình cũ. |
| Dung lượng ổ đĩa tăng cao | Kiểm tra bằng `df -h`; dọn Docker images thừa với `docker image prune -f`; hệ thống tự dọn log local quá 3 ngày và backup quá 2 ngày. |
