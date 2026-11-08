# WelcomeTPU

«Welcome TPU» — адаптивная веб-платформа для ознакомления абитуриентов и студентов с историей, фактами, школами и мероприятиями ТПУ.
Платформа должна объединить разрозненную информацию об университете в одном сервисе с современным интерфейсом и корректным отображением на мобильных устройствах и десктопах.

## Подготовка

Склонируйте репозиторий и перейдите в его корень:

```bash
git clone https://github.com/Estuss/WelcomeTPU.git
cd WelcomeTPU
```

## Запуск всего проекта через Docker

Нужны установленный и запущенный Docker (например, Docker Desktop) и Docker Compose. Node.js, Java и Maven на компьютере для этого варианта не требуются.

Из корня репозитория выполните:

```bash
docker compose up --build -d
```

Команда соберёт фронтенд и бэкенд и запустит три сервиса: PostgreSQL, бэкенд и Nginx с фронтендом. Бэкенд дождётся готовности базы, а фронтенд — готовности бэкенда.

| Сервис | Адрес |
| --- | --- |
| Сайт | http://localhost:8081 |
| REST API | http://localhost:8080/api/v1/schools и http://localhost:8080/api/v1/cards |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Проверка состояния бэкенда | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5432` |

База, пользователь и пароль для локального запуска: `welcomtpu`. Данные PostgreSQL сохраняются в Docker volume `welcomtpu_pgdata`.

Просмотр состояния и логов:

```bash
docker compose ps
docker compose logs -f
```

Остановка с сохранением данных базы:

```bash
docker compose down
```

После изменения исходников повторите `docker compose up --build -d`. Для полного сброса, включая удаление данных базы, используйте `docker compose down -v`.

## Локальный запуск для разработки

### Фронтенд

Нужны Node.js 22.12+ и npm. Из корня репозитория выполните:

```bash
cd frontend
npm install
npm run dev
```

Откройте http://localhost:5173. Сервер разработки останавливается сочетанием `Ctrl+C`.

### Бэкенд

Нужны JDK 25, Maven 3.9+ и запущенный Docker с Compose для PostgreSQL. В отдельном терминале из корня репозитория выполните:

```bash
cd backend
mvn spring-boot:run
```

Spring Boot автоматически запустит PostgreSQL из `backend/compose.yaml`, применит миграции Flyway и поднимет API на http://localhost:8080. Swagger UI доступен по адресу http://localhost:8080/swagger-ui/index.html. При остановке приложения через `Ctrl+C` Spring Boot также останавливает базу, которую запустил сам.

Подключение к БД по умолчанию: `jdbc:postgresql://localhost:5432/welcomtpu`, пользователь и пароль — `welcomtpu`. Настройки находятся в `backend/src/main/resources/application.properties`.

Перед переходом с полного Docker-запуска на локальную разработку выполните `docker compose down` из корня репозитория, чтобы освободить порты и имя контейнера PostgreSQL.

## Архитектура

- **Фронтенд (`frontend/`)** — React 19, React Router и Vite. В `src/pages/` находятся страницы, в `src/components/` — общие компоненты, в `src/assets/` — изображения, в `src/data/` — локальные данные. Маршруты описаны в `src/App.jsx`, точка входа — `src/main.jsx`, общие стили — `src/index.css`.
- **Бэкенд (`backend/`)** — Java 25 и Spring Boot 4. REST-контроллеры (`controller/`) обрабатывают запросы школ и карточек, репозитории (`repository/`) работают с PostgreSQL через Spring Data JPA, сущности хранятся в `model/`, а DTO и преобразования ответов — в `dto/`. Пакеты находятся в `src/main/java/ru/tpu/welcomtpu/`.
- **База данных** — PostgreSQL 16. Миграции Flyway в `backend/src/main/resources/db/migration/` создают схему и добавляют начальные данные школ и карточек.
- **Docker** — корневой `docker-compose.yml` объединяет три сервиса в общую сеть. Nginx отдаёт сборку React и перенаправляет запросы `/api/` на `backend:8080`; бэкенд подключается к `postgres:5432`.