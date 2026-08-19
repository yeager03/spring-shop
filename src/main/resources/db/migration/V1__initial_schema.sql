CREATE TABLE users
(
    user_id                BIGSERIAL,

    email                  VARCHAR(255) NOT NULL,
    password_hash          VARCHAR(255) NOT NULL,
    role                   VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,

    first_name             VARCHAR(100) NOT NULL,
    last_name              VARCHAR(100) NOT NULL,

    authentication_version INTEGER      NOT NULL DEFAULT 0,

    avatar_key             VARCHAR(512) NULL,

    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users
        PRIMARY KEY (user_id),

    CONSTRAINT uq_users_email
        UNIQUE (email),

    CONSTRAINT ck_users_role
        CHECK (role IN ('CUSTOMER', 'MANAGER', 'ADMIN'))
);


CREATE TABLE user_addresses
(
    address_id      BIGSERIAL,

    user_id         BIGINT       NOT NULL,

    label           VARCHAR(50)  NOT NULL,

    recipient_name  VARCHAR(200) NOT NULL,
    recipient_phone VARCHAR(30)  NOT NULL,

    country         VARCHAR(100) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    street          VARCHAR(255) NOT NULL,
    house           VARCHAR(50)  NOT NULL,
    apartment       VARCHAR(50)  NULL,
    postal_code     VARCHAR(20)  NULL,

    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_addresses
        PRIMARY KEY (address_id),

    CONSTRAINT fk_user_addresses_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE,

    CONSTRAINT uq_user_addresses_user_label
        UNIQUE (user_id, label)
);
CREATE UNIQUE INDEX uq_user_addresses_default
    ON user_addresses (user_id) WHERE is_default = TRUE;


CREATE TABLE sessions
(
    session_id  BIGSERIAL,

    user_id     BIGINT       NOT NULL,

    jti         VARCHAR(36)  NOT NULL,
    token_hash  VARCHAR(128) NOT NULL,

    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    issued_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ  NOT NULL,

    replaced_by BIGINT       NULL,

    CONSTRAINT pk_sessions
        PRIMARY KEY (session_id),

    CONSTRAINT fk_sessions_user_id
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE,

    CONSTRAINT uq_sessions_jti
        UNIQUE (jti),

    CONSTRAINT fk_sessions_replaced_by
        FOREIGN KEY (replaced_by)
            REFERENCES sessions (session_id)
            ON DELETE SET NULL,

    CONSTRAINT ck_sessions_status
        CHECK (status IN ('ACTIVE', 'REVOKED', 'REUSED')),

    CONSTRAINT ck_sessions_dates
        CHECK (issued_at < expires_at)
);
CREATE INDEX idx_sessions_user_id_status
    ON sessions (user_id, status);


CREATE TABLE categories
(
    category_id BIGSERIAL,

    parent_id   BIGINT       NULL,

    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(255) NOT NULL,

    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    position    INTEGER      NOT NULL DEFAULT 0,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_categories
        PRIMARY KEY (category_id),

    CONSTRAINT fk_categories_parent_id
        FOREIGN KEY (parent_id)
            REFERENCES categories (category_id),

    CONSTRAINT ck_categories_not_self_parent
        CHECK (parent_id IS NULL OR parent_id <> category_id),

    CONSTRAINT uq_categories_slug
        UNIQUE (slug)
);
CREATE INDEX idx_categories_parent_id_position
    ON categories (parent_id, position);


CREATE TABLE products
(
    product_id  BIGSERIAL,

    title       VARCHAR(255)   NOT NULL,
    slug        VARCHAR(255)   NOT NULL,
    description TEXT           NULL,

    price       NUMERIC(12, 2) NOT NULL,
    stock       INTEGER        NOT NULL DEFAULT 0,

    is_active   BOOLEAN        NOT NULL DEFAULT TRUE,

    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_products
        PRIMARY KEY (product_id),

    CONSTRAINT uq_products_slug
        UNIQUE (slug),

    CONSTRAINT ck_products_price
        CHECK (price >= 0),

    CONSTRAINT ck_products_stock
        CHECK (stock >= 0)
);


CREATE TABLE product_categories
(
    product_id  BIGINT      NOT NULL,
    category_id BIGINT      NOT NULL,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_product_categories
        PRIMARY KEY (product_id, category_id),

    CONSTRAINT fk_product_categories_product
        FOREIGN KEY (product_id)
            REFERENCES products (product_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_product_categories_category
        FOREIGN KEY (category_id)
            REFERENCES categories (category_id)
            ON DELETE CASCADE
);
CREATE INDEX idx_product_categories_category_id
    ON product_categories (category_id);


CREATE TABLE product_images
(
    image_id   BIGSERIAL,

    product_id BIGINT       NOT NULL,

    image_key  VARCHAR(512) NOT NULL,
    position   INTEGER      NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_product_images
        PRIMARY KEY (image_id),

    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id)
            REFERENCES products (product_id)
            ON DELETE CASCADE,

    CONSTRAINT uq_product_images_product_position
        UNIQUE (product_id, position),

    CONSTRAINT ck_product_images_position
        CHECK (position >= 0)
);


CREATE TABLE carts
(
    cart_id    BIGSERIAL,

    user_id    BIGINT      NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_carts
        PRIMARY KEY (cart_id),

    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE,

    CONSTRAINT uq_carts_user
        UNIQUE (user_id)
);


CREATE TABLE cart_items
(
    cart_item_id BIGSERIAL,

    cart_id      BIGINT      NOT NULL,
    product_id   BIGINT      NOT NULL,

    quantity     INTEGER     NOT NULL DEFAULT 1,

    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_cart_items
        PRIMARY KEY (cart_item_id),

    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id)
            REFERENCES carts (cart_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id)
            REFERENCES products (product_id)
            ON DELETE CASCADE,

    CONSTRAINT uq_cart_items_cart_product
        UNIQUE (cart_id, product_id),

    CONSTRAINT ck_cart_items_quantity
        CHECK (quantity > 0)
);


CREATE TABLE orders
(
    order_id        BIGSERIAL,

    user_id         BIGINT         NOT NULL,

    status          VARCHAR(30)    NOT NULL DEFAULT 'CREATED',

    total_amount    NUMERIC(12, 2) NOT NULL,

    recipient_name  VARCHAR(200)   NOT NULL,
    recipient_phone VARCHAR(30)    NOT NULL,

    country         VARCHAR(100)   NOT NULL,
    city            VARCHAR(100)   NOT NULL,
    street          VARCHAR(255)   NOT NULL,
    house           VARCHAR(50)    NOT NULL,
    apartment       VARCHAR(50)    NULL,
    postal_code     VARCHAR(20)    NULL,

    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_orders
        PRIMARY KEY (order_id),

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id),

    CONSTRAINT ck_orders_status
        CHECK (status IN ('CREATED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),

    CONSTRAINT ck_orders_total_amount
        CHECK (total_amount >= 0)
);
CREATE INDEX idx_orders_user_id_created_at
    ON orders (user_id, created_at DESC);

CREATE INDEX idx_orders_status
    ON orders (status);


CREATE TABLE order_items
(
    order_item_id BIGSERIAL,

    order_id      BIGINT         NOT NULL,
    product_id    BIGINT         NULL,

    product_title VARCHAR(255)   NOT NULL,
    unit_price    NUMERIC(12, 2) NOT NULL,
    quantity      INTEGER        NOT NULL,

    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_order_items
        PRIMARY KEY (order_item_id),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
            REFERENCES orders (order_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id)
            REFERENCES products (product_id)
            ON DELETE SET NULL,

    CONSTRAINT ck_order_items_unit_price
        CHECK (unit_price >= 0),

    CONSTRAINT ck_order_items_quantity
        CHECK (quantity > 0)
);
CREATE INDEX idx_order_items_order_id
    ON order_items (order_id);


CREATE TABLE reviews
(
    review_id  BIGSERIAL,

    user_id    BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,

    rating     SMALLINT    NOT NULL,
    comment    TEXT        NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_reviews
        PRIMARY KEY (review_id),

    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_reviews_product
        FOREIGN KEY (product_id)
            REFERENCES products (product_id)
            ON DELETE CASCADE,

    CONSTRAINT uq_reviews_user_product
        UNIQUE (user_id, product_id),

    CONSTRAINT ck_reviews_rating
        CHECK (rating BETWEEN 1 AND 5)
);
CREATE INDEX idx_reviews_product_id
    ON reviews (product_id);


CREATE TABLE favorites
(
    user_id    BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_favorites
        PRIMARY KEY (user_id, product_id),

    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_favorites_product
        FOREIGN KEY (product_id)
            REFERENCES products (product_id)
            ON DELETE CASCADE
);
CREATE INDEX idx_favorites_product_id
    ON favorites (product_id);