# SmartSell AI — Starter Project

โครงเริ่มต้น 3 ส่วนตาม stack ที่คุยกันไว้: **React + Java Spring Boot + PostgreSQL**

```
smartsell-ai/
├── frontend/     React (Vite) — หน้าเว็บ
├── backend/      Spring Boot — REST API
└── database/     schema.sql — โครงสร้างตาราง
```

## 1) ตั้งค่าฐานข้อมูล

ติดตั้ง PostgreSQL แล้วสร้าง database ชื่อ `smartsell`:

```bash
createdb smartsell
psql smartsell -f database/schema.sql
```

(ถ้าใช้ Hibernate `ddl-auto=update` ตามที่ตั้งไว้ใน `application.properties` จริง ๆ แล้ว
ไม่จำเป็นต้องรัน schema.sql เอง — Spring Boot จะสร้างตารางจาก entity ให้อัตโนมัติตอนรันครั้งแรก
แต่เก็บไฟล์นี้ไว้ดูโครงสร้าง/อ้างอิงตอนเขียนรายงานก็ได้)

## 2) รัน Backend

แก้ `backend/src/main/resources/application.properties`:
- ใส่ username/password ฐานข้อมูลของกัส
- ใส่ `app.ai.gemini-api-key` (ขอ API key ฟรีที่ https://aistudio.google.com)

แล้วรัน:

```bash
cd backend
mvn spring-boot:run
```

จะรันที่ `http://localhost:8080`

## 3) รัน Frontend

```bash
cd frontend
npm install
npm run dev
```

จะรันที่ `http://localhost:5173` และ proxy request `/api/*` ไปที่ backend ให้อัตโนมัติ
(ตั้งค่าไว้ใน `vite.config.js` แล้ว)

## สิ่งที่ทำให้พร้อมใช้แล้ว

- **Database**: ตาราง product, product_variant, product_tag, app_user, customer_session, chat_message, customer_interaction
- **Backend**: `GET /api/products`, `GET /api/products/{id}`, `GET /api/products/recommend`, `POST /api/chat`
- **Frontend**: หน้า Home (ดึงสินค้าจริงจาก API), Product Detail (สวอตช์สีเลือกได้จริง), Personal Shopping Assistant (แชทได้จริง ต่อกับ backend)

## สิ่งที่ต้องทำต่อ (ตามที่คุยกันไว้ใน sprint)

1. **เชื่อม Gemini/OpenAI API จริง** ใน `ChatService.java` (ตอนนี้เป็นแค่ echo stub ไว้ให้ endpoint รันได้ก่อน)
2. **Personal Color Check** page (อัปโหลดรูป → เรียก Vision API → เก็บผลใน `customer_session.personal_color`)
3. **Lucky Color** feature (logic ตามวันเกิด — ทำเป็น rule-based table ง่าย ๆ ก่อนได้)
4. **Staff/Manager** pages + role-based login (โครง `AppUser.role` และ `SecurityConfig` เตรียมไว้ให้แล้ว แค่ยังไม่ auth จริง)
5. **Analytics Dashboard** — query จากตาราง `customer_interaction` ที่เตรียมไว้ให้
6. **Virtual Try-On** — เรียก FASHN AI API จากปุ่มในหน้า Product Detail

## หน้าเว็บที่ยังไม่ได้ทำเป็น React component

เอา mockup HTML (`smartsell_ai_mockup.html`) ที่ทำไว้ก่อนหน้ามาแตกเป็น component เพิ่มได้เลย
โครงสี/ฟอนต์ใน `frontend/src/styles/theme.css` เป็นชุดเดียวกับใน mockup แล้ว
