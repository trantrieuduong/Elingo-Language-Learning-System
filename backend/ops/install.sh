#!/usr/bin/env bash
# Cài đặt và đồng bộ các script ops lên VPS (/opt/elingo).
# Bắt buộc phải chạy bằng user deploy.

set -euo pipefail

SRC="$(cd "$(dirname "$0")" && pwd)"
DEST=/opt/elingo

# 1. Kiểm tra user: cron và thư mục deploy phải thuộc user deploy
CURRENT_USER=$(id -un)
if [ "$CURRENT_USER" != "deploy" ]; then
  echo "Lỗi: Script này phải được chạy bởi user 'deploy' (user hiện tại: $CURRENT_USER)." >&2
  exit 1
fi

# 2. Kiểm tra các công cụ phụ thuộc cần thiết
for cmd in docker rclone flock python3 curl; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Lỗi: Lệnh '$cmd' chưa được cài đặt trên hệ thống." >&2
    exit 1
  fi
done

mkdir -p "$DEST/bot" "$DEST/logs" "$DEST/backups"

# 3. Luôn ghi đè script từ git và cấp quyền thực thi 755
install -m 755 "$SRC/backup.sh" "$SRC/notify.sh" "$SRC/ship_logs.sh" "$DEST/"
install -m 755 "$SRC/monitor.py" "$DEST/bot/monitor.py"

# Cài đặt file cấu hình logrotate (quyền 644)
install -m 644 "$SRC/logrotate.conf" "$DEST/logrotate.conf"

# Loại bỏ ký tự \r nếu file được checkout từ môi trường Windows
sed -i 's/\r$//' "$DEST"/*.sh "$DEST/bot"/*.py "$DEST/logrotate.conf" 2>/dev/null || true

# 4. Quản lý file .alert.env và luôn đặt quyền 600
if [ ! -f "$DEST/.alert.env" ]; then
  install -m 600 "$SRC/alert.env.example" "$DEST/.alert.env"
  sed -i 's/\r$//' "$DEST/.alert.env" 2>/dev/null || true
fi
chmod 600 "$DEST/.alert.env"

# 5. Lưu bản backup crontab trước khi cập nhật
CURRENT_CRON=""
CRON_ERR_TMP=$(mktemp)
if ! CURRENT_CRON=$(crontab -l 2>"$CRON_ERR_TMP"); then
  ERR_MSG=$(cat "$CRON_ERR_TMP")
  rm -f "$CRON_ERR_TMP"
  if echo "$ERR_MSG" | grep -qi "no crontab"; then
    CURRENT_CRON=""
  else
    echo "Lỗi khi đọc crontab hiện tại: $ERR_MSG" >&2
    exit 1
  fi
else
  rm -f "$CRON_ERR_TMP"
fi

echo "$CURRENT_CRON" > "$DEST/crontab.bak"

# 6. Cập nhật khối cron do script quản lý, bảo toàn các cron job khác của user deploy
BEGIN="# BEGIN elingo-ops"
END="# END elingo-ops"
{
  if [ -n "$CURRENT_CRON" ]; then
    echo "$CURRENT_CRON" \
      | sed "/^$BEGIN\$/,/^$END\$/d" \
      | grep -vE '/opt/elingo/(backup|ship_logs)\.sh|/opt/elingo/bot/monitor\.py|logrotate.*logrotate\.conf|^PATH=/usr/local/bin:/usr/bin:/bin$' \
      || true
  fi
  echo "$BEGIN"
  sed 's/\r$//' "$SRC/crontab.txt"
  echo "$END"
} | crontab -

echo "Đã đồng bộ ops scripts và cập nhật crontab thành công tại $DEST."
