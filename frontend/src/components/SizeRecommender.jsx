import { useState } from 'react';
import { EASE_INCHES, recommendSize } from './sizeRecommendation';

const CM_PER_INCH = 2.54;

/**
 * ช่องแนะนำไซส์ในตารางขนาด — ลูกค้ากรอกรอบอกตัวเองตามหน่วยที่เลือกอยู่ใน modal (นิ้ว/ซม.)
 * แล้วแนะนำไซส์ที่เผื่อหลวม 2 นิ้ว พร้อมปุ่มเลือกไซส์นั้นในหน้าสินค้าเลย
 */
export default function SizeRecommender({ rows, unit, isSoldOut, onPick }) {
  const [input, setInput] = useState('');
  const [inputUnit, setInputUnit] = useState(unit);

  // ลูกค้าสลับหน่วยหลังกรอกแล้ว — แปลงตัวเลขที่กรอกไว้ตามไปด้วย (36 นิ้ว → 91.4 ซม.) ไม่ใช่ตีความเลขเดิมเป็นหน่วยใหม่
  if (inputUnit !== unit) {
    setInputUnit(unit);
    const typed = Number(input);
    if (input.trim() !== '' && Number.isFinite(typed)) {
      const converted = unit === 'cm' ? typed * CM_PER_INCH : typed / CM_PER_INCH;
      setInput(String(Math.round(converted * 10) / 10));
    }
  }

  const value = Number(input);
  const hasValue = input.trim() !== '' && Number.isFinite(value) && value > 0;
  const bodyChestInches = unit === 'cm' ? value / CM_PER_INCH : value;
  const recommended = hasValue ? recommendSize(rows, bodyChestInches) : null;
  const soldOut = recommended && isSoldOut(recommended.size);

  return (
    <div style={{ marginTop: 20, padding: 16, borderRadius: 12, background: 'var(--parchment-deep)' }}>
      <label htmlFor="size-recommender-chest" style={{ display: 'block', fontSize: 13.5, fontWeight: 700, color: 'var(--ink)' }}>
        ไม่แน่ใจไซส์? กรอกรอบอกของคุณ
      </label>
      <div style={{ fontSize: 12, color: 'var(--text-dim)', margin: '4px 0 10px' }}>
        วัดรอบตัวตรงส่วนที่กว้างที่สุดของหน้าอก เราจะเผื่อหลวมให้ {unit === 'cm' ? `${Math.round(EASE_INCHES * CM_PER_INCH)} ซม.` : `${EASE_INCHES} นิ้ว`}
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <input
          id="size-recommender-chest"
          type="number"
          inputMode="decimal"
          min="0"
          step="0.1"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={unit === 'cm' ? 'เช่น 90' : 'เช่น 36'}
          style={{
            width: 120, height: 36, padding: '0 12px', borderRadius: 8, border: '1px solid var(--line)',
            fontSize: 14, fontFamily: 'inherit', background: '#fff',
          }}
        />
        <span style={{ fontSize: 13, color: 'var(--text-dim)' }}>{unit === 'cm' ? 'ซม.' : 'นิ้ว'}</span>
      </div>

      {/* aria-live ให้โปรแกรมอ่านหน้าจออ่านผลใหม่ทุกครั้งที่ตัวเลขเปลี่ยน */}
      <div aria-live="polite" style={{ marginTop: 12, fontSize: 13.5 }}>
        {hasValue && recommended && (
          <div style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
            <span>ไซส์ที่แนะนำ: <b style={{ fontSize: 16, color: 'var(--accent)' }}>{recommended.size}</b></span>
            {soldOut
              ? <span style={{ fontSize: 12, color: 'var(--text-dim)' }}>(ไซส์นี้หมดในสีที่เลือก)</span>
              : (
                <button
                  type="button"
                  onClick={() => onPick(recommended.size)}
                  className="btn primary"
                  style={{ height: 30, padding: '0 14px', fontSize: 12.5 }}
                >
                  เลือกไซส์ {recommended.size}
                </button>
              )}
          </div>
        )}
        {hasValue && !recommended && (
          <span style={{ color: 'var(--text-dim)' }}>ไม่มีไซส์ที่พอดี</span>
        )}
        {input.trim() !== '' && !hasValue && (
          <span style={{ color: '#b45309' }}>กรอกเป็นตัวเลขที่มากกว่า 0</span>
        )}
      </div>
    </div>
  );
}
