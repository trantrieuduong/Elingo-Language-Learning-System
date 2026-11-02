#!/usr/bin/env bash
# Dùng: /opt/elingo/notify.sh "nội dung"
[ -f /opt/elingo/.alert.env ] && source /opt/elingo/.alert.env || true

if [ -z "${TG_TOKEN:-}" ] || [ -z "${TG_CHAT_ID:-}" ]; then
  exit 0
fi

curl -s -m 10 -X POST "https://api.telegram.org/bot${TG_TOKEN}/sendMessage" \
  --data-urlencode "chat_id=${TG_CHAT_ID}" \
  --data-urlencode "text=[elingo-vps] $1" >/dev/null || true
