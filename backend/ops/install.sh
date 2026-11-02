#!/usr/bin/env bash
set -euo pipefail
SRC="$(cd "$(dirname "$0")" && pwd)"
DEST=/opt/elingo

mkdir -p "$DEST/bot" "$DEST/logs" "$DEST/backups"

# luôn ghi đè script từ git và cấp quyền 755
install -m 755 "$SRC/backup.sh" "$SRC/notify.sh" "$SRC/ship_logs.sh" "$DEST/"
install -m 755 "$SRC/monitor.py" "$DEST/bot/monitor.py"

# loại bỏ ký tự \r nếu file được checkout từ môi trường Windows
sed -i 's/\r$//' "$DEST"/*.sh "$DEST/bot"/*.py 2>/dev/null || true

# chỉ tạo file .alert.env mẫu nếu chưa tồn tại
if [ ! -f "$DEST/.alert.env" ]; then
  install -m 600 "$SRC/alert.env.example" "$DEST/.alert.env"
  sed -i 's/\r$//' "$DEST/.alert.env" 2>/dev/null || true
fi

# cập nhật khối cron do script quản lý, giữ nguyên các dòng cron khác
BEGIN="# BEGIN elingo-ops"
END="# END elingo-ops"
{
  crontab -l 2>/dev/null \
    | sed "/^$BEGIN\$/,/^$END\$/d" \
    | grep -vE '/opt/elingo/(backup|ship_logs)\.sh|/opt/elingo/bot/monitor\.py|^PATH=/usr/local/bin:/usr/bin:/bin$' \
    || true
  echo "$BEGIN"
  sed 's/\r$//' "$SRC/crontab.txt"
  echo "$END"
} | crontab -

echo "Đã đồng bộ ops scripts và cập nhật crontab thành công tại $DEST."