#!/usr/bin/env bash
# Gửi thông báo Telegram alert.
# Dùng: /opt/elingo/notify.sh "nội dung"
set -euo pipefail

# shellcheck disable=SC1091
[ -f /opt/elingo/.alert.env ] && source /opt/elingo/.alert.env || true

# Thoát an toàn nếu chưa cấu hình thông tin bot
if [ -z "${TG_TOKEN:-}" ] || [ -z "${TG_CHAT_ID:-}" ]; then
  exit 0
fi

# Cắt nội dung tối đa 4000 ký tự
MSG="${1:-}"
MSG="${MSG:0:4000}"
TEXT="[elingo-vps] $MSG"

# Nạp thông số và URL qua stdin của curl để không lộ token trong danh sách tiến trình ps
curl -s -m 10 -X POST --config - <<EOF >/dev/null || true
url = "https://api.telegram.org/bot${TG_TOKEN}/sendMessage"
data-urlencode = "chat_id=${TG_CHAT_ID}"
data-urlencode = "text=${TEXT}"
EOF
