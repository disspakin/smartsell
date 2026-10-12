/** 170.00 → "170", 179.5 → "179.5" — ตัด .00 ทิ้งให้เหมือนที่แสดงอยู่เดิม */
export function formatBaht(value) {
  return String(Number(value));
}

/**
 * ราคาแบบช่วง (หน้าสินค้าตอนยังไม่เลือกไซส์): ใช้ minPrice/maxPrice จาก backend ถ้าไม่เท่ากันแสดงเป็นช่วง "150 – 180 บาท"
 * ข้อมูลที่ไม่มี min/max (เช่น API เก่า) ใช้ price เดิม
 */
export function formatPriceRange(product) {
  const min = product.minPrice ?? product.price;
  const max = product.maxPrice ?? product.price;
  if (Number(min) === Number(max)) return `${formatBaht(min)} บาท`;
  return `${formatBaht(min)} – ${formatBaht(max)} บาท`;
}

/** ราคาบนการ์ดสินค้า: แสดงราคาไซส์ที่ถูกที่สุดราคาเดียว (minPrice) — ข้อมูลที่ไม่มี minPrice ใช้ price เดิม */
export function formatMinPrice(product) {
  return `${formatBaht(product.minPrice ?? product.price)} บาท`;
}
