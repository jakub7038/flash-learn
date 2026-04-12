-- FlashLearn migration V3
-- Dodanie ograniczeń długości kolumn tekstowych

ALTER TABLE decks
    ALTER COLUMN title TYPE VARCHAR(100),
    ALTER COLUMN description TYPE VARCHAR(500);
