# Документация backend Welcome TPU

## Сборка и запуск

```bash
mvn clean package          # сборка
mvn spring-boot:run        # запуск (требует запущенного compose: docker compose up -d)
```

- Java 25, Spring Boot 4.1, PostgreSQL 16 (docker compose), Flyway-миграции `V1..V4`.
- База: `welcomtpu/welcomtpu@localhost:5432`.

## Структура

| Пакет/каталог | Содержимое |
|---|---|
| `controller/` | Публичные: `CardsController`, `SchoolController`. Админские: `AdminCardsController` (CRUD карточек), `AdminCardsListController` (список с фильтрами), `AdminSchoolsController` (CRUD школ), `CardImagesController` (загрузка картинок) |
| `auth/` | OAuth ТПУ: `AuthController`, `TpuOAuthService`, `JwtService`, `AdminAuthFilter` |
| `model/`, `repository/`, `dto/` | JPA-сущности, репозитории, DTO |
| `db/migration/` | Flyway: V1 схема, V2/V3 сиды, V4 — `qr_details` (тип карточки QR) |
| `config/WebConfig.java` | Раздача загруженных файлов `/assets/**` → `./uploads/` |

## Типы карточек

`FACT`, `EVENT`, `STATISTIC`, `PERSON`, `MEDIA`, `QR`.

У `QR` обязательны `qr_details.target_url` (URL, зашиваемый в QR), опциональны `caption`, `source`.

## API

Полная спецификация — в файле `REST API для Welcome ТПУ.md`.

- Публичные: `GET /api/v1/schools`, `GET /api/v1/schools/{id}`, `GET /api/v1/cards` (постранично, seed-перемешивание, заголовок `X-Seed`), `GET /api/v1/cards/{id}`
- Админские (нужна JWT-cookie `access_token`, HttpOnly, срок 1 час):
  - `GET|POST /api/v1/admin/schools`, `PUT|DELETE /api/v1/admin/schools/{id}` — при удалении школы её карточки остаются как «ТПУ в целом»
  - `GET /api/v1/admin/cards` — фильтры `type`, `school_id`, `published`, `search`; сортировка `created_at|updated_at|title|published_at`
  - `POST /api/v1/admin/cards`, `PUT|DELETE /api/v1/admin/cards/{id}` — `unique_hash` (sha256 от type/school_id/title/content), дубликат → 409
  - `POST /api/v1/admin/cards/{id}/images` (multipart `file`, `alt_text`; jpeg/png/webp, ≤10 МБ, sha256-id), `DELETE .../images/{asset_id}`
  - `GET|POST /api/v1/auth/*` — см. ниже

## Аутентификация (единая авторизация ТПУ)

Схема: oauth.tpu.ru authorization code flow.

```
GET /api/v1/auth/login        → {"authorization_url": "https://oauth.tpu.ru/authorize?...&state=..."}
фронт открывает authorization_url
GET /api/v1/auth/callback?code&state → обмен code на access_token (GET /access_token),
  проверка админа, JWT (HS256, 1ч) в cookie, 302 на админку
GET /api/v1/auth/me           → {user_id, username, is_admin}
POST /api/v1/auth/logout      → 204, сброс cookie
```

- `state` одноразовый, TTL 10 минут (CSRF-защита).
- Проверка админа: по `auth.admin-user-ids` (user_id) или `auth.admin-usernames` (логин) — из `oauth.tpu.ru/check-token`.
- Все `/api/v1/admin/**` закрыты фильтром `AdminAuthFilter`: 401 — нет/просрочен токен, 403 — не админ.
- Документация ТПУ: https://devdocs.tpu.ru/books/edinaia-avtorizaciia-tpu

## Секреты

Лежат в `../.secret/` (вне репозитория, не коммитить):

| Файл | Содержимое |
|---|---|
| `backend-oauth.properties` | `oauth.tpu.client-id/client-secret`, `oauth.tpu.redirect-uri`, `auth.jwt.secret`, `auth.admin-user-ids`, `auth.admin-usernames` |
| `welcome-tpu-private.pem` / `-public.pem` | SSL-ключи приложения (для api.tpu.ru, подпись sig) |
| `welcome-tpu-oauth.txt`, `welcome-tpu-version-01.txt` | креды из дашборда api.tpu.ru |
| `localhost-https.p12` | self-signed серт для локального HTTPS (пароль devpass) |

Подключение: `spring.config.import=optional:file:../.secret/backend-oauth.properties`.

## Локальная разработка

1. `docker compose up -d` — Postgres.
2. Дашборд api.tpu.ru (МЕНЮ → Авторизация → Oauth2 Сервер): в доверенные redirect uri добавить `http://localhost:8080/api/v1/auth/callback`.
3. `mvn spring-boot:run`.

Запуск «как в проде» (HTTPS + Secure-cookie):

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8443 \
  --server.ssl.key-store=../.secret/localhost-https.p12 \
  --server.ssl.key-store-password=devpass --server.ssl.key-store-type=PKCS12 \
  --auth.cookie.secure=true"
```

Полный OAuth-флоу снаружи без доступа к прод-домену: туннель (`ngrok http 8080`), домен туннеля в доверенные redirect uri.

## Прод-настройки

- `AUTH_COOKIE_SECURE=true`
- `server.ssl.*` — реальный сертификат (обычно терминируется reverse-proxy)
- `OAUTH_SUCCESS_REDIRECT` — URL админки фронта
- `oauth.tpu.redirect-uri` — `https://<домен>/api/v1/auth/callback`
- `auth.admin-user-ids` — вместо/вместе с логинами

## Известные особенности

- `unique_hash` карточки считается от `type|school_id|title|content` — дубликаты дают 409.
- PUT карточки обновляет детали in-place (`@MapsId`-сущности нельзя пересоздавать).
- Ассеты дедуплицируются по sha256 содержимого; файлы хранятся в `./uploads/{id}.{ext}`.
