# REST API для Welcome ТПУ

## Список эндпоинтов

| Метод    | Путь                                         | Доступ    | Назначение                          |
| -------- | -------------------------------------------- | --------- | ----------------------------------- |
| `GET`    | `/api/v1/auth/login`                         | Публичный | URL OAuth-редиректа                 |
| `GET`    | `/api/v1/auth/callback`                      | Публичный | OAuth callback (устанавливает куки) |
| `GET`    | `/api/v1/auth/me`                            | Админ     | Текущий пользователь                |
| `POST`   | `/api/v1/auth/logout`                        | Админ     | Выход                               |
| `GET`    | `/api/v1/schools`                            | Публичный | Список школ                         |
| `GET`    | `/api/v1/schools/{id}`                       | Публичный | Данные школы                        |
| `GET`    | `/api/v1/cards`                              | Публичный | Подборка карточек                   |
| `GET`    | `/api/v1/cards/{id}`                         | Публичный | Данные карточки                     |
| `GET`    | `/api/v1/admin/schools`                      | Админ     | Список школ                         |
| `POST`   | `/api/v1/admin/schools`                      | Админ     | Создать школу                       |
| `PUT`    | `/api/v1/admin/schools/{id}`                 | Админ     | Обновить данные школы               |
| `DELETE` | `/api/v1/admin/schools/{id}`                 | Админ     | Удалить школу (каскад)              |
| `GET`    | `/api/v1/admin/cards`                        | Админ     | Список карточек                     |
| `POST`   | `/api/v1/admin/cards`                        | Админ     | Создать новую карточку              |
| `PUT`    | `/api/v1/admin/cards/{id}`                   | Админ     | Обновить данные карточки            |
| `DELETE` | `/api/v1/admin/cards/{id}`                   | Админ     | Удалить карточку                    |
| `POST`   | `/api/v1/admin/cards/{id}/images`            | Админ     | Добавить картинку для карточки      |
| `DELETE` | `/api/v1/admin/cards/{id}/images/{asset_id}` | Админ     | Удалить картинку карточки           |

## 1. Общие сведения

### 1.1. Коды ответов

| Код                     | Назначение                               |
| ----------------------- | ---------------------------------------- |
| `200 OK`                | Успех                                    |
| `201 Created`           | Создание сущности                        |
| `204 No Content`        | Успех без тела (удаление, logout)        |
| `302 Found`             | Редирект (только OAuth callback)         |
| `400 Bad Request`       | Ошибка валидации                         |
| `401 Unauthorized`      | Нет/просрочен токен                      |
| `403 Forbidden`         | Не админ                                 |
| `404 Not Found`         | Ресурс не найден                         |
| `409 Conflict`          | Дубликат (`unique_hash`, `schools.name`) |
| `413 Payload Too Large` | Файл > 10 МБ                             |

### 1.2. Формат ошибок

Spring Boot ProblemDetail (RFC 7807)



**Пример:**

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Поле 'title' не может быть пустым",
  "instance": "/api/v1/admin/cards",
  "errors": [
    { "field": "title", "message": "must not be blank" },
    { "field": "event_details.start_date", "message": "must be a future date" }
  ]
}
```

Поле `errors[]` присутствует только для `400`.

---

## 2. Аутентификация и авторизация

### 2.1. Модель доступа

В MVP две роли:

- **Пользователь**  — доступ к публичным эндпоинтам без авторизации.
- **Администратор** — доступ к админским эндпоинтам после OAuth-входа.

**Правила:**

- Все пути `/api/v1/admin/**` — только для администраторов.
- Все остальные пути (`/api/v1/schools/**`, `/api/v1/cards/**`, `/api/v1/auth/**`, `/assets/**`, `/swagger-ui/**`, `/v3/api-docs/**`) — публичные.
- Токен передаётся в **`HttpOnly` cookie** `access_token`. На клиенте не хранится и не читается JS.
- Схема токена — **JWT**, подпись **HMAC-SHA256**, срок жизни **1 час**, без `refresh` (при истечении — повторный OAuth-вход).

### 2.2. OAuth-флоу через `oauth.tpu.ru`

```
┌────────┐   1. GET /auth/login   ┌────────┐
│Frontend│ ─────────────────────► │Backend │
│        │ ◄───────────────────── │        │
└────────┘   authorization_url    └────────┘
    │ 2. window.location.href = authorization_url
    ▼
┌────────────────┐
│oauth.tpu.ru    │  пользователь вводит корпоративные данные
└────────────────┘
    │ 3. 302 → /auth/callback?code=...&state=...
    ▼
┌────────┐  4. GET /auth/callback ┌────────┐
│Frontend│ ◄──────────────────────│Backend │
└────────┘  302 + Set-Cookie JWT  └────────┘
                                     │
                                     │ 5. POST access_token, GET user
                                     ▼
                                  oauth.tpu.ru
```

**Шаги:**

1. Фронт вызывает `GET /auth/login` и получает `authorization_url`.
2. Фронт переходит на `oauth.tpu.ru` с помощью `authorization_url`.
3. Пользователь вводит корпоративные данные, OAuth редиректит на `/auth/callback`.
4. Бэкенд обменивает `code` на `access_token`, получает `user_id`, проверяет, что он в `admin_users`.
5. Ставит cookie `access_token` (JWT) и редиректит на админ панель.

### 2.3. JWT

**Структура:**

```json
{
  "sub": "12345", // user_id
  "username": "ivanov",
  "is_admin": true,
  "iat": 1717000000,
  "exp": 1717003600
}
```

**Передача:** cookie `access_token`:

```
Set-Cookie: access_token=<jwt>; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=3600
```

**Параметры безопасности:**

- `HttpOnly` — недоступно из JS.
- `Secure` — только по HTTPS.
- `SameSite=Lax` — защита от CSRF на GET-запросах; для state-changing методов (`POST`/`PUT`/`DELETE`) фронт добавляет заголовок `X-Requested-With: XMLHttpRequest`, который проверяется на бэке (или используем `SameSite=Strict` — решим при реализации).
- `Max-Age=3600` — час.

### 2.6. Эндпоинты аутентификации

#### `GET /api/v1/auth/login`

**Доступ:** публичный. 
**Ответ `200 OK`:**

```json
{
  "authorization_url": "https://oauth.tpu.ru/authorize?client_id=welcome-tpu&redirect_uri=https%3A%2F%2Fwelcome.tpu.ru%2Fapi%2Fv1%2Fauth%2Fcallback&response_type=code&state=abc123&scope=openid%20profile%20email"
}
```

---

#### `GET /api/v1/auth/callback?code={code}&state={state}`

**Доступ:** публичный. 
**Ответ `302 Found`:**

```
Location: https://welcome.tpu.ru/admin
Set-Cookie: access_token=<jwt>; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=3600
```

**Ошибки:** `403 Forbidden` невалидный `state` или `user_id` не в `admin_users`.  

---

#### `GET /api/v1/auth/me`

**Доступ:** админ. 
**Ответ `200 OK`:**

```json
{
  "user_id": 12345,
  "username": "ivanov",
  "email": "ivanov@tpu.ru",
  "is_admin": true
}
```

**Ошибки:** `401` — нет/просрочена cookie. 

---

#### `POST /api/v1/auth/logout`

**Доступ:** админ. 
**Ответ `204 No Content`:**

```
Set-Cookie: access_token=; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=0
```

---

## 3. Публичные эндпоинты

> Все эндпоинты этого раздела доступны без авторизации.

### 3.1. Школы

#### `GET /api/v1/schools`

**Доступ:** публичный. 
**Назначение:** список школ для главной страницы.

**Query-параметры (Spring `Pageable`):**

| Параметр | Тип    | По умолчанию | Описание                               |
| -------- | ------ | ------------ | -------------------------------------- |
| `page`   | int    | `0`          | Номер страницы                         |
| `size`   | int    | `20`         | Размер страницы (макс. 100)            |
| `sort`   | string | `name,asc`   | Поле сортировки (`name`, `created_at`) |

**Ответ `200 OK`:**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Инженерная школа информационных технологий и робототехники",
      "short_description": "Готовит специалистов в области ИТ и робототехники",
      "official_site_url": "https://it.tpu.ru"
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 20, "sort": { "sorted": true, "unsorted": false, "empty": false } },
  "totalElements": 10,
  "totalPages": 1,
  "first": true,
  "last": true,
  "size": 20,
  "number": 0,
  "numberOfElements": 10,
  "empty": false
}
```

---

#### `GET /api/v1/schools/{id}`

**Доступ:** публичный. 
**Ответ `200 OK`:**

```json
{
  "id": 1,
  "name": "...",
  "short_description": "...",
  "full_description": "...",
  "official_site_url": "https://it.tpu.ru",
  "created_at": "2025-01-01T10:00:00Z",
  "updated_at": "2025-01-01T10:00:00Z"
}
```

**Ошибки:** `404 Not Found`. 

---

### 3.2. Карточки

#### `GET /api/v1/cards`

**Доступ:** публичный. 
**Назначение:** подборка карточек для пользователя.

**Query-параметры:**

| Параметр    | Тип  | Обязательно | Описание                                                                                                 |
| ----------- | ---- | ----------- | -------------------------------------------------------------------------------------------------------- |
| `school_id` | long | нет         | Не передан -> карточки "ТПУ в целом" (`school_id IS NULL`). Передан -> карточки школы                    |
| `type`      | enum | нет         | `FACT`, `STORY`, `EVENT`, `STATISTIC`. Без фильтра — все                                                 |
| `seed`      | UUID | нет         | Seed для случайной сортировки. Не передан -> бэк генерирует `UUID.randomUUID()` только для этого запроса |
| `page`      | int  | нет         | По умолчанию `0`                                                                                         |
| `size`      | int  | нет         | По умолчанию `50`, макс. `1000`                                                                          |

**Ответ `200 OK`** — `Page<CardDto>`:

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "type": "FACT",
      "school_id": 1,
      "title": "Первый в Сибири",
      "content": "ТПУ — первый технический вуз за Уралом...",
      "content_format": "PLAIN",
      "published_at": "2025-01-15T10:00:00Z",
      "sources": [ { "id": 1, "title": "Официальный сайт", "url": "https://tpu.ru/history" } ],
      "images": [ { "id": "sha256abc", "url": "/assets/sha256abc.jpg", "alt_text": "Фото корпуса" } ],
      "event_details": null,
      "statistic_details": null
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "type": "EVENT",
      "school_id": null,
      "title": "День открытых дверей",
      "content": "Приглашаем абитуриентов...",
      "content_format": "MARKDOWN",
      "published_at": "2025-02-01T09:00:00Z",
      "sources": [],
      "images": [],
      "event_details": {
        "start_date": "2025-06-15",
        "end_date": null,
        "start_time": "10:00:00",
        "end_time": "14:00:00",
        "location": "г. Томск, пр. Ленина, 30",
        "details_url": "https://tpu.ru/events/123"
      },
      "statistic_details": null
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "type": "STATISTIC",
      "school_id": null,
      "title": "Студентов в ТПУ",
      "content": "Общее число студентов",
      "content_format": "PLAIN",
      "published_at": "2025-03-01T12:00:00Z",
      "sources": [],
      "images": [],
      "event_details": null,
      "statistic_details": { "value": 13500.0000, "unit": "человек", "period": "2024/2025", "source": "Отчёт ТПУ" }
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 50, "sort": { "sorted": true, "unsorted": false, "empty": false } },
  "totalElements": 42,
  "totalPages": 1,
  "first": true,
  "last": true,
  "size": 50,
  "number": 0,
  "numberOfElements": 42,
  "empty": false
}
```

---

#### `GET /api/v1/cards/{id}`

**Доступ:** публичный. 
**Ответ `200 OK`** одна карточка в том же формате. 
**Ошибки:** `404` не существует, не опубликована или истёк `delete_after`. 

---

## 4. Административные эндпоинты

> **Все эндпоинты раздела требуют JWT администратора**. В заголовке каждого эндпоинта указано «Доступ: админ». 
> Отдельные эндпоинты от публичных — потому что админу нужны черновики, прошедшие события, `unique_hash`, `created_by_user_id`, `delete_after`, расширенный поиск и фильтры.

### 4.1. Школы

#### `GET /api/v1/admin/schools`

**Доступ:** админ. 
Аналогично `GET /schools`, но отдаёт все поля (`full_description`, `created_at`, `updated_at`). Поддерживает `Pageable`.

---

#### `POST /api/v1/admin/schools`

**Доступ:** админ. 


**Тело:**

```json
{
  "name": "Новая школа",
  "short_description": "Краткое описание",
  "full_description": "Полное описание",
  "official_site_url": "https://new.tpu.ru"
}
```

**Ответ `201 Created`** объект с `id`, `created_at`, `updated_at`. 


**Ошибки:**

- `400` не заполнено `name` или `full_description`.
- `409` школа с таким `name` уже есть (уникальный индекс `schools.name`). 

---

#### `PUT /api/v1/admin/schools/{id}`

**Доступ:** админ. 
Полное обновление. Тело как у `POST`. 
**Ответ `200 OK`**. **`404`** если школы нет.

---

#### `DELETE /api/v1/admin/schools/{id}`

**Доступ:** админ. 
**Ответ `204 No Content`.** 

---

### 4.2. Карточки

#### `GET /api/v1/admin/cards`

**Доступ:** админ.

**Query-параметры:**

| Параметр               | Тип               | Описание                                                                                            |
| ---------------------- | ----------------- | --------------------------------------------------------------------------------------------------- |
| `page`, `size`, `sort` | Spring `Pageable` | По умолчанию `sort=created_at,desc`. Разрешено: `created_at`, `updated_at`, `title`, `published_at` |
| `type`                 | enum              | Фильтр по типу                                                                                      |
| `school_id`            | long              | Фильтр по школе                                                                                     |
| `published`            | bool              | `true` — только опубликованные; `false` — черновики; не передан — все                               |
| `search`               | string            | Поиск по `title` и `content` (`ILIKE %...%`)                                                        |

**Ответ `200 OK`**:

```json
{
  "content": [
    {
      "id": "550e8400-...",
      "type": "FACT",
      "school_id": 1,
      "title": "...",
      "content": "...",
      "content_format": "PLAIN",
      "published_at": "2025-01-15T10:00:00Z",
      "unique_hash": "a1b2c3...",
      "created_by_user_id": 12345,
      "created_at": "2025-01-15T10:00:00Z",
      "updated_at": "2025-01-15T10:00:00Z",
      "delete_after": null,
      "sources": [...],
      "images": [...],
      "event_details": null,
      "statistic_details": null
    }
  ],
  "pageable": { ... },
  "totalElements": 42,
  "totalPages": 5
}
```

---

#### `POST /api/v1/admin/cards`

**Доступ:** админ. 


**Тело (пример `EVENT`):**

```json
{
  "type": "EVENT",
  "school_id": null,
  "title": "День открытых дверей",
  "content": "Приглашаем абитуриентов...",
  "content_format": "MARKDOWN",
  "event_details": {
    "start_date": "2025-06-15",
    "end_date": null,
    "start_time": "10:00:00",
    "end_time": "14:00:00",
    "location": "г. Томск, пр. Ленина, 30",
    "details_url": "https://tpu.ru/events/123"
  },
  "sources": [ { "title": "Регистрация", "url": "https://tpu.ru/reg" } ],
  "image_ids": ["sha256abc"],
  "delete_after": "2025-06-20T00:00:00Z"
}
```

**Тело (пример `STATISTIC`):**

```json
{
  "type": "STATISTIC",
  "school_id": null,
  "title": "Студентов в ТПУ",
  "content": "Общее число студентов",
  "content_format": "PLAIN",
  "statistic_details": { "value": 13500.0000, "unit": "человек", "period": "2024/2025", "source": "Отчёт ТПУ" },
  "sources": [],
  "image_ids": []
}
```

**Ответ `201 Created`**. 


**Ошибки:**

- `400` — валидация (ProblemDetail с `errors[]`).
- `409` — дубликат `unique_hash`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Карточка с таким содержимым уже существует (id=550e8400-...)",
  "instance": "/api/v1/admin/cards"
}
```

---

#### `PUT /api/v1/admin/cards/{id}`

**Доступ:** админ. 
Полное обновление. Тело как у `POST`. 
**Ответ `200 OK`**. **`404`** если карточки нет. 

---

#### `DELETE /api/v1/admin/cards/{id}`

**Доступ:** админ. 
**Ответ `204 No Content`.** **`404`** если карточки нет. 

---

### 4.3. Изображения

#### `POST /api/v1/admin/cards/{cardId}/images`

**Доступ:** админ. 


**Заголовки:**

```
Cookie: access_token=<jwt>
Content-Type: multipart/form-data
```

**Тело (`multipart/form-data`):**

| Имя части  | Тип    | Обязательность | Описание                              |
| ---------- | ------ | -------------- | ------------------------------------- |
| `file`     | binary | да             | Файл изображения                      |
| `alt_text` | text   | нет            | Альтернативный текст (<=512 символов) |

**Пример `curl`:**

```bash
curl -X POST 'https://welcome.tpu.ru/api/v1/admin/cards/550e8400-e29b-41d4-a716-446655440000/images' \
  -H 'Cookie: access_token=<jwt>' \
  -F 'file=@/path/to/photo.jpg;type=image/jpeg' \
  -F 'alt_text=Главный корпус ТПУ'
```

**Ответ `201 Created`:**

```json
{
  "id": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "url": "/assets/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855.jpg",
  "mime_type": "image/jpeg",
  "size_bytes": 123456,
  "width": 1200,
  "height": 800,
  "alt_text": "Главный корпус ТПУ",
  "created_at": "2025-06-01T12:00:00Z"
}
```

**Ошибки:**

| Код           | Ситуация                                         |
| ------------- | ------------------------------------------------ |
| `400`         | Не image / пустой `file` / невалидный `alt_text` |
| `401` / `403` | Нет токена / не админ                            |
| `404`         | Карточки нет                                     |
| `409`         | Изображение уже привязано к этой карточке        |
| `413`         | Файл > 10 МБ                                     |

---

#### `DELETE /api/v1/admin/cards/{cardId}/images/{assetId}`

**Доступ:** админ. 
**Ответ `204 No Content`.** 

---

### 4.4. Отдача файлов

- Файлы лежат в `./uploads/{assetId}.{ext}`.
- Отдаются через `WebMvcConfigurer.addResourceHandlers("/assets/**").addResourceLocations("file:./uploads/")`.
- В `assets.url` — относительный путь `/assets/{assetId}.{ext}`.
