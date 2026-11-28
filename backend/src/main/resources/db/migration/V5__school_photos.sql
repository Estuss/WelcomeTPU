-- Связь школа <-> ассеты: несколько фотографий на школу
CREATE TABLE school_assets (
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    asset_id  VARCHAR(64) NOT NULL REFERENCES assets (id),
    position  INT NOT NULL DEFAULT 0, -- порядок отображения
    PRIMARY KEY (school_id, asset_id)
);

CREATE INDEX idx_school_assets_school ON school_assets (school_id);
