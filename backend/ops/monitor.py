#!/usr/bin/env python3
"""
Elingo monitor bot (chạy mỗi phút bằng cron).

- Đọc log JSON mới của service backend, gom các dòng level=ERROR và gửi Telegram
  kèm requestId + vài dòng stack trace. Cùng một lỗi không báo lại trong ERR_COOLDOWN.
- Theo dõi CPU, RAM, DISK của VPS: chỉ báo khi vượt ngưỡng liên tục SUSTAIN lần kiểm tra,
  nhắc lại mỗi REMIND_SEC nếu chưa hết, báo khi đã phục hồi.
- Theo dõi trạng thái hoạt động của các container (SERVICES).
- Theo dõi độ tươi của bản backup MySQL trong backups/.

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
SERVICES = ["backend", "nginx", "mysql", "redis"]

CPU_LIMIT = 85          # %
RAM_LIMIT = 85          # % (tính theo MemAvailable)
DISK_LIMIT = 85         # % (tính theo phân vùng "/")
SUSTAIN = 3             # số lần kiểm tra liên tiếp (~3 phút) mới báo
REMIND_SEC = 30 * 60    # nhắc lại nếu chưa hết (30 phút)
ERR_COOLDOWN = 10 * 60  # cùng một lỗi không báo lại trong 10 phút
MAX_GROUPS = 5          # tối đa số nhóm lỗi gửi block đầy đủ trong 1 tin nhắn

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
    """Gửi tin nhắn Telegram, chia tối đa 3 đoạn <= 3900 ký tự. Trả về True/False."""
    token = ENV.get("TG_TOKEN")
    chat_id = ENV.get("TG_CHAT_ID")
    if not token or not chat_id:
        return True

    full_text = f"[elingo-vps]\n{text}".strip()
    chunks = []
    limit = 3900
    remaining = full_text

    while remaining and len(chunks) < 3:
        if len(remaining) <= limit:
            chunks.append(remaining)
            break
        # Nếu đã đạt đoạn thứ 3 (đoạn cuối cùng), cắt cứng tại limit
        if len(chunks) == 2:
            chunks.append(remaining[:limit].strip())
            break
        split_pos = remaining.rfind("\n", 0, limit)
        if split_pos <= 0:
            split_pos = limit
        chunks.append(remaining[:split_pos].strip())
        remaining = remaining[split_pos:].strip()

    all_ok = True
    for chunk in chunks:
        if not chunk:
            continue
        data = urllib.parse.urlencode({"chat_id": chat_id, "text": chunk}).encode("utf-8")
        req = urllib.request.Request(
            f"https://api.telegram.org/bot{token}/sendMessage",
            data=data,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                if resp.status != 200:
                    all_ok = False
        except Exception as e:
            print(f"notify failed: {e}", file=sys.stdout)
            all_ok = False
    return all_ok


def load_state():
    if not os.path.exists(STATE_FILE):
        return {}
    try:
        with open(STATE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        print(f"Cảnh báo: File state {STATE_FILE} bị hỏng hoặc không đọc được ({e}), sử dụng state mới.", file=sys.stdout)
        return {}


def save_state(state):
    tmp = STATE_FILE + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(state, f)
    os.replace(tmp, STATE_FILE)


# ---------------------------------------------------------------- CPU / RAM / DISK
def cpu_percent(state):
    """CPU dùng (%) của cả máy trong khoảng thời gian kể từ lần chạy trước."""
    try:
        with open("/proc/stat") as f:
            vals = list(map(int, f.readline().split()[1:]))
    except Exception:
        return None
    total, idle = sum(vals), vals[3] + vals[4]  # idle + iowait
    prev = state.get("cpu_prev")
    state["cpu_prev"] = [total, idle]
    if not prev:
        return None
    dt, di = total - prev[0], idle - prev[1]
    return None if dt <= 0 else 100.0 * (dt - di) / dt


def ram_percent():
    m = {}
    try:
        with open("/proc/meminfo") as f:
            for line in f:
                k, v = line.split(":")
                m[k] = int(v.split()[0])
        return 100.0 * (1 - m["MemAvailable"] / m["MemTotal"])
    except Exception:
        return None


def disk_percent():
    """Tỷ lệ sử dụng dung lượng phân vùng root (/) theo phần trăm."""
    try:
        st = os.statvfs("/")
        total = st.f_blocks * st.f_frsize
        avail = st.f_bavail * st.f_frsize
        if total <= 0:
            return None
        used = total - avail
        return 100.0 * used / total
    except Exception:
        return None


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
            extra = top_containers() if name in ("CPU", "RAM") else ""
            text = f"{head} {name} {value:.0f}% (ngưỡng {limit}%, kéo dài ≥{SUSTAIN} phút)"
            if extra:
                text += f"\n\n{extra}"
            if notify(text):
                s["alerting"] = True
                s["last_alert"] = now
    else:
        s["over"] = 0
        if s["alerting"] and value < limit - 5:
            if notify(f"🟢 {name} đã về {value:.0f}%"):
                s["alerting"] = False


# ------------------------------------------------------------ Containers & Backup check
def check_containers(state, now):
    """Kiểm tra xem tất cả container trong SERVICES có đang chạy không."""
    try:
        r = subprocess.run(
            ["docker", "compose", "ps", "--status", "running", "--services"],
            cwd=BASE, capture_output=True, text=True, timeout=20,
        )
        if r.returncode != 0:
            return
        running = set(line.strip() for line in r.stdout.splitlines() if line.strip())
    except Exception as e:
        print(f"check_containers failed: {e}", file=sys.stdout)
        return

    down = [s for s in SERVICES if s not in running]
    if not down:
        return

    fp = "containers_down:" + ",".join(sorted(down))
    seen = state.setdefault("container_alerts", {})
    rec = seen.setdefault(fp, {"last": 0, "muted": 0})
    if now - rec["last"] < ERR_COOLDOWN:
        rec["muted"] += 1
        return

    extra = f" (+{rec['muted']} lần bị gộp trước đó)" if rec["muted"] else ""
    text = f"🔴 Container không ở trạng thái running{extra}: {', '.join(down)}"
    if notify(text):
        rec["last"] = now
        rec["muted"] = 0


def check_backup_age(state, now):
    """Cảnh báo nếu bản backup MySQL mới nhất trong backups/ quá 90 phút tuổi."""
    backup_dir = os.path.join(BASE, "backups")
    if not os.path.isdir(backup_dir):
        return

    newest_mtime = None
    try:
        # Quét kiểm tra file .sql.gz (hỗ trợ cả cấu trúc phẳng lẫn thư mục con)
        for root, _dirs, files in os.walk(backup_dir):
            for fname in files:
                if fname.endswith(".sql.gz"):
                    fpath = os.path.join(root, fname)
                    try:
                        mtime = os.path.getmtime(fpath)
                        if newest_mtime is None or mtime > newest_mtime:
                            newest_mtime = mtime
                    except OSError:
                        continue
    except Exception as e:
        print(f"check_backup_age failed: {e}", file=sys.stdout)
        return

    fp = "backup_age_alert"
    seen = state.setdefault("backup_alerts", {})
    rec = seen.setdefault(fp, {"last": 0, "muted": 0})
    if now - rec["last"] < ERR_COOLDOWN:
        rec["muted"] += 1
        return

    msg = None
    if newest_mtime is None:
        extra = f" (+{rec['muted']} lần bị gộp)" if rec["muted"] else ""
        msg = f"🔴 Cảnh báo backup{extra}: Không tìm thấy file backup .sql.gz nào trong {backup_dir}"
    elif (now - newest_mtime) > 90 * 60:
        age_min = int((now - newest_mtime) / 60)
        extra = f" (+{rec['muted']} lần bị gộp)" if rec["muted"] else ""
        msg = f"🔴 Cảnh báo backup{extra}: Bản backup MySQL mới nhất đã {age_min} phút tuổi (vượt ngưỡng 90 phút)"

    if msg:
        if notify(msg):
            rec["last"] = now
            rec["muted"] = 0


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
    causes = [line for line in trace if line.startswith("Caused by:")]
    if causes and causes[-1] not in short:
        short.append(causes[-1])
    return {
        "level": d.get("level", ""),
        "rid": d.get("requestId") or d.get("traceId") or "",
        "logger": d.get("logger_name", "?"),
        "msg": d.get("message", ""),
        "time": (d.get("@timestamp") or "")[:23].replace("T", " "),
        "trace": [line.strip()[:160] for line in short],
    }


def fingerprint(ev):
    """Tạo khóa phân nhóm lỗi: chuẩn hóa UUID, chuỗi hex dài và các số thành N."""
    msg = ev.get("msg", "")
    # Chuẩn hóa UUID
    msg = re.sub(r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "N", msg)
    # Chuẩn hóa chuỗi hex dài (từ 8 ký tự hex trở lên)
    msg = re.sub(r"\b[0-9a-fA-F]{8,}\b", "N", msg)
    # Chuẩn hóa số
    msg = re.sub(r"\d+", "N", msg)
    logger = ev.get("logger", "?")
    return f"{logger}|{msg[:120]}"


def check_errors(state, now):
    cursor = state.get("log_cursor")
    if cursor is None:  # lần chạy đầu: chỉ đặt mốc, không phát lại log cũ
        state["log_cursor"] = int(now * 10**9)
        return
    try:
        lines = fetch_logs(cursor)
    except Exception as e:
        print(f"fetch logs failed: {e}", file=sys.stdout)
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

    groups = {}
    for ev in events:
        if ev["level"] != "ERROR":
            continue
        g = groups.setdefault(fingerprint(ev), {"first": ev, "count": 0, "rids": []})
        g["count"] += 1
        if ev["rid"] and ev["rid"] not in g["rids"] and len(g["rids"]) < 3:
            g["rids"].append(ev["rid"])

    seen = state.setdefault("errors", {})
    for k in [k for k, v in seen.items() if now - v.get("last", 0) > 86400]:
        del seen[k]

    active_groups = []
    for fp, g in groups.items():
        rec = seen.setdefault(fp, {"last": 0, "muted": 0})
        if now - rec["last"] < ERR_COOLDOWN:
            rec["muted"] += g["count"]
        else:
            active_groups.append((fp, g, rec))

    # Nếu không có nhóm nào cần báo
    if not active_groups:
        state["log_cursor"] = newest
        state.pop("error_notify_failed_at", None)
        return

    full_blocks = []
    summary_lines = []
    for idx, (fp, g, rec) in enumerate(active_groups):
        ev = g["first"]
        extra = f" (+{rec['muted']} lần bị gộp trước đó)" if rec["muted"] else ""
        if idx < MAX_GROUPS:
            rid = ", ".join(g["rids"]) if g["rids"] else "(không có - job nền)"
            block = (
                f"🔴 ERROR x{g['count']}{extra}  {ev['time']}\n"
                f"req: {rid}\n{ev['logger']} - {ev['msg'][:200]}"
            )
            if ev["trace"]:
                block += "\n" + "\n".join(ev["trace"][:4])
            full_blocks.append(block)
        else:
            msg_short = ev["msg"][:80].replace("\n", " ")
            summary_lines.append(f"• x{g['count']} {ev['logger']} - {msg_short}")

    parts = []
    if full_blocks:
        parts.append("\n\n".join(full_blocks))
    if summary_lines:
        parts.append("\n".join(summary_lines))
    text = "\n\n".join(parts)

    send_ok = notify(text)
    if send_ok:
        for fp, g, rec in active_groups:
            rec["last"] = now
            rec["muted"] = 0
        state["log_cursor"] = newest
        state.pop("error_notify_failed_at", None)
    else:
        # Nếu gửi lỗi, giữ cursor để thử lại, trừ khi đã quá 30 phút
        failed_at = state.setdefault("error_notify_failed_at", now)
        if now - failed_at >= 30 * 60:
            state["log_cursor"] = newest
            state.pop("error_notify_failed_at", None)


def main():
    if len(sys.argv) > 1 and sys.argv[1] == "--test":
        notify("✅ Bot hoạt động bình thường")
        return
    state = load_state()
    now = time.time()
    check_metric(state, "CPU", cpu_percent(state), CPU_LIMIT, now)
    check_metric(state, "RAM", ram_percent(), RAM_LIMIT, now)
    check_metric(state, "DISK", disk_percent(), DISK_LIMIT, now)
    check_containers(state, now)
    check_backup_age(state, now)
    check_errors(state, now)
    save_state(state)


if __name__ == "__main__":
    main()
