#!/usr/bin/env python3
"""
parse_cards_txt.py — конвертация backend/cards.txt в cards.json
для make_migration.py / add_cards.py.

Формат cards.txt — вертикальные таблицы:
  строка «Карточка», затем колонки-заголовки (каждая с "\t" в начале),
  затем карточки: строка с id («H-01 · ТПУ»), затем значения колонок.

Особенности:
- Типа HISTORY в БД нет → маппится в FACT.
- Все карточки в файле «· ТПУ» → school_id = null.
- Источники без реальных URL → заглушка https://tpu.ru + название из колонки source.
- imageUrl — плейсхолдеры («Открыть изображение») → изображения не заполняются.
- Пустые EVENT-карточки (прочерки) пропускаются.
- published_at = null → карточки загружаются черновиками (есть непроверенные).

Использование: python3 backend/scripts/parse_cards_txt.py backend/cards.txt > cards.json
"""

import json
import sys

GENERIC_URL = "https://tpu.ru"
PLACEHOLDERS = {
    "—", "-", "", "Открыть изображение", "Открыть изображение / ссылку",
    "Прямая ссылка на видеоплеер не подтверждена",
}
TYPE_MAP = {"HISTORY": "FACT"}


def clean(value) -> str | None:
    if value is None:
        return None
    value = value.strip()
    return None if value in PLACEHOLDERS else value


import re

ID_RE = re.compile(r"^\s*\w+-\d+\s*·")


def parse(path: str) -> list[dict]:
    lines = [l for l in open(path, encoding="utf-8-sig").read().splitlines()
             if l.strip()]  # пустые строки обрывают вертикальные таблицы — убираем

    cards: list[dict] = []
    i = 0
    while i < len(lines):
        if lines[i].strip() == "Карточка":
            # заголовки-колонки до первой строки-идентификатора карточки
            header = ["Карточка"]
            i += 1
            while i < len(lines) and lines[i].startswith("\t") \
                    and not ID_RE.match(lines[i]):
                header.append(lines[i].strip().rstrip("\r"))
                i += 1
            # карточки: каждая начинается со строки «X-NN · ТПУ» (с табом)
            values: list[str] = []

            def flush():
                if values:
                    data = dict(zip(header, (values + [""] * len(header))[:len(header)]))
                    if "·" in data.get("Карточка", ""):
                        card = build(data)
                        if card:
                            cards.append(card)
                values.clear()

            while i < len(lines) and lines[i].startswith("\t"):
                if ID_RE.match(lines[i]):
                    flush()
                values.append(lines[i].strip().rstrip("\r"))
                i += 1
            flush()
        else:
            i += 1
    return cards


def build(d: dict) -> dict | None:
    ctype = (d.get("type") or "").upper() or "STATISTIC"
    ctype = TYPE_MAP.get(ctype, ctype)
    if ctype not in {"FACT", "EVENT", "STATISTIC", "PERSON", "MEDIA", "QR"}:
        return None

    title = clean(d.get("title"))
    content = clean(d.get("text")) or clean(d.get("content")) or ""
    if not title:  # пустые карточки (E-01 и т.п.)
        return None

    src_title = clean(d.get("source"))
    card = {
        "type": ctype,
        "school_id": None,
        "title": title,
        "content": content or clean(d.get("bio")) or clean(d.get("description")) \
            or clean(d.get("caption")) or "",
        "content_format": "PLAIN",
        "published_at": None,
        "sources": [{"title": src_title, "url": GENERIC_URL}] if src_title else [],
    }

    if ctype == "STATISTIC":
        value = float(d["value"].replace(" ", "").replace("\u00a0", "").replace(",", "."))
        card["statistic_details"] = {
            "value": value,
            "unit": clean(d.get("unit / label")),
            "period": clean(d.get("period")),
            "source": src_title,
        }
    elif ctype == "PERSON":
        card["person_details"] = {
            "full_name": clean(d.get("full_name")),
            "bio": clean(d.get("bio")),
            "achievements": clean(d.get("achievements")),
        }
    elif ctype == "MEDIA":
        card["media_details"] = {
            "media_type": (d.get("media_type") or "PHOTO").upper(),
            "caption": clean(d.get("caption")),
            "description": clean(d.get("description")),
            "external_url": clean(d.get("external_url")),
        }
    elif ctype == "QR":
        card["qr_details"] = {
            "target_url": clean(d.get("target_url")) or GENERIC_URL,
            "caption": clean(d.get("caption")),
            "source": src_title,
        }
    elif ctype == "EVENT":
        details = {
            "start_date": clean(d.get("start_date")),
            "end_date": clean(d.get("end_date")),
            "start_time": clean(d.get("start_time")),
            "end_time": clean(d.get("end_time")),
            "location": clean(d.get("location")),
            "details_url": clean(d.get("details_url")),
        }
        card["event_details"] = {k: v for k, v in details.items() if v}
    return card


if __name__ == "__main__":
    cards = parse(sys.argv[1] if len(sys.argv) > 1 else "cards.txt")
    json.dump(cards, sys.stdout, ensure_ascii=False, indent=2)
    print(f"\n// {len(cards)} карточек", file=sys.stderr)
