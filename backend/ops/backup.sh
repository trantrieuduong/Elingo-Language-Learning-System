#!/usr/bin/env bash
# Backup MySQL (Docker) -> /opt/elingo/backups -> Cloudflare R2. Chạy mỗi giờ bằng cron.
set -euo pipefail

cd /opt/elingo
[ -f /opt/elingo/.alert.env ] && source /opt/elingo/.alert.env || true
[ -f /opt/elingo/.env ] && source /opt/elingo/.env || true

BACKUP_DIR=/opt/elingo/backups
KEEP_DAYS=2
DB_NAME="${MYSQL_DATABASE:-elingo}"
R2_DEST=r2:elingo-db-prod-backups/mysql/
FILE="$BACKUP_DIR/${DB_NAME}_$(date +%F_%H%M).sql.gz"

on_err() {
  rm -f "$FILE"
  /opt/elingo/notify.sh "❌ Backup lỗi (dòng $1)" || true
}
trap 'on_err $LINENO' ERR

mkdir -p "$BACKUP_DIR"

docker compose exec -T mysql sh -c \
  "mysqldump -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --single-transaction --routines --triggers --events --set-gtid-purged=OFF --default-character-set=utf8mb4 $DB_NAME" \
  | gzip > "$FILE"

# file quá nhỏ nghĩa là dump hỏng
[ "$(stat -c %s "$FILE")" -gt 500 ] || { echo "Backup file too small"; false; }

# xóa bản local cũ
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +"$KEEP_DAYS" -delete

# đẩy lên R2
rclone copy "$FILE" "$R2_DEST" --s3-no-check-bucket