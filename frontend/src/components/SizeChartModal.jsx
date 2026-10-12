import { useEffect, useRef, useState } from 'react';
import SizeRecommender from './SizeRecommender';

const CM_PER_INCH = 2.54;

/** ตัวเลขจาก DB เป็นนิ้วเสมอ — แปลงเป็นซม. ปัดทศนิยม 1 ตำแหน่ง */
function formatMeasure(inches, unit) {
  if (unit === 'cm') return (Math.round(inches * CM_PER_INCH * 10) / 10).toFixed(1);
  return Number.isInteger(inches) ? String(inches) : inches.toFixed(1);
}

const thStyle = {
  padding: '10px 14px', textAlign: 'left', fontSize: 12, fontWeight: 700,
  color: 'var(--text-dim)', borderBottom: '1.5px solid var(--line)', whiteSpace: 'nowrap',
};
const tdStyle = { padding: '10px 14px', fontSize: 13.5, borderBottom: '1px solid var(--line)', whiteSpace: 'nowrap' };

/** ปุ่มสลับหน่วย นิ้ว / ซม. */
function UnitToggle({ unit, onChange }) {
  const option = (value, label) => (
    <button
      type="button"
      onClick={() => onChange(value)}
      aria-pressed={unit === value}
      style={{
        height: 28, padding: '0 14px', border: 'none', borderRadius: 14, cursor: 'pointer',
        fontSize: 12.5, fontWeight: 600, fontFamily: 'inherit',
        background: unit === value ? 'var(--ink)' : 'transparent',
        color: unit === value ? '#fff' : 'var(--text-dim)',
      }}
    >
      {label}
    </button>
  );
  return (
    // userSelect: none — ดับเบิลคลิกสลับหน่วยเร็วๆ แล้วข้อความบนปุ่มไม่ถูกคลุมสี
    <div style={{ display: 'inline-flex', padding: 2, borderRadius: 16, background: 'var(--parchment-deep)', userSelect: 'none', WebkitUserSelect: 'none' }}>
      {option('inch', 'นิ้ว')}
      {option('cm', 'ซม.')}
    </div>
  );
}

/** รูปวิธีวัด: เสื้อหน้าตรง เส้นแนวนอนใต้แขน = รอบอก, เส้นแนวตั้งจากไหล่ถึงชายเสื้อ = ความยาว
 *  isPolo = วาดเป็นเสื้อมีปกและกระดุม แทนเสื้อยืดคอกลม */
function MeasureGuide({ isPolo }) {
  const shirtStyle = { fill: 'var(--parchment-deep)', stroke: 'var(--text-dim)', strokeWidth: 2, strokeLinejoin: 'round' };
  return (
    <svg viewBox="0 0 200 200" width="150" height="150" role="img" aria-label="วิธีวัดรอบอกและความยาวเสื้อ">
      <defs>
        <marker id="size-chart-arrow" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
          <path d="M0,0 L10,5 L0,10 z" fill="var(--accent)" />
        </marker>
      </defs>
      {isPolo ? (
        <>
          {/* ตัวเสื้อคอปก: คอเป็นร่อง V ให้ปกสองข้างวางทับ */}
          <path d="M70,20 L100,36 L130,20 L168,34 L192,78 L164,90 L158,72 L158,185 L42,185 L42,72 L36,90 L8,78 L32,34 Z" {...shirtStyle} />
          {/* สาบกระดุม + กระดุม 2 เม็ด */}
          <rect x="95" y="36" width="10" height="34" {...shirtStyle} fill="#fff" strokeWidth="1.5" />
          <circle cx="100" cy="48" r="2.2" fill="var(--text-dim)" />
          <circle cx="100" cy="61" r="2.2" fill="var(--text-dim)" />
          {/* ปกเสื้อซ้าย/ขวา */}
          <path d="M70,20 Q100,10 130,20" fill="none" stroke="var(--text-dim)" strokeWidth="2" />
          <path d="M70,20 L100,37 L80,50 Z" {...shirtStyle} fill="#fff" />
          <path d="M130,20 L100,37 L120,50 Z" {...shirtStyle} fill="#fff" />
        </>
      ) : (
        <path d="M70,20 Q100,40 130,20 L168,34 L192,78 L164,90 L158,72 L158,185 L42,185 L42,72 L36,90 L8,78 L32,34 Z" {...shirtStyle} />
      )}
      {/* รอบอก: วัดใต้รักแร้ ตรงที่กว้างที่สุดของลำตัว — เป็นค่ารอบตัว ไม่ใช่ความกว้างวางราบ จึงเขียนกำกับไว้ */}
      <line x1="46" y1="84" x2="154" y2="84" stroke="var(--accent)" strokeWidth="2" markerStart="url(#size-chart-arrow)" markerEnd="url(#size-chart-arrow)" />
      <text x="88" y="100" textAnchor="middle" fontSize="12" fontWeight="700" fill="var(--accent)">รอบอก</text>
      <text x="88" y="113" textAnchor="middle" fontSize="10" fontWeight="600" fill="var(--accent)">(วัดรอบตัวเสื้อ)</text>
      {/* ความยาว: จากจุดสูงสุดของไหล่ข้างคอลงไปถึงชายเสื้อ */}
      <line x1="134" y1="24" x2="134" y2="181" stroke="var(--accent)" strokeWidth="2" markerStart="url(#size-chart-arrow)" markerEnd="url(#size-chart-arrow)" />
      <text x="128" y="140" textAnchor="end" fontSize="12" fontWeight="700" fill="var(--accent)">ความยาว</text>
    </svg>
  );
}

/**
 * Modal "ตารางขนาด" — เปิดจากลิงก์ใต้ปุ่มไซส์ในหน้าสินค้า
 * chart มาจาก GET /api/products/{id}/size-chart (หน่วยนิ้ว เรียงตาม sort_order มาแล้ว)
 */
export default function SizeChartModal({ chart, selectedSize, isSoldOut, onPickSize, onClose }) {
  const [unit, setUnit] = useState('inch');
  const closeRef = useRef(null);

  // กด Esc ปิดได้ และล็อกไม่ให้หน้าหลังเลื่อนตามตอน modal เปิด
  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', onKey);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeRef.current?.focus();
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = prevOverflow;
    };
  }, [onClose]);

  const unitLabel = unit === 'cm' ? 'ซม.' : 'นิ้ว';

  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed', inset: 0, zIndex: 3000, background: 'rgba(15,27,45,.45)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16,
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="size-chart-title"
        onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%', maxWidth: 520, maxHeight: '90vh', overflowY: 'auto',
          background: '#fff', borderRadius: 14, padding: '20px 20px 24px',
          boxShadow: '0 20px 50px rgba(0,0,0,.25)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12, marginBottom: 4 }}>
          <div>
            <h2 id="size-chart-title" style={{ fontSize: 18, margin: 0 }}>ตารางขนาด</h2>
            <div style={{ fontSize: 12.5, color: 'var(--text-dim)', marginTop: 4 }}>{chart.name}</div>
          </div>
          <button
            ref={closeRef}
            type="button"
            onClick={onClose}
            aria-label="ปิดตารางขนาด"
            style={{
              width: 32, height: 32, borderRadius: '50%', border: 'none', cursor: 'pointer',
              background: 'var(--parchment-deep)', fontSize: 18, lineHeight: 1, color: 'var(--ink)',
            }}
          >
            ×
          </button>
        </div>

        <div style={{ display: 'flex', justifyContent: 'center', margin: '8px 0 4px' }}>
          <MeasureGuide isPolo={chart.name.startsWith('Polo')} />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', margin: '8px 0 10px' }}>
          <span style={{ fontSize: 12, color: 'var(--text-dim)' }}>หน่วย: {unitLabel}</span>
          <UnitToggle unit={unit} onChange={setUnit} />
        </div>

        {/* จอแคบ ตารางเลื่อนแนวนอนในกล่องนี้ได้ ไม่ดันทั้ง modal ให้กว้างเกินจอ */}
        <div style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
          <table style={{ width: '100%', minWidth: 300, borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                <th style={thStyle}>ไซส์</th>
                <th style={thStyle}>รอบอก ({unitLabel})</th>
                <th style={thStyle}>ความยาว ({unitLabel})</th>
              </tr>
            </thead>
            <tbody>
              {chart.rows.map((row) => {
                const isSelected = row.size === selectedSize;
                return (
                  <tr
                    key={row.size}
                    aria-current={isSelected ? 'true' : undefined}
                    style={{ background: isSelected ? 'var(--parchment-deep)' : 'transparent' }}
                  >
                    <td style={{ ...tdStyle, fontWeight: 700, color: 'var(--ink)', boxShadow: isSelected ? 'inset 3px 0 0 var(--accent)' : 'none' }}>
                      {row.size}
                      {isSelected && <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--accent)', marginLeft: 6 }}>ที่เลือก</span>}
                    </td>
                    <td style={{ ...tdStyle, fontWeight: isSelected ? 700 : 400 }}>{formatMeasure(row.chestIn, unit)}</td>
                    <td style={{ ...tdStyle, fontWeight: isSelected ? 700 : 400 }}>{formatMeasure(row.lengthIn, unit)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <div style={{ fontSize: 11.5, color: 'var(--text-dim)', marginTop: 8 }}>
          * ขนาดตัวเสื้อ ไม่ใช่ขนาดตัวคน อาจคลาดเคลื่อน ±1 นิ้ว
        </div>

        <SizeRecommender rows={chart.rows} unit={unit} isSoldOut={isSoldOut} onPick={onPickSize} />
      </div>
    </div>
  );
}
