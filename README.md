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

> **หมายเหตุ:** `schema.sql` ครบตาม entity แล้ว (รวม `size_chart`, `size_chart_row`, `store_info`, `store_hours`)
> แต่ **ข้อมูล** ของตารางเหล่านี้ (ตารางไซส์ ราคาตามไซส์ ข้อมูลร้าน เวลาเปิด) ต้องใส่เองใน DB — สคริปต์นี้สร้างแค่โครงตาราง

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

## หลัง pull: ต้องทำอะไรบ้าง

อัปเดตล่าสุดเปลี่ยน **ข้อมูลใน DB** ด้วย ไม่ใช่แค่โค้ด — pull แล้วต้องทำตามลำดับนี้ครั้งเดียว:

1. **รัน backend หนึ่งครั้ง** ให้ Hibernate เพิ่มคอลัมน์ใหม่ `product.target_group` และ `customer_session.gender`
2. **รันสคริปต์ 2 ตัว** (รันซ้ำได้ ไม่มีผลข้างเคียง):
   ```bash
   psql -d smartsell -f database/normalize_categories.sql
   psql -d smartsell -f database/seed_target_group.sql
   ```
   ท้ายสคริปต์แต่ละตัวจะแสดงสรุปผล ต้องได้หมวดหมู่ 3 หมวด (โปโล 2 / เสื้อครอป 1 / เสื้อยืด 8)
   และกลุ่มเป้าหมาย FEMALE 2 / MALE 1 / KIDS 1 / UNISEX 7
3. **รีสตาร์ท backend** (ไม่จำเป็นต้องทำ แต่ทำแล้วชัวร์กว่า)

ถ้าข้ามข้อ 2 สินค้าทุกตัวจะเป็น UNISEX และ occasion rule จะไม่มีผล (หมวดหมู่ไม่ตรงกับกฎ)

### อัปเดตล่าสุด

**หมวดหมู่สินค้า** — รวมเหลือ 3 หมวด: `โปโล` / `เสื้อยืด` / `เสื้อครอป`
(เสื้อสปอร์ต, AI ซีรีส์, สงกรานต์, เด็กหัวการค้า ฯลฯ เป็นเสื้อยืดทั้งหมด ชื่อคอลเลกชันยังอยู่ในชื่อสินค้า)
สินค้าตัวอย่างใน `DataInitializer` ใช้หมวดภาษาไทยแล้ว และเอาแจ็คเก็ตออก

**เพศ / กลุ่มเป้าหมายของสินค้า** — คอลัมน์ใหม่ `product.target_group`: `MALE` / `FEMALE` / `UNISEX` (default) / `KIDS`
กฎอยู่ที่ `entity/TargetGroup.java`
- **ผู้ช่วย AI** ถามเพศเป็นข้อที่ 5 (ข้อสุดท้าย) ตัวเลือก ชาย / หญิง / ไม่ระบุเพศ แล้ว **กรอง** สินค้า:
  ชาย → MALE + UNISEX, หญิง → FEMALE + UNISEX, ไม่ระบุ → ทุกตัว — **เสื้อเด็ก (KIDS) ไม่ถูกแนะนำเลย**
- **แชทกับเรา** ข้อมูลสินค้าที่ส่งให้ AI มีบรรทัด "สำหรับ: ผู้ชาย/ผู้หญิง/ทุกเพศ/เด็ก"
- **หน้าสีมงคล** มีกรอบ "เสื้อสำหรับ" (♂ ชาย / ♀ หญิง / ไม่ระบุ — ค่าเริ่มต้นไม่ระบุ) กรองสินค้าด้วยกฎเดียวกัน
  วงกลมสีมงคลไม่เปลี่ยนตามเพศ

**Occasion rule** (`OccasionRuleEngine`) — เดิมใช้หมวด `Polo` / `T-Shirt` ซึ่งไม่ตรงกับ DB จึงไม่มีผลเลย ตอนนี้:

| หมวดหมู่ | ทางการ | ทั่วไป | เข้ากิจกรรม |
|---|---|---|---|
| โปโล | 3 | 2 | 2 |
| เสื้อยืด | 0 | 3 | 3 |
| เสื้อครอป | 0 | 3 | 1 |

เป็น **คะแนน** ไม่ใช่ตัวกรอง — เลือก "ทางการ" โปโลขึ้นก่อน แต่ยังมีเสื้อยืดเติมให้ครบ 3 ตัว

**สีมงคล** (`ColorRuleEngine`, `LuckyColorScoringService`)
- คะแนนสีตามกลุ่ม: ตรงทั้งสีวันเกิดและด้านที่เสริม = 3 > ตรงแค่ด้านที่เสริม = 2 > ตรงแค่วันเกิด = 1
  (สีที่ตรงแค่บางส่วน เช่น "ดำ-ครีม" × 0.6) — ใช้ทั้งเรียงวงกลมสีและคัดสินค้า
- แสดงวงกลมสีไม่เกิน 4 สี และจองไว้อย่างน้อย 1 สีวันเกิดเสมอ
- แสดงสินค้าที่สีตรงทั้งหมด สูงสุด 10 ชิ้น (ไม่เอาสินค้าที่สีไม่ตรงมาเติม) ลูกศรเลื่อนโชว์เมื่อมีมากกว่า 3 ชิ้น
- วงกลมสีใช้ hex จาก backend (`colorHexes`) แล้ว สีทอง/พีช/แสด ไม่เป็นสีเทาอีก
- `ColorRuleEngine.scoreColor` หาสีที่ตรงเป๊ะให้ครบก่อนหาตรงบางส่วน (เดิม "เหลือง" ไปชน "เหลืองทอง" ของ Gold ก่อน)
  — มีผลกับ Personal Color ด้วย

**Personal Color Check** (`PersonalColorService`, `PersonalColorCheck.jsx`)
- การคำนวณโทนสีย้ายมาอยู่ที่ backend แล้ว หน้าเว็บส่งแค่คำตอบ 5 ข้อไป `POST /api/personal-color/quiz`
  เช่น `{ "sessionToken": "...", "answers": ["warm","cool","light","warm","deep"] }`
- ข้อ 1, 2, 4 ถามโทน (`warm`/`cool`) ข้อ 3, 5 ถามความเข้ม (`light`/`deep`)
  อุ่น+สว่าง = Spring, อุ่น+เข้ม = Autumn, เย็น+สว่าง = Summer, เย็น+เข้ม = Winter
- backend ตรวจคำตอบ — จำนวนไม่ครบ 5 ข้อหรือมีค่าที่ไม่ถูกต้องได้ 400 และบันทึกผลลง `personal_color_result` ด้วย `input_method = 'QUIZ'`
- **ถ้าแก้ลำดับหรือจำนวนคำถามในหน้าเว็บ ต้องแก้ `TONE_QUESTION` ใน `PersonalColorService` ให้ตรงกันด้วย**
  (ถ้าสลับข้อโทนกับข้อความเข้ม จะไม่ error แต่ผลผิด)

**Dashboard: ตัวกรองช่วงเวลา** (`components/DateRangeFilter.jsx`, `controller/DashboardPeriod.java`)
- เลือกได้ 4 โหมด: ทั้งหมด (ค่าเริ่มต้น) / รายเดือน / รายปี / กำหนดเอง (วันที่ถึงวันที่) — ปีแสดงเป็น พ.ศ.
- dropdown ปีเริ่มจากปีแรกที่มีข้อมูลในระบบ (`firstDataYear` จาก `/summary`) ถึงปีปัจจุบัน
- ทุกโหมดส่ง backend เป็น `?from=YYYY-MM-DD&to=YYYY-MM-DD` (รวมทั้งสองวัน) — ไม่ส่ง = ข้อมูลทั้งหมด
- กรองทุกการ์ด ตาราง Top 5 กราฟสถิติ และหน้า Product Analytics
- ช่วงเวลาอยู่ใน URL ของหน้าเว็บ (`?mode=&from=&to=`) refresh ไม่หลุด และกด "ดูเพิ่มเติม" แล้วใช้ช่วงเดียวกันต่อ
- นับตาม `created_at` — แอปบังคับใช้เวลาไทย (`Asia/Bangkok`) ตอนเริ่มทำงาน (`SmartSellApplication`)
  รันบนเซิร์ฟเวอร์ที่ตั้งเวลา UTC ก็ยังบันทึกและนับเป็นเวลาไทย

## ฟีเจอร์

**ฝั่งลูกค้า**
- **หน้าแรก** — เครื่องมือ Personal Color และ Size Chart (เดโมกรอกรอบอกแล้วแนะนำไซส์ได้จริง),
  "สินค้าแนะนำสำหรับคุณ" 6 อันดับตามยอดเปิดดู (เสมอกันใช้ยอดกดถูกใจ)
- **รายการสินค้า** — การ์ดแสดงราคาไซส์ที่ถูกที่สุด
- **หน้าสินค้า** — เลือกสี/ไซส์, ราคาเปลี่ยนตามไซส์, ปุ่มถูกใจ,
  ตารางขนาด (นิ้ว/ซม., รูปวิธีวัดแยกเสื้อยืด/โปโล, แนะนำไซส์จากรอบอก เผื่อหลวม 2 นิ้ว)
- **ผู้ช่วย AI** (`/assistant`) — ถาม 5 ข้อ (Personal Color, โอกาส, งบ, ไซส์, เพศ) แล้วแนะนำสินค้า 3 ตัว
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
| POST | `/api/personal-color/quiz` | ส่งคำตอบแบบทดสอบ 5 ข้อ → backend คำนวณโทนสี (Spring/Summer/Autumn/Winter) พร้อม palette |
| POST | `/api/personal-color/manual` | บันทึกโทนสีที่ลูกค้าเลือกเอง (หน้าเว็บไม่ได้เรียกแล้ว) |
| POST | `/api/lucky-color` | สีมงคล |
| POST | `/api/interactions`, `/api/interactions/rating` | เก็บสถิติ (VIEW, INTERESTED_CLICK, คะแนน) |
| POST | `/api/auth/register`, `/login`, `/verify-otp`, `/resend-otp`, `/forgot-password`, `/reset-password` | ระบบพนักงาน |
| GET | `/api/auth/me` | ข้อมูลผู้ใช้ที่ล็อกอิน |
| GET | `/api/admin/dashboard/summary`, `/demand-analytics`, `/products/analytics` (`?from=YYYY-MM-DD&to=YYYY-MM-DD` ไม่บังคับ) | Dashboard (ต้องล็อกอินเป็นผู้จัดการ/แอดมิน) — วันที่ผิดรูปแบบหรือ from หลัง to ได้ 400 |

## สิ่งที่ยังไม่ได้ทำ

- **Virtual Try-On** — เรียก FASHN AI API จากปุ่มในหน้า Product Detail
- **Body Analysis** — เอาการ์ดออกจากหน้าแรกไว้ก่อน (โค้ดเดิมอยู่ใน commit `bdc05fa`)
