#!/usr/bin/env python3
"""
add_cards.py — добавление карточек в БД через админский API бэка WelcomeTPU.

Как работает:
  1. Подписывает локальный админ-JWT (HS256) секретом из .secret/backend-oauth.properties
     (тот же формат payload, что в JwtService: sub, username, is_admin, iat, exp).
  2. POST'ит карточки из JSON-файла в POST /api/v1/admin/cards.
  3. Опционально загружает картинки карточек (POST /api/v1/admin/cards/{id}/images).

Использование:
  # карточки из файла
  ./add_cards.py cards.json

  # свой backend и свой user_id автора
  ./add_cards.py --base-url http://localhost:8080 --user-id 12345 cards.json

  # отладка: только показать JWT и тела запросов, ничего не слать
  ./add_cards.py --dry-run cards.json

  # stdin
  echo '[{...}]' | ./add_cards.py -

Формат cards.json — массив объектов, тело как у POST /api/v1/admin/cards:
[
  {
    "type": "FACT",
    "school_id": 1,
    "title": "ТПУ основан в 1896",
    "content": "Томский политех...",
    "content_format": "PLAIN",
    "published_at": "2025-01-15T10:00:00Z",     // null => черновик
    "sources": [{"title": "ТПУ", "url": "https://tpu.ru"}],
    "images": [{"path": "/path/to/img.jpg", "alt_text": "..."}],   // опционально
    "qr_details": {"target_url": "https://..."}  // детали по типу карточки
  }
]
"""

import argparse
import base64
import hashlib
import hmac
import json
import mimetypes
import os
import sys
import time
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SECRET_FILE = os.path.join(ROOT, ".secret", "backend-oauth.properties")
DEFAULT_BASE_URL = "http://localhost:8080"
JWT_TTL = 3600  # 1 час, как auth.jwt.ttl-seconds в application.properties


def read_jwt_secret() -> str:
    if not os.path.exists(SECRET_FILE):
        sys.exit(f"Не найден {SECRET_FILE}")
    with open(SECRET_FILE, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line.startswith("auth.jwt.secret="):
                secret = line.split("=", 1)[1].strip()
                if len(secret) < 32:
                    sys.exit("auth.jwt.secret короче 32 символов")
                return secret
    sys.exit(f"В {SECRET_FILE} нет auth.jwt.secret")


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def make_admin_jwt(secret: str, user_id: int, username: str = "card-script") -> str:
    now = int(time.time())
    header = b64url(b'{"alg":"HS256","typ":"JWT"}')
    payload = json.dumps(
        {"sub": str(user_id), "username": username, "is_admin": True,
         "iat": now, "exp": now + JWT_TTL},
        separators=(",", ":"), ensure_ascii=False,
    ).encode()
    payload_b64 = b64url(payload)
    signing_input = f"{header}.{payload_b64}".encode()
    sig = hmac.new(secret.encode(), signing_input, hashlib.sha256).digest()
    return f"{header}.{payload_b64}.{b64url(sig)}"


def request(base_url: str, method: str, path: str, token: str,
            body: dict | None = None, file_path: str | None = None,
            alt_text: str | None = None, dry_run: bool = False):
    url = base_url.rstrip("/") + path
    if dry_run:
        print(f"[dry-run] {method} {url}")
        if body:
            print(json.dumps(body, ensure_ascii=False, indent=2))
        return 201, {}

    if file_path:
        boundary = "----addcards" + hashlib.sha1(os.urandom(16)).hexdigest()[:12]
        mime = mimetypes.guess_type(file_path)[0] or "application/octet-stream"
        with open(file_path, "rb") as f:
            file_bytes = f.read()
        parts = [
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; "
            f"filename=\"{os.path.basename(file_path)}\"\r\n"
            f"Content-Type: {mime}\r\n\r\n".encode() + file_bytes + b"\r\n"
        ]
        if alt_text:
            parts.append(
                f"--{boundary}\r\nContent-Disposition: form-data; name=\"alt_text\"\r\n\r\n"
                f"{alt_text}\r\n".encode())
        parts.append(f"--{boundary}--\r\n".encode())
        data = b"".join(parts)
        headers = {
            "Cookie": f"access_token={token}",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        }
    else:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers = {
            "Cookie": f"access_token={token}",
            "Content-Type": "application/json; charset=utf-8",
        }

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read().decode("utf-8", "replace")
            return resp.status, (json.loads(raw) if raw.strip() else {})
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            detail = json.loads(raw)
        except json.JSONDecodeError:
            detail = raw
        return e.code, detail


def validate_card(card: dict, idx: int) -> None:
    for field in ("type", "title", "content"):
        if not card.get(field):
            sys.exit(f"Карточка #{idx}: отсутствует обязательное поле '{field}'")
    t = card["type"].upper()
    if t not in {"FACT", "EVENT", "STATISTIC", "PERSON", "MEDIA", "QR"}:
        sys.exit(f"Карточка #{idx}: недопустимый type '{card['type']}'")
    if t == "QR" and not (card.get("qr_details") or {}).get("target_url"):
        sys.exit(f"Карточка #{idx}: для QR обязателен qr_details.target_url")


def main() -> int:
    ap = argparse.ArgumentParser(description="Добавление карточек в БД бэка WelcomeTPU")
    ap.add_argument("file", help="JSON-файл с карточками (массив) или '-' для stdin")
    ap.add_argument("--base-url", default=os.environ.get("BASE_URL", DEFAULT_BASE_URL))
    ap.add_argument("--user-id", type=int, default=1, help="user_id автора карточки (для JWT sub)")
    ap.add_argument("--dry-run", action="store_true", help="не отправлять запросы")
    args = ap.parse_args()

    raw = sys.stdin.read() if args.file == "-" else open(args.file, encoding="utf-8").read()
    try:
        cards = json.loads(raw)
    except json.JSONDecodeError as e:
        sys.exit(f"Невалидный JSON: {e}")
    if isinstance(cards, dict):
        cards = [cards]

    token = make_admin_jwt(read_jwt_secret(), args.user_id)
    if args.dry_run:
        print(f"[dry-run] JWT: {token}\n")

    ok = fail = 0
    for i, card in enumerate(cards, 1):
        validate_card(card, i)
        images = card.pop("images", []) or []

        status, resp = request(args.base_url, "POST", "/api/v1/admin/cards",
                               token, body=card, dry_run=args.dry_run)
        if status == 201:
            card_id = resp.get("id", "?")
            print(f"[{i}/{len(cards)}] OK   {status} «{card['title']}» id={card_id}")
            ok += 1
            for img in images:
                s2, r2 = request(args.base_url, "POST",
                                 f"/api/v1/admin/cards/{card_id}/images", token,
                                 file_path=img["path"],
                                 alt_text=img.get("alt_text"), dry_run=args.dry_run)
                if s2 == 201:
                    print(f"        image OK «{os.path.basename(img['path'])}»")
                else:
                    print(f"        image FAIL {s2}: {r2}", file=sys.stderr)
        elif status == 409:
            print(f"[{i}/{len(cards)}] SKIP 409 дубликат «{card['title']}»: "
                  f"{resp.get('detail', resp) if isinstance(resp, dict) else resp}")
        else:
            print(f"[{i}/{len(cards)}] FAIL {status} «{card['title']}»: "
                  f"{json.dumps(resp, ensure_ascii=False) if isinstance(resp, dict) else resp}",
                  file=sys.stderr)
            fail += 1

    print(f"\nИтого: создано {ok}, ошибок {fail}, пропущено {len(cards) - ok - fail}")
    return 1 if fail else 0


if __name__ == "__main__":
    sys.exit(main())
