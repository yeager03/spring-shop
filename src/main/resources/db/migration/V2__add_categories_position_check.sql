ALTER TABLE categories
    ADD CONSTRAINT ck_categories_position
        CHECK (position >= 0);