#!/usr/bin/env python3
"""
make_migration.py — генерация Flyway-миграции с карточками из cards.json.

Создаёт файл src/main/resources/db/migration/V<N>__add_cards_<slug>.sql
с INSERT'ами в cards, card_sources и таблицы деталей (event/statistic/person/
media/qr). UUID детерминированные (UUIDv5 от заголовка карточки), unique_hash
считается так же, как в AdminCardsController.computeHash: sha256("TYPE|school|title|content").
Все INSERT идемпотентные (WHERE NOT EXISTS) — миграцию можно смело перегонять.

Использование:
  python3 backend/scripts/make_migration.py backend/scripts/cards.example.json
  python3 backend/scripts/make_migration.py cards.json --version 5 --author 10001 --dry-run
"""

import argparse
import hashlib
import json
import os
import re
import sys
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MIGRATIONS_DIR = os.path.join(ROOT, "backend", "src", "main", "resources", "db", "migration")
# фиксированный namespace для UUIDv5 карточек
NAMESPACE = uuid.UUID("6f5f2a7e-8f3c-4a2a-9b1e-0c1d2e3f4a5b")

DETAIL_TABLES = {
    "EVENT": "event_details",
    "STATISTIC": "statistic_details",
    "PERSON": "person_details",
    "MEDIA": "media_details",
    "QR": "qr_details",
}

# json-поле -> колонка таблицы деталей
COLUMNS = {
    "event_details": {
        "start_date": "start_date", "end_date": "end_date",
        "start_time": "start_time", "end_time": "end_time",
        "location": "location", "details_url": "details_url",
    },
    "statistic_details": {
        "value": "value", "unit": "unit", "period": "period", "source": "source",
    },
    "person_details": {
        "full_name": "full_name", "photo_asset_id": "photo_asset_id",
        "bio": "bio", "achievements": "achievements",
    },
    "media_details": {
        "media_type": "media_type", "asset_id": "asset_id",
        "external_url": "external_url", "caption": "caption",
        "description": "description", "source": "source",
    },
    "qr_details": {
        "target_url": "target_url", "caption": "caption", "source": "source",
    },
}


def next_version() -> int:
    versions = [0]
    for name in os.listdir(MIGRATIONS_DIR):
        m = re.match(r"V(\d+)__", name)
        if m:
            versions.append(int(m.group(1)))
    return max(versions) + 1


def sql_str(value) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def unique_hash(card: dict) -> str:
    school = "" if card.get("school_id") is None else str(card["school_id"])
    data = f"{card['type'].upper()}|{school}|{card['title'].strip()}|{card['content']}"
    return hashlib.sha256(data.encode("utf-8")).hexdigest()


def card_uuid(card: dict) -> str:
    return str(uuid.uuid5(NAMESPACE, unique_hash(card)))


def slugify(text: str) -> str:
    table = {
        'а': 'a', 'б': 'b', 'в': 'v', 'г': 'g', 'д': 'd', 'е': 'e', 'ё': 'e',
        'ж': 'zh', 'з': 'z', 'и': 'i', 'й': 'j', 'к': 'k', 'л': 'l', 'м': 'm',
        'н': 'n', 'о': 'o', 'п': 'p', 'р': 'r', 'с': 's', 'т': 't', 'у': 'u',
        'ф': 'f', 'х': 'h', 'ц': 'c', 'ч': 'ch', 'ш': 'sh', 'щ': 'sch',
        'ъ': '', 'ы': 'y', 'ь': '', 'э': 'e', 'ю': 'yu', 'я': 'ya',
    }
    s = text.lower()
    s = ''.join(table.get(ch, ch) for ch in s)
    s = re.sub(r"[^a-z0-9_]+", "_", s).strip("_")
    return s[:40] or "cards"


def validate_card(card: dict, idx: int) -> None:
    for field in ("type", "title", "content"):
        if not card.get(field):
            sys.exit(f"Карточка #{idx}: отсутствует обязательное поле '{field}'")
    t = card["type"].upper()
    if t not in DETAIL_TABLES and t != "FACT":
        sys.exit(f"Карточка #{idx}: недопустимый type '{card['type']}'")
    if t == "QR" and not (card.get("qr_details") or {}).get("target_url"):
        sys.exit(f"Карточка #{idx}: для QR обязателен qr_details.target_url")


def generate_sql(cards: list[dict], author: int) -> str:
    out = ["-- Автогенерировано scripts/make_migration.py — новые карточки",
           "-- id и unique_hash детерминированы (повторный запуск даёт тот же файл)", ""]

    # cards
    out.append("INSERT INTO cards (id, type, school_id, title, content, content_format, "
               "published_at, unique_hash, created_by_user_id) VALUES")
    rows = []
    for card in cards:
        rows.append(
            f"('{card_uuid(card)}', '{card['type'].upper()}', "
            f"{sql_str(card.get('school_id'))},\n"
            f" {sql_str(card['title'])},\n"
            f" {sql_str(card['content'])},\n"
            f" '{(card.get('content_format') or 'PLAIN').upper()}', "
            f"{sql_str(card.get('published_at'))}, "
            f"'{unique_hash(card)}', {author})"
        )
    out.append(",\n".join(rows) + "\nON CONFLICT (unique_hash) DO NOTHING;\n")

    # details
    for card in cards:
        t = card["type"].upper()
        table = DETAIL_TABLES.get(t)
        details = card.get(t.lower() + "_details")
        if not table or not details:
            continue
        cols = COLUMNS[t.lower() + "_details"]
        used = [(cols[k], v) for k, v in details.items() if v is not None]
        col_list = ", ".join(["card_id"] + [c for c, _ in used])
        val_list = ", ".join(["'" + card_uuid(card) + "'"] + [sql_str(v) for _, v in used])
        out.append(f"INSERT INTO {table} ({col_list}) VALUES ({val_list})\n"
                   f"ON CONFLICT (card_id) DO NOTHING;\n")

    # sources
    src_rows = []
    for card in cards:
        for s in card.get("sources") or []:
            src_rows.append(f"('{card_uuid(card)}', {sql_str(s.get('title'))}, {sql_str(s['url'])})")
    if src_rows:
        out.append("INSERT INTO card_sources (card_id, title, url) VALUES\n"
                   + ",\n".join(src_rows) + ";\n")

    # images (только привязка существующих ассетов по id)
    link_rows = []
    for card in cards:
        for aid in card.get("image_ids") or []:
            link_rows.append(f"('{card_uuid(card)}', '{aid}')")
    if link_rows:
        out.append("INSERT INTO card_assets (card_id, asset_id) VALUES\n"
                   + ",\n".join(link_rows) + "\nON CONFLICT DO NOTHING;\n")

    return "\n".join(out)


def main() -> int:
    ap = argparse.ArgumentParser(description="Генерация Flyway-миграции с карточками")
    ap.add_argument("file", help="JSON-файл с карточками (массив) или '-' для stdin")
    ap.add_argument("--version", type=int, default=None, help="номер миграции (по умолчанию — следующий)")
    ap.add_argument("--author", type=int, default=10001, help="created_by_user_id")
    ap.add_argument("--name", default=None, help="имя миграции (slug), по умолчанию из первого title")
    ap.add_argument("--dry-run", action="store_true", help="вывести SQL в stdout, не писать файл")
    args = ap.parse_args()

    raw = sys.stdin.read() if args.file == "-" else open(args.file, encoding="utf-8").read()
    cards = json.loads(raw)
    if isinstance(cards, dict):
        cards = [cards]
    for i, card in enumerate(cards, 1):
        validate_card(card, i)

    version = args.version or next_version()
    name = args.name or slugify(cards[0]["title"])
    sql = generate_sql(cards, args.author)

    if args.dry_run:
        print(sql)
        return 0

    os.makedirs(MIGRATIONS_DIR, exist_ok=True)
    path = os.path.join(MIGRATIONS_DIR, f"V{version}__add_cards_{name}.sql")
    if os.path.exists(path):
        sys.exit(f"Файл {path} уже существует — укажите --version/--name")
    with open(path, "w", encoding="utf-8") as f:
        f.write(sql)
    print(f"Создано: {path} ({len(cards)} карточек)")
    print("Миграция применится автоматически при следующем запуске бэка "
          "(Flyway) или вручную: docker compose exec db psql -U welcomtpu -d welcomtpu -f -")
    return 0


if __name__ == "__main__":
    sys.exit(main())
