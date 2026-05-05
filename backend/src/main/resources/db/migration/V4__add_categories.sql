-- V4__add_categories.sql
-- Tabela kategorii talii

CREATE TABLE categories (
                            id         BIGSERIAL PRIMARY KEY,
                            name       VARCHAR(50)  NOT NULL UNIQUE,
                            slug       VARCHAR(50)  NOT NULL UNIQUE,
                            icon_name  VARCHAR(50)  NOT NULL
);

-- Predefiniowane kategorie
INSERT INTO categories (name, slug, icon_name) VALUES
                                                   ('Języki',        'jezyki',        'language'),
                                                   ('Programowanie', 'programowanie', 'code'),
                                                   ('Matematyka',    'matematyka',    'calculate'),
                                                   ('Nauki ścisłe',  'nauki-scisle',  'science'),
                                                   ('Historia',      'historia',      'history_edu'),
                                                   ('Inne',          'inne',          'category');

-- Dodanie kolumny category_id do decks (nullable — talia nie musi miec kategorii)
ALTER TABLE decks
    ADD COLUMN category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL;

-- Indeks dla wydajnego filtrowania po kategorii w Marketplace
CREATE INDEX idx_decks_category_id ON decks(category_id);