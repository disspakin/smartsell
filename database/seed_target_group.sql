-- ตั้งว่าสินค้าทำมาสำหรับใคร (product.target_group) — ที่ไม่อยู่ในรายการนี้เป็น UNISEX ตาม default
-- อ้างจากชื่อสินค้า ไม่ใช่ id เพราะ id ในแต่ละเครื่องไม่ตรงกัน
-- ต้องรัน backend ครั้งหนึ่งก่อน ให้ Hibernate เพิ่มคอลัมน์ target_group
-- รันซ้ำได้ ไม่มีผลข้างเคียง: psql -d smartsell -f database/seed_target_group.sql

BEGIN;

UPDATE product SET target_group = 'FEMALE' WHERE name IN ('เสื้อโปโล หญิง', 'เสื้อครอป');
UPDATE product SET target_group = 'MALE'   WHERE name = 'เสื้อโปโล ชาย';
UPDATE product SET target_group = 'KIDS'   WHERE name LIKE '%(เสื้อเด็ก)%';

COMMIT;

SELECT target_group, string_agg(name, ', ' ORDER BY id) AS products
FROM product GROUP BY target_group ORDER BY target_group;
