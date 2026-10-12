# SmartSell AI

เว็บโชว์สินค้าร้านเสื้อของ UTCC พร้อมผู้ช่วย AI แนะนำเสื้อผ้า — **React + Java Spring Boot + PostgreSQL**
ลูกค้าดูสินค้าบนเว็บแล้วมาซื้อที่หน้าร้าน (ไม่มีตะกร้าหรือสั่งซื้อออนไลน์)

```
smartsell/
├── frontend/     React (Vite) — หน้าเว็บ
├── backend/      Spring Boot — REST API
└── database/     schema.sql + สคริปต์ข้อมูลเสริม
```

## 1) ตั้งค่าฐานข้อมูล

ติดตั้ง PostgreSQL แล้วสร้าง database ชื่อ `smartsell`:

```bash
createdb smartsell
psql smartsell -f database/schema.sql
```

backend ตั้ง `spring.jpa.hibernate.ddl-auto=update` ไว้ Hibernate จะสร้างตาราง/คอลัมน์ที่ยังไม่มีให้จาก entity ตอนรัน
(ห้ามเปลี่ยนเป็น `create` หรือ `create-drop` — ข้อมูลจะถูกลบทุกครั้งที่รันแอป)

> **หมายเหตุ:** `schema.sql` ยังไม่มีตาราง/คอลัมน์ที่เพิ่มทีหลัง ได้แก่ `size_chart`, `size_chart_row`,
> `product.size_chart_id`, `product_variant.price`, `store_info`, `store_hours`, `product_tag`
> Hibernate สร้างโครงให้ แต่ **ข้อมูล** ของตารางเหล่านี้ (ตารางไซส์ ราคาตามไซส์ ข้อมูลร้าน เวลาเปิด) ต้องใส่เองใน DB

ข้อมูลที่ seed อัตโนมัติตอนรันแอป (`config/DataInitializer.java`) — ทุกตัวเช็กก่อน insert รันซ้ำไม่เกิดข้อมูลซ้ำ:
- สินค้าตัวอย่าง: เฉพาะตอนตาราง `product` ว่าง
- บัญชีแอดมิน: จาก `app.seed.admin-email` / `app.seed.admin-password` ถ้ายังไม่มีอีเมลนี้

ตาราง `season_palette` ไม่มีโค้ด seed — ต้อง insert เอง **ครั้งเดียว** (รันซ้ำจะได้แถวซ้ำ)

## 2) รัน Backend

คัดลอก `backend/src/main/resources/application.properties.example` เป็น `application.properties` แล้วใส่ค่า:
- `spring.datasource.*` — username/password ฐานข้อมูล
- `app.ai.gemini-api-key` — ขอ API key ฟรีที่ https://aistudio.google.com (ไม่ใส่ แชทจะตอบด้วยกฎสำรอง)
- `app.jwt.secret`, `app.auth.invite-code` — สำหรับระบบพนักงาน
- `app.seed.admin-email`, `app.seed.admin-password` — บัญชีแอดมินเริ่มต้น (ไม่ใส่ก็ได้)
- `spring.mail.*` — Gmail SMTP สำหรับส่ง OTP ยืนยันอีเมล/รีเซ็ตรหัสผ่าน

`application.properties` อยู่ใน `.gitignore` แล้ว ห้าม commit ไฟล์นี้

```bash
cd backend
mvn spring-boot:run      # หรือ sh mvnw spring-boot:run
```

จะรันที่ `http://localhost:8080`

รันเทสต์ (ใช้ H2 ในหน่วยความจำ ไม่แตะ PostgreSQL จริง):

```bash
cd backend
sh mvnw test
```

## 3) รัน Frontend

```bash
cd frontend
npm install
npm run dev
```

จะรันที่ `http://localhost:5173` และ proxy request `/api/*` ไปที่ backend ให้อัตโนมัติ (ตั้งค่าไว้ใน `vite.config.js`)

## ฟีเจอร์

**ฝั่งลูกค้า**
- **หน้าแรก** — เครื่องมือ Personal Color และ Size Chart (เดโมกรอกรอบอกแล้วแนะนำไซส์ได้จริง),
  "สินค้าแนะนำสำหรับคุณ" 6 อันดับตามยอดเปิดดู (เสมอกันใช้ยอดกดถูกใจ)
- **รายการสินค้า** — การ์ดแสดงราคาไซส์ที่ถูกที่สุด
- **หน้าสินค้า** — เลือกสี/ไซส์, ราคาเปลี่ยนตามไซส์, ปุ่มถูกใจ,
  ตารางขนาด (นิ้ว/ซม., รูปวิธีวัดแยกเสื้อยืด/โปโล, แนะนำไซส์จากรอบอก เผื่อหลวม 2 นิ้ว)
- **ผู้ช่วย AI** (`/assistant`) — ถาม 4 ข้อ (Personal Color, โอกาส, งบ, ไซส์) แล้วแนะนำสินค้า 3 ตัว
- **แชทกับเรา** (`/support`) — ตอบคำถามสินค้าจากข้อมูลในฐานข้อมูล
- **Personal Color Check**, **สีมงคลประจำวัน**

**แชทบอททั้งสองตัว** ได้ข้อมูลร้าน (`store_info` ทุกแถว), เวลาเปิด (`store_hours`), วันเวลาปัจจุบัน
(เวลาไทย `Asia/Bangkok`) และตารางไซส์ใหม่ทุกข้อความ:
- ตอบเรื่องร้านจากข้อมูลใน DB เท่านั้น สถานะเปิด/ปิดคำนวณในโค้ด ไม่ให้ AI คิดเวลาเอง
- เรื่องของร้านที่ไม่มีข้อมูล → "ขออภัย ยังไม่มีข้อมูลนี้ กรุณาติดต่อร้านที่เบอร์ ..."
- ถามสต็อก → แนะนำโทรถามร้านก่อนเดินทาง (สต็อกในระบบยังไม่ใช่ของจริง)
- ตารางไซส์ที่ `is_estimated = true` → บอกลูกค้าว่าเป็นค่าประมาณ
- เรื่องนอกเหนือสินค้า/ร้าน → "ขออภัยค่ะ ไม่มีข้อมูลเรื่องนี้ในฐานระบบข้อมูลของร้านค้าค่ะ"

**ฝั่งพนักงาน** — สมัคร (ต้องมี invite code) + ยืนยันอีเมลด้วย OTP, เข้าสู่ระบบ, ลืมรหัสผ่าน,
Dashboard และ Product Analytics (ดูได้เฉพาะ STORE_MANAGER / ADMIN)

## ราคาตามไซส์

`product_variant.price` เป็น NULL ได้ — NULL = ใช้ `product.price`
ราคาที่ใช้จริงคำนวณที่เดียวใน `ProductVariant.getEffectivePrice()` แล้วส่งออก API เป็น
`variants[].effectivePrice` และ `minPrice` / `maxPrice` ของสินค้า

## REST API

| Method | Path | ใช้ทำอะไร |
|---|---|---|
| GET | `/api/products` (`?category=`) | รายการสินค้า |
| GET | `/api/products/popular?limit=6` | สินค้ายอดนิยม (หน้าแรก) |
| GET | `/api/products/{id}` | รายละเอียดสินค้า |
| GET | `/api/products/{id}/size-chart` | ตารางไซส์ของสินค้า (404 = ไม่มีตาราง) |
| GET | `/api/store` | ข้อมูลร้าน เวลาเปิด และสถานะเปิด/ปิดตอนนี้ |
| POST | `/api/chat/assistant` | ผู้ช่วย AI |
| POST | `/api/chat/support` | แชทกับเรา |
| POST | `/api/personal-color/manual` | ผล Personal Color จากแบบสอบถาม |
| POST | `/api/lucky-color` | สีมงคล |
| POST | `/api/interactions`, `/api/interactions/rating` | เก็บสถิติ (VIEW, INTERESTED_CLICK, คะแนน) |
| POST | `/api/auth/register`, `/login`, `/verify-otp`, `/resend-otp`, `/forgot-password`, `/reset-password` | ระบบพนักงาน |
| GET | `/api/auth/me` | ข้อมูลผู้ใช้ที่ล็อกอิน |
| GET | `/api/admin/dashboard/summary`, `/demand-analytics`, `/products/analytics` | Dashboard (ต้องล็อกอินเป็นผู้จัดการ/แอดมิน) |

## สิ่งที่ยังไม่ได้ทำ

- **Virtual Try-On** — เรียก FASHN AI API จากปุ่มในหน้า Product Detail
- **Body Analysis** — เอาการ์ดออกจากหน้าแรกไว้ก่อน (โค้ดเดิมอยู่ใน commit `bdc05fa`)
- อัปเดต `database/schema.sql` ให้ครบตามตารางที่เพิ่มทีหลัง (ดูหมายเหตุในข้อ 1)
