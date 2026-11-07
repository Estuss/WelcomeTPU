-- Начальная схема БД Welcome TPU (по database-scheme-draft.txt + иконки школ + PERSON/MEDIA карточки)

CREATE TABLE assets (
    id          VARCHAR(64) PRIMARY KEY, -- sha256 содержимого файла
    url         VARCHAR(1024) NOT NULL,
    mime_type   VARCHAR(50) NOT NULL,    -- image/jpeg, image/png, image/webp
    size_bytes  BIGINT NOT NULL,
    width       INT NOT NULL,
    height      INT NOT NULL,
    alt_text    VARCHAR(512),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);


CREATE TABLE schools (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL UNIQUE,
    short_description  TEXT,
    full_description   TEXT NOT NULL,
    official_site_url  VARCHAR(1024),
    icon_asset_id      VARCHAR(64) REFERENCES assets (id),
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE cards (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type                VARCHAR(20) NOT NULL, -- FACT | EVENT | STATISTIC | PERSON | MEDIA
    school_id           BIGINT REFERENCES schools (id), -- null - карточка о ТПУ в целом
    title               VARCHAR(1024) NOT NULL,
    content             TEXT NOT NULL,
    content_format      VARCHAR(20) NOT NULL DEFAULT 'PLAIN', -- PLAIN | MARKDOWN | HTML
    published_at        TIMESTAMP, -- null - черновик
    unique_hash         VARCHAR(64) NOT NULL UNIQUE, -- sha256 от type, school_id, title, content
    created_by_user_id  BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE card_sources (
    id       BIGSERIAL PRIMARY KEY,
    card_id  UUID NOT NULL REFERENCES cards (id),
    title    VARCHAR(255),
    url      VARCHAR(1024) NOT NULL
);

CREATE TABLE event_details (
    card_id      UUID PRIMARY KEY REFERENCES cards (id),
    start_date   DATE NOT NULL,
    end_date     DATE,
    start_time   TIME,
    end_time     TIME,
    location     VARCHAR(1024),
    details_url  VARCHAR(1024)
);

CREATE TABLE statistic_details (
    card_id  UUID PRIMARY KEY REFERENCES cards (id),
    value    NUMERIC(20, 4) NOT NULL,
    unit     VARCHAR(50) NOT NULL,
    period   VARCHAR(100),
    source   VARCHAR(500)
);

-- Персона: имя, фото, краткая биография, достижения
CREATE TABLE person_details (
    card_id         UUID PRIMARY KEY REFERENCES cards (id),
    full_name       VARCHAR(255) NOT NULL,
    photo_asset_id  VARCHAR(64) REFERENCES assets (id),
    bio             TEXT,
    achievements    TEXT
);

-- Медиа: фото или видео, подпись, краткое описание, источник
CREATE TYPE media_type AS ENUM ('PHOTO', 'VIDEO');

CREATE TABLE media_details (
    card_id       UUID PRIMARY KEY REFERENCES cards (id),
    media_type    MEDIA_TYPE NOT NULL,
    asset_id      VARCHAR(64) REFERENCES assets (id),   -- для фото (и видео-превью)
    external_url  VARCHAR(1024),                        -- для внешнего видео
    caption       VARCHAR(255),
    description   TEXT,
    source        VARCHAR(500)
);

CREATE TABLE card_assets (
    card_id   UUID NOT NULL REFERENCES cards (id),
    asset_id  VARCHAR(64) NOT NULL REFERENCES assets (id),
    PRIMARY KEY (card_id, asset_id)
);

CREATE INDEX idx_cards_school_id ON cards (school_id);
CREATE INDEX idx_cards_type ON cards (type);
CREATE INDEX idx_cards_published_at ON cards (published_at);
