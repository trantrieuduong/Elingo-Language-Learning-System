#!/usr/bin/env bash
# Xuất log cho từng service theo giờ, nén và đẩy lên Cloudflare R2 (prefix logs/).
# Tên thư mục và file log được đặt theo UTC+7 (Asia/Ho_Chi_Minh).
# Truy vấn log của docker compose dùng UTC.
#
# QUY TẮC RETENTION:
# R2 bucket elingo-db-prod-backups có lifecycle xóa logs/ sau 14 ngày.
# Thời gian lưu trữ local (KEEP_DAYS=3) bắt buộc phải NGẮN HƠN lifecycle của R2,
# nhằm tránh việc rclone upload lại các file log cũ đã hết hạn và bị R2 xóa.

set -euo pipefail

cd /opt/elingo

LOG_DIR=/opt/elingo/logs
BUCKET=r2:elingo-db-prod-backups/logs
KEEP_DAYS=3
CATCHUP_HOURS=6
SERVICES=(backend nginx mysql redis)

# Thư mục tạm dùng chứa log chưa nén, tự động dọn sạch khi script thoát
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

# Lấy epoch hiện tại
NOW_EPOCH=$(date +%s)

for svc in "${SERVICES[@]}"; do
  # Chạy bù CATCHUP_HOURS giờ gần nhất, duyệt từ giờ cũ hơn đến giờ gần nhất
  for ((h = CATCHUP_HOURS; h >= 1; h--)); do
    # Tính mốc đầu giờ của h giờ trước
    SLOT_START_EPOCH=$(( ((NOW_EPOCH - h * 3600) / 3600) * 3600 ))
    SLOT_END_EPOCH=$(( SLOT_START_EPOCH + 3600 ))

    # Docker compose logs truy vấn theo chuẩn UTC
    START_UTC=$(date -u -d "@$SLOT_START_EPOCH" +%Y-%m-%dT%H:00:00Z)
    END_UTC=$(date -u -d "@$SLOT_END_EPOCH" +%Y-%m-%dT%H:00:00Z)

    # Thư mục ngày và tên file giờ theo UTC+7 (Asia/Ho_Chi_Minh)
    DAY_VN=$(TZ=Asia/Ho_Chi_Minh date -d "@$SLOT_START_EPOCH" +%Y-%m-%d)
    STAMP_VN=$(TZ=Asia/Ho_Chi_Minh date -d "@$SLOT_START_EPOCH" +%Y-%m-%d_%H)

    TARGET_DIR="$LOG_DIR/$svc/$DAY_VN"
    TARGET_FILE="$TARGET_DIR/$STAMP_VN.log.gz"
    PART_FILE="$TARGET_FILE.part"

    # Nếu file đã tồn tại thì bỏ qua
    if [ -f "$TARGET_FILE" ]; then
      continue
    fi

    mkdir -p "$TARGET_DIR"
    TMP_FILE="$TMP_DIR/${svc}_${STAMP_VN}.log"

    # Trích xuất log, không dùng || true để tránh nuốt lỗi ngầm
    if ! docker compose logs --no-color --no-log-prefix --timestamps \
        --since "$START_UTC" --until "$END_UTC" "$svc" > "$TMP_FILE"; then
      /opt/elingo/notify.sh "❌ Lỗi trích xuất log $svc ($START_UTC -> $END_UTC)" || true
      rm -f "$TMP_FILE"
      continue
    fi

    # Chỉ nén và lưu nếu có dữ liệu log trong khung giờ này
    if [ -s "$TMP_FILE" ]; then
      gzip -c "$TMP_FILE" > "$PART_FILE"
      mv "$PART_FILE" "$TARGET_FILE"
    fi
    rm -f "$TMP_FILE"
  done
done

# Đồng bộ lên Cloudflare R2
rclone copy "$LOG_DIR" "$BUCKET" --include "*.log.gz" --ignore-existing --s3-no-check-bucket -v

# Dọn dẹp log local cũ bằng -mmin để bảo đảm retention local ngắn hơn R2 lifecycle
find "$LOG_DIR" -type f -name "*.log.gz" -mmin +"$((KEEP_DAYS * 1440))" -delete

# Xóa các thư mục ngày rỗng bên trong từng service
find "$LOG_DIR" -mindepth 2 -type d -empty -delete
