#!/usr/bin/env python3
"""
Elingo monitor bot (chạy mỗi phút bằng cron).

- Đọc log JSON mới của service backend, gom các dòng level=ERROR và gửi Telegram
  kèm requestId + vài dòng stack trace. Cùng một lỗi không báo lại trong ERR_COOLDOWN.
- Theo dõi CPU/RAM của VPS: chỉ báo khi vượt ngưỡng liên tục SUSTAIN lần kiểm tra,
  nhắc lại mỗi REMIND_SEC nếu chưa hết, báo khi đã phục hồi.

Chỉ dùng thư viện chuẩn của Python 3.
Dùng: python3 monitor.py          (chạy bình thường)
      python3 monitor.py --test   (gửi 1 tin nhắn thử)
"""
import calendar
import json
import os
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request

BASE = os.environ.get("ELINGO_BASE", "/opt/elingo")
STATE_FILE = f"{BASE}/bot/state.json"
SERVICE = "backend"

CPU_LIMIT = 85          # %
RAM_LIMIT = 85          # % (tính theo MemAvailable)
SUSTAIN = 3             # số lần kiểm tra liên tiếp (~3 phút) mới báo
REMIND_SEC = 30 * 60    # nhắc lại nếu chưa hết
ERR_COOLDOWN = 10 * 60  # cùng một lỗi không báo lại trong 10 phút
MAX_GROUPS = 5          # tối đa số nhóm lỗi trong 1 tin nhắn

# "<timestamp docker> <nội dung dòng log>"
DOCKER_RE = re.compile(r"^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?Z\s?(.*)$")


def load_env(path):
    env = {}
    if not os.path.exists(path):
        return env
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                env[k.strip()] = v.strip().strip('"').strip("'")
    return env


ENV = load_env(f"{BASE}/.alert.env")


def notify(text):
    token = ENV.get("TG_TOKEN")
    chat_id = ENV.get("TG_CHAT_ID")
    if not token or not chat_id:
        return
    data = urllib.parse.urlencode(
        {"chat_id": chat_id, "text": ("[elingo-vps]\n" + text)[:3900]}
    ).encode()
    try:
        urllib.request.urlopen(
            f"https://api.telegram.org/bot{token}/sendMessage", data, timeout=10
        )
    except Exception as e:
        print("notify failed:", e)


def load_state():
    try:
        with open(STATE_FILE) as f:
            return json.load(f)
    except Exception:
        return {}


def save_state(state):
    tmp = STATE_FILE + ".tmp"
    with open(tmp, "w") as f:
        json.dump(state, f)
    os.replace(tmp, STATE_FILE)


# ---------------------------------------------------------------- CPU / RAM
def cpu_percent(state):
    """CPU dùng (%) của cả máy trong khoảng thời gian kể từ lần chạy trước."""
    with open("/proc/stat") as f:
        vals = list(map(int, f.readline().split()[1:]))
    total, idle = sum(vals), vals[3] + vals[4]  # idle + iowait
    prev = state.get("cpu_prev")
    state["cpu_prev"] = [total, idle]
    if not prev:
        return None
    dt, di = total - prev[0], idle - prev[1]
    return None if dt <= 0 else 100.0 * (dt - di) / dt


def ram_percent():
    m = {}
    with open("/proc/meminfo") as f:
        for line in f:
            k, v = line.split(":")
            m[k] = int(v.split()[0])
    return 100.0 * (1 - m["MemAvailable"] / m["MemTotal"])


def top_containers():
    try:
        r = subprocess.run(
            ["docker", "stats", "--no-stream", "--format",
             "{{.Name}}  CPU {{.CPUPerc}}  MEM {{.MemUsage}}"],
            capture_output=True, text=True, timeout=20,
        )
        return r.stdout.strip()
    except Exception:
        return ""


def check_metric(state, name, value, limit, now):
    if value is None:
        return
    s = state.setdefault("metrics", {}).setdefault(
        name, {"over": 0, "alerting": False, "last_alert": 0}
    )
    if value >= limit:
        s["over"] += 1
        if s["over"] >= SUSTAIN and (not s["alerting"] or now - s["last_alert"] >= REMIND_SEC):
            head = "🔴" if not s["alerting"] else "⏰ Vẫn chưa hết:"
            notify(
                f"{head} {name} {value:.0f}% (ngưỡng {limit}%, kéo dài ≥{SUSTAIN} phút)\n\n"
                + top_containers()
            )
            s["alerting"], s["last_alert"] = True, now
    else:
        s["over"] = 0
        if s["alerting"] and value < limit - 5:
            notify(f"🟢 {name} đã về {value:.0f}%")
            s["alerting"] = False


# ------------------------------------------------------------ ERROR trong log
def to_ns(sec_part, frac):
    base = calendar.timegm(time.strptime(sec_part, "%Y-%m-%dT%H:%M:%S"))
    return base * 10**9 + int(((frac or "") + "000000000")[:9])


def fetch_logs(cursor_ns):
    since = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(cursor_ns // 10**9))
    r = subprocess.run(
        ["docker", "compose", "logs", "--no-color", "--no-log-prefix", "--timestamps",
         "--since", since, SERVICE],
        cwd=BASE, capture_output=True, text=True, timeout=60,
    )
    if r.returncode != 0:
        raise RuntimeError(r.stderr[:300])
    return r.stdout.splitlines()


def parse_event(text):
    """Trả về dict nếu là một dòng log JSON, ngược lại None."""
    text = text.strip()
    if not text.startswith("{"):
        return None
    try:
        d = json.loads(text)
    except ValueError:
        return None
    if not isinstance(d, dict):
        return None
    trace = (d.get("stack_trace") or "").splitlines()
    short = trace[:3]
    causes = [l for l in trace if l.startswith("Caused by:")]
    if causes and causes[-1] not in short:
        short.append(causes[-1])
    return {
        "level": d.get("level", ""),
        "rid": d.get("requestId") or d.get("traceId") or "",
        "logger": d.get("logger_name", "?"),
        "msg": d.get("message", ""),
        "time": (d.get("@timestamp") or "")[:23].replace("T", " "),
        "trace": [l.strip()[:160] for l in short],
    }


def fingerprint(ev):
    return ev["logger"] + "|" + re.sub(r"\d+", "N", ev["msg"])[:120]


def check_errors(state, now):
    cursor = state.get("log_cursor")
    if cursor is None:  # lần chạy đầu: chỉ đặt mốc, không phát lại log cũ
        state["log_cursor"] = int(now * 10**9)
        return
    try:
        lines = fetch_logs(cursor)
    except Exception as e:
        print("fetch logs failed:", e)
        return

    events, newest = [], cursor
    for line in lines:
        m = DOCKER_RE.match(line)
        if not m:
            continue
        ts = to_ns(m.group(1), m.group(2))
        if ts <= cursor:
            continue
        newest = max(newest, ts)
        ev = parse_event(m.group(3))
        if ev:
            events.append(ev)
    state["log_cursor"] = newest

    groups = {}
    for ev in events:
        if ev["level"] != "ERROR":
            continue
        g = groups.setdefault(fingerprint(ev), {"first": ev, "count": 0, "rids": []})
        g["count"] += 1
        if ev["rid"] and ev["rid"] not in g["rids"] and len(g["rids"]) < 3:
            g["rids"].append(ev["rid"])

    seen = state.setdefault("errors", {})
    for k in [k for k, v in seen.items() if now - v["last"] > 86400]:
        del seen[k]

    parts = []
    for fp, g in groups.items():
        rec = seen.setdefault(fp, {"last": 0, "muted": 0})
        if now - rec["last"] < ERR_COOLDOWN:
            rec["muted"] += g["count"]
            continue
        ev = g["first"]
        extra = f" (+{rec['muted']} lần bị gộp trước đó)" if rec["muted"] else ""
        rid = ", ".join(g["rids"]) if g["rids"] else "(không có - job nền)"
        block = (
            f"🔴 ERROR x{g['count']}{extra}  {ev['time']}\n"
            f"req: {rid}\n{ev['logger']} - {ev['msg'][:300]}"
        )
        if ev["trace"]:
            block += "\n" + "\n".join(ev["trace"])
        parts.append(block)
        rec["last"], rec["muted"] = now, 0

    if parts:
        more = (
            f"\n\n… và {len(parts) - MAX_GROUPS} nhóm lỗi khác"
            if len(parts) > MAX_GROUPS else ""
        )
        notify("\n\n".join(parts[:MAX_GROUPS]) + more)


def main():
    if len(sys.argv) > 1 and sys.argv[1] == "--test":
        notify("✅ Bot hoạt động bình thường")
        return
    state = load_state()
    now = time.time()
    check_metric(state, "CPU", cpu_percent(state), CPU_LIMIT, now)
    check_metric(state, "RAM", ram_percent(), RAM_LIMIT, now)
    check_errors(state, now)
    save_state(state)


if __name__ == "__main__":
    main()
