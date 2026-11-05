#!/usr/bin/env bash
# Backup MySQL (Docker) -> /opt/elingo/backups -> Cloudflare R2 (prefix mysql/<yyyy-mm-dd>/).
# Chạy mỗi giờ bằng cron. Tên file theo múi giờ UTC+7 (Asia/Ho_Chi_Minh).
#
# QUY TẮC RETENTION:
# R2 bucket elingo-db-prod-backups có lifecycle xóa mysql/ sau 30 ngày.
# Thời gian lưu trữ local (KEEP_DAYS=2) bắt buộc phải NGẮN HƠN lifecycle của R2,
# nhằm tránh việc rclone upload lại các file backup cũ đã hết hạn và bị R2 xóa.

set -euo pipefail

cd /opt/elingo

BACKUP_DIR=/opt/elingo/backups
KEEP_DAYS=2

# Đọc tên database an toàn từ .env nếu có (không source toàn bộ .env)
DB_NAME="elingo"
if [ -f /opt/elingo/.env ]; then
  ENV_DB=$(grep -E '^[[:space:]]*MYSQL_DATABASE=' /opt/elingo/.env | head -n1 | cut -d'=' -f2- | tr -d '\r"' | tr -d "'" || true)
  if [ -n "${ENV_DB:-}" ]; then
    DB_NAME="$ENV_DB"
  fi
fi

# Backup local giữ phẳng trong /opt/elingo/backups
STAMP=$(TZ=Asia/Ho_Chi_Minh date +%F_%H%M)
FILE="$BACKUP_DIR/${DB_NAME}_${STAMP}.sql.gz"
PART_FILE="${FILE}.part"

mkdir -p "$BACKUP_DIR"

# Bước 1: Dump dữ liệu và kiểm tra tính toàn vẹn
# Mật khẩu được truyền qua biến môi trường MYSQL_PWD để không lộ trong danh sách tiến trình ps
if ! {
  docker compose exec -T mysql sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers --events --set-gtid-purged=OFF --default-character-set=utf8mb4 '"$DB_NAME" \
    | gzip > "$PART_FILE" \
  && gzip -t "$PART_FILE" \
  && [ "$(stat -c %s "$PART_FILE")" -gt 500 ] \
  && mv "$PART_FILE" "$FILE";
}; then
  rm -f "$PART_FILE" "$FILE"
  /opt/elingo/notify.sh "❌ Backup MySQL lỗi trong quá trình dump hoặc kiểm tra file" || true
  exit 1
fi

# Bước 2: Dọn dẹp bản backup local cũ bằng -mmin để đảm bảo retention local ngắn hơn R2 lifecycle
find "$BACKUP_DIR" -maxdepth 1 -name "${DB_NAME}_*.sql.gz" -mmin +"$((KEEP_DAYS * 1440))" -delete

# Bước 3: Đẩy file backup lên Cloudflare R2 vào thư mục ngày theo UTC+7
# Tính DAY ngay thời điểm upload
DAY=$(TZ=Asia/Ho_Chi_Minh date +%F)
R2_DEST="r2:elingo-db-prod-backups/mysql/$DAY/"

# Nếu upload lỗi: giữ lại file local để thử lại ở lần chạy sau, không xóa file
if ! rclone copy "$FILE" "$R2_DEST" --s3-no-check-bucket; then
  /opt/elingo/notify.sh "❌ Upload backup MySQL lên R2 thất bại (đã giữ lại file local)" || true
  exit 1
fi
