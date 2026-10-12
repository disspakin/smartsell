// เผื่อหลวม: เสื้อต้องมีรอบอกมากกว่ารอบอกตัวลูกค้าอย่างน้อยเท่านี้ (นิ้ว)
export const EASE_INCHES = 2;

/**
 * แนะนำไซส์จากรอบอกของลูกค้า (นิ้ว): ไซส์ที่ chestIn เล็กที่สุดที่ยัง >= รอบอกลูกค้า + 2 นิ้ว
 * คืน row ของไซส์นั้น หรือ null ถ้าไม่มีไซส์ไหนในตารางใหญ่พอ
 */
export function recommendSize(rows, bodyChestInches) {
  const needed = bodyChestInches + EASE_INCHES;
  return rows
    .filter((row) => row.chestIn >= needed)
    .reduce((best, row) => (best === null || row.chestIn < best.chestIn ? row : best), null);
}
