-- Новый тип карточки QR: карточка с QR-кодом, ведущим на целевой URL

-- cards.type — VARCHAR(20), значения не ограничены constraint'ом, поэтому DDL для cards не нужен.

CREATE TABLE qr_details (
    card_id     UUID PRIMARY KEY REFERENCES cards (id),
    target_url  VARCHAR(1024) NOT NULL,          -- URL, зашитый в QR-код
    caption     VARCHAR(255),                    -- подпись под QR-кодом
    source      VARCHAR(500)
);

CREATE INDEX idx_cards_type_qr ON cards (type) WHERE type = 'QR';
