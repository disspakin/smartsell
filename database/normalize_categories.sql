-- รวมหมวดหมู่สินค้าเหลือ 3 หมวด: โปโล / เสื้อยืด / เสื้อครอป
-- ชื่อคอลเลกชันเดิม (เด็กหัวการค้า, AI ซีรีส์, สงกรานต์ ...) ยังอยู่ในชื่อสินค้า
-- รันซ้ำได้ ไม่มีผลข้างเคียง: psql -d smartsell -f database/normalize_categories.sql

BEGIN;

UPDATE product SET category = 'เสื้อครอป' WHERE category = 'ครอป';

UPDATE product SET category = 'เสื้อยืด'
WHERE category IN ('เสื้อยืดเด็กหัวการค้า', 'เสื้อยืดเด็กหัวการค้า สีพื้น',
                   'AI ซีรีส์', 'สกรีนพิเศษ', 'สงกรานต์', 'สปอร์ต');

-- ข้อมูลตัวอย่างจาก DataInitializer รุ่นก่อน
UPDATE product SET category = 'โปโล'   WHERE category = 'Polo';
UPDATE product SET category = 'เสื้อยืด' WHERE category = 'T-Shirt';

COMMIT;

-- ต้องเหลือแค่ 3 หมวดนี้ ถ้ามีหมวดอื่นโผล่มา ให้เพิ่มใน UPDATE ด้านบน
SELECT category, count(*) AS products, string_agg(name, ', ' ORDER BY id) AS names
FROM product GROUP BY category ORDER BY category;
