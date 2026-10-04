-- SmartSell AI — Database Schema (PostgreSQL)

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    role          VARCHAR(20) NOT NULL DEFAULT 'STORE_MANAGER', -- STORE_MANAGER | ADMIN
    display_name  VARCHAR(100),
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE product (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    category    VARCHAR(50),          -- Polo, T-Shirt, ...
    price       NUMERIC(10,2) NOT NULL,
    description TEXT,
    image_url   VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- one product can come in several colors/sizes, each a separate sellable variant
CREATE TABLE product_variant (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    color      VARCHAR(30),
    color_hex  VARCHAR(7),
    size       VARCHAR(10),           -- XS,S,M,L,XL
    image_url  VARCHAR(500)
);



-- one row per anonymous or logged-in shopping session with the AI assistant
CREATE TABLE customer_session (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT REFERENCES app_user(id),
    personal_color    VARCHAR(20),    -- Spring/Summer/Autumn/Winter
    occasion          VARCHAR(30),
    budget_min        NUMERIC(10,2),
    budget_max        NUMERIC(10,2),
    size              VARCHAR(10),
    birth_weekday     VARCHAR(15),
    lucky_goal        VARCHAR(30),
    preferences_json  JSONB,          -- anything else the LLM extracts, kept flexible
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE chat_message (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT NOT NULL REFERENCES customer_session(id) ON DELETE CASCADE,
    sender      VARCHAR(10) NOT NULL,  -- USER | AI
    content     TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- logged interactions for the analytics dashboard (product views, clicks, AI usage)
CREATE TABLE customer_interaction (
    id             BIGSERIAL PRIMARY KEY,
    session_id     BIGINT REFERENCES customer_session(id) ON DELETE CASCADE,
    product_id     BIGINT REFERENCES product(id) ON DELETE CASCADE,
    event_type     VARCHAR(30) NOT NULL, -- VIEW | INTERESTED_CLICK | AI_RECOMMENDED
    personal_color VARCHAR(20),
    occasion       VARCHAR(30),
    lucky_color    VARCHAR(30),
    size           VARCHAR(10),
    budget         NUMERIC(10,2),
    rating         INT,
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE personal_color_result (
    id            BIGSERIAL PRIMARY KEY,
    session_token VARCHAR(100),
    input_method  VARCHAR(20) NOT NULL,
    season        VARCHAR(20) NOT NULL,
    confidence    NUMERIC(4,1),
    reasoning     TEXT,
    photo_url     VARCHAR(500),
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE season_palette (
    id         BIGSERIAL PRIMARY KEY,
    season     VARCHAR(20) NOT NULL,
    color_name VARCHAR(50) NOT NULL,
    color_hex  VARCHAR(7) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_variant_product ON product_variant(product_id);
CREATE INDEX idx_interaction_product ON customer_interaction(product_id);
