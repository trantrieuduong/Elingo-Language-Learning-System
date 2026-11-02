#!/usr/bin/env bash
# Xuất log của giờ vừa kết thúc cho từng service, nén và đẩy lên R2 (prefix logs/).
# Tên file theo giờ UTC (giờ VN = UTC+7). Chạy mỗi giờ ở phút thứ 5.
set -euo pipefail

cd /opt/elingo
trap '/opt/elingo/notify.sh "❌ Đẩy log lên R2 lỗi (dòng $LINENO)"' ERR

LOG_DIR=/opt/elingo/logs
BUCKET=r2:elingo-db-prod-backups/logs
KEEP_DAYS=3

START=$(date -u -d '1 hour ago' +%Y-%m-%dT%H:00:00Z)
END=$(date -u +%Y-%m-%dT%H:00:00Z)
STAMP=$(date -u -d '1 hour ago' +%F_%H)

for svc in backend nginx mysql redis; do
  mkdir -p "$LOG_DIR/$svc"
  tmp=$(mktemp)
  docker compose logs --no-color --no-log-prefix --timestamps \
    --since "$START" --until "$END" "$svc" > "$tmp" || true
  if [ -s "$tmp" ]; then
    gzip -c "$tmp" > "$LOG_DIR/$svc/$STAMP.log.gz"
  fi
  rm -f "$tmp"
done

rclone copy "$LOG_DIR" "$BUCKET" --include "*.log.gz" --s3-no-check-bucket

find "$LOG_DIR" -name '*.log.gz' -mtime +"$KEEP_DAYS" -delete
