-- SmartSell AI — Database Schema (PostgreSQL)

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    role          VARCHAR(20) NOT NULL DEFAULT 'STORE_MANAGER', -- STORE_MANAGER | ADMIN
    display_name  VARCHAR(100),
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    -- บัญชีใหม่ตั้งเป็น false ตอนสมัคร DEFAULT TRUE มีไว้ให้บัญชีเก่าที่มีก่อนระบบ OTP
    email_verified BOOLEAN NOT NULL DEFAULT TRUE
);

-- OTP ยืนยันอีเมล/ตั้งรหัสผ่านใหม่ หนึ่งบัญชีมีได้หนึ่งแถว เก็บแค่ BCrypt hash ของรหัส
CREATE TABLE email_otp (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    purpose         VARCHAR(20) NOT NULL DEFAULT 'VERIFY_EMAIL', -- VERIFY_EMAIL | RESET_PASSWORD
    code_hash       VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- ตารางไซส์หนึ่งแบบ สินค้าหลายตัวใช้ร่วมกันได้ (product.size_chart_id) ตัวเลขเป็นนิ้ว
CREATE TABLE size_chart (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE,
    is_estimated BOOLEAN NOT NULL DEFAULT FALSE  -- true = ตัวเลขยังเป็นค่าประมาณ ร้านยังไม่ยืนยัน
);

CREATE TABLE size_chart_row (
    id            BIGSERIAL PRIMARY KEY,
    size_chart_id BIGINT NOT NULL REFERENCES size_chart(id) ON DELETE CASCADE,
    size          VARCHAR(10) NOT NULL,
    chest_in      NUMERIC(4,1) NOT NULL,
    length_in     NUMERIC(4,1) NOT NULL,
    sort_order    INT NOT NULL,       -- เรียงตามตัวอักษรจะได้ 2XL, 3XL ก่อน S
    UNIQUE (size_chart_id, size)
);

CREATE TABLE product (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    category    VARCHAR(50),          -- โปโล / เสื้อยืด / เสื้อครอป (ดู normalize_categories.sql)
    price       NUMERIC(10,2) NOT NULL,
    description TEXT,
    image_url   VARCHAR(500),
    target_group VARCHAR(10) NOT NULL DEFAULT 'UNISEX', -- MALE / FEMALE / UNISEX / KIDS
    size_chart_id BIGINT REFERENCES size_chart(id),     -- NULL = ยังไม่มีตารางไซส์ หน้าเว็บซ่อนลิงก์
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- one product can come in several colors/sizes, each a separate sellable variant
CREATE TABLE product_variant (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    color      VARCHAR(30),
    color_hex  VARCHAR(7),
    size       VARCHAR(10),           -- XS,S,M,L,XL
    image_url  VARCHAR(500),
    stock_qty  INT NOT NULL DEFAULT 0 CHECK (stock_qty >= 0),
    price      NUMERIC(10,2)          -- ราคาเฉพาะไซส์นี้ (เช่น 3XL ขึ้นไป) NULL = ใช้ product.price
);

-- ข้อมูลร้านแบบ key/label/value เพิ่มหัวข้อได้ใน DB โดยไม่ต้องแก้โค้ด (chatbot และ footer ใช้ทุกแถว)
CREATE TABLE store_info (
    id         BIGSERIAL PRIMARY KEY,
    info_key   VARCHAR(255) NOT NULL UNIQUE,
    label      VARCHAR(255) NOT NULL,
    info_value TEXT NOT NULL
);

-- เวลาเปิด-ปิดรายวัน day_of_week: 1 = จันทร์ ... 7 = อาทิตย์ วันที่ปิด: is_closed = true และเวลาเป็น NULL
CREATE TABLE store_hours (
    id          BIGSERIAL PRIMARY KEY,
    day_of_week SMALLINT NOT NULL UNIQUE CHECK (day_of_week BETWEEN 1 AND 7),
    open_time   TIME,
    close_time  TIME,
    is_closed   BOOLEAN NOT NULL DEFAULT FALSE
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
    gender            VARCHAR(10),    -- MALE / FEMALE / ANY (ไม่ระบุ)
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
    event_type     VARCHAR(30) NOT NULL, -- VIEW | INTERESTED_CLICK | AI_RECOMMENDED | LUCKY_COLOR_SEARCH | *_RATING
    personal_color VARCHAR(20),
    occasion       VARCHAR(30),
    lucky_color    VARCHAR(30),   -- เป้าหมายเสริมดวง (การงาน, ความรัก, ...) ไม่ใช่ชื่อสี
    client_token   VARCHAR(100),  -- token ของผู้ใช้ที่ไม่ได้ล็อกอิน ใช้กันนับ LUCKY_COLOR_SEARCH ซ้ำ
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
