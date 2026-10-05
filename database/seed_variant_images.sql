-- ใส่รูปให้ variant ของเสื้อ "เด็กหัวการค้า สีพื้น" (product 28) และเสื้อเด็กฟ้า-ขาว (product 44)
-- รันซ้ำได้ ไม่มีผลข้างเคียง: psql -d smartsell -f database/seed_variant_images.sql

BEGIN;

UPDATE product_variant v
SET image_url = m.image_url
FROM (VALUES
    (28, 'เขียวมะนาว',       '/images/products/tshirt_lime.png'),
    (28, 'บานเย็น-ครีม',     '/images/products/tshirt_magenta.png'),
    (28, 'ดำ-ครีม',          '/images/products/tshirt_black_cream.png'),
    (28, 'ชมพู',             '/images/products/tshirt_pink.png'),
    (28, 'ส้ม',              '/images/products/tshirt_orange.png'),
    (28, 'มัลเบอรี่-เหลือง',  '/images/products/tshirt_mulberry_yellow.png'),
    (28, 'ขาว-โอรส',         '/images/products/tshirt_white_coral.png'),
    (28, 'เบจ-เขียว',        '/images/products/tshirt_beige_green.png'),
    (28, 'ยีนส์-เงิน',       '/images/products/tshirt_denim_silver.png'),
    (28, 'เขียวขี้ม้า-แดง',   '/images/products/tshirt_olive_red.png'),
    (28, 'แดงเลือดหมู-ครีม', '/images/products/tshirt_maroon_cream.png'),
    (28, 'วอลนัท-เหลือง',    '/images/products/tshirt_walnut_yellow.png'),
    (28, 'ขาว-ดำ',           '/images/products/tshirt_white_black.png'),
    (28, 'แดง-ทอง',          '/images/products/tshirt_red_gold.png'),
    (28, 'เทา-ส้ม',          '/images/products/tshirt_gray_orange.png'),
    (28, 'ม่วง-ครีม',        '/images/products/tshirt_purple_cream.png'),
    (44, 'ฟ้า-ขาว',          '/images/products/tshirt_skyblue_white.png')
) AS m(product_id, color, image_url)
WHERE v.product_id = m.product_id
  AND v.color = m.color;

COMMIT;
