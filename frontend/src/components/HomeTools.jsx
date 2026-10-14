import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ToolCard from './ToolCard';
import { EASE_INCHES, recommendSize } from './sizeRecommendation';

const SEASONS = [
  { name: 'Spring', colors: ['#E8A27C', '#E8C65A', '#9CC98A'] },
  { name: 'Summer', colors: ['#BFC9E0', '#D9AFC4', '#9AA8B8'] },
  { name: 'Autumn', colors: ['#A8642E', '#7D7A3A', '#C49A4E'] },
  { name: 'Winter', colors: ['#23358F', '#C0265A', '#1A1A1A'] },
];

const CM_PER_INCH = 2.54;

// ตัวอย่างสำหรับเดโมในการ์ดเท่านั้น — ค่าเดียวกับ size chart "T-Shirt คอกลม" ใน DB (หน่วยนิ้ว)
// ถ้าร้านแก้ตารางนี้ใน DB ให้แก้ตรงนี้ตาม; ตารางจริงของแต่ละสินค้าอยู่ใน modal หน้าสินค้า
const DEMO_ROWS = [
  { size: 'SS', chestIn: 36, lengthIn: 25 },
  { size: 'S', chestIn: 38, lengthIn: 26 },
  { size: 'M', chestIn: 40, lengthIn: 27 },
  { size: 'L', chestIn: 42, lengthIn: 28 },
  { size: 'XL', chestIn: 44, lengthIn: 29 },
  { size: '2XL', chestIn: 46, lengthIn: 30 },
  { size: '3XL', chestIn: 48, lengthIn: 31 },
  { size: '4XL', chestIn: 50, lengthIn: 32 },
];

function toUnit(inches, unit) {
  return unit === 'cm' ? Math.round(inches * CM_PER_INCH) : inches;
}

const svgProps = {
  width: 18, height: 18, viewBox: '0 0 24 24', fill: 'none',
  stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round',
};

const icons = {
  bag: (
    <svg {...svgProps} width={16} height={16}>
      <path d="M6 7h12l-1 13H7z" /><path d="M9 7V5.5a3 3 0 0 1 6 0V7" />
    </svg>
  ),
  sun: (
    <svg {...svgProps} width={16} height={16}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  ),
  sparkle: (
    <svg {...svgProps} width={14} height={14}>
      <path d="M12 3l1.9 5.6L19.5 10.5l-5.6 1.9L12 18l-1.9-5.6L4.5 10.5l5.6-1.9z" />
    </svg>
  ),
  palette: (
    <svg {...svgProps}>
      <path d="M12 3a9 9 0 1 0 0 18c1.1 0 1.7-.8 1.7-1.7 0-.5-.2-.9-.5-1.2-.3-.3-.5-.7-.5-1.2 0-.9.8-1.7 1.7-1.7H16a5 5 0 0 0 5-5c0-4-4-7.2-9-7.2z" />
      <circle cx="7.5" cy="11" r="1" /><circle cx="10.5" cy="7.5" r="1" /><circle cx="15" cy="8" r="1" />
    </svg>
  ),
  ruler: (
    <svg {...svgProps}>
      <rect x="2.5" y="8" width="19" height="8" rx="1.5" /><path d="M6.5 8v3M10 8v4M13.5 8v3M17 8v4" />
    </svg>
  ),
};

/** เดโมในการ์ด Size Chart: กรอกรอบอก → ไฮไลต์ไซซ์ที่แนะนำ (logic เดียวกับ modal ตารางขนาด), กดไซซ์เพื่อดูขนาดเสื้อ */
function SizeChartDemo() {
  const [unit, setUnit] = useState('inch');
  const [input, setInput] = useState('37');
  const [picked, setPicked] = useState(null);

  const unitLabel = unit === 'cm' ? 'ซม.' : 'นิ้ว';
  const value = Number(input);
  const hasValue = input.trim() !== '' && Number.isFinite(value) && value > 0;
  const recommended = hasValue ? recommendSize(DEMO_ROWS, unit === 'cm' ? value / CM_PER_INCH : value) : null;
  const shown = picked ? DEMO_ROWS.find((r) => r.size === picked) : null;

  function switchUnit(next) {
    if (next === unit) return;
    // แปลงเลขที่กรอกไว้ตามหน่วยใหม่ (37 นิ้ว → 94 ซม.) ไม่ใช่ตีความเลขเดิมเป็นหน่วยใหม่
    if (hasValue) setInput(String(Math.round(next === 'cm' ? value * CM_PER_INCH : value / CM_PER_INCH)));
    setUnit(next);
  }

  let note;
  if (shown) {
    note = <>ไซซ์ <b>{shown.size}</b> · รอบอกเสื้อ {toUnit(shown.chestIn, unit)} ยาว {toUnit(shown.lengthIn, unit)} {unitLabel}</>;
  } else if (!hasValue) {
    note = 'กรอกรอบอกของคุณเพื่อดูไซซ์ที่แนะนำ';
  } else if (!recommended) {
    note = 'ไม่มีไซซ์ที่พอดี';
  } else {
    note = <>ไซซ์ที่แนะนำ: <b>{recommended.size}</b> · รอบอกเสื้อ {toUnit(recommended.chestIn, unit)} {unitLabel}</>;
  }

  return (
    <>
      <div className="tp-fields">
        <div className="tp-field">
          <label htmlFor="home-size-demo-chest">รอบอกของคุณ ({unitLabel})</label>
          <input
            id="home-size-demo-chest"
            type="number"
            inputMode="decimal"
            min="0"
            value={input}
            onChange={(e) => { setInput(e.target.value); setPicked(null); }}
          />
        </div>
        <div className="tp-field">
          <label>หน่วย · เผื่อหลวม {unit === 'cm' ? `${Math.round(EASE_INCHES * CM_PER_INCH)} ซม.` : `${EASE_INCHES} นิ้ว`}</label>
          <div className="tp-units">
            {[['inch', 'นิ้ว'], ['cm', 'ซม.']].map(([key, label]) => (
              <button key={key} type="button" aria-pressed={unit === key} className={unit === key ? 'active' : ''} onClick={() => switchUnit(key)}>
                {label}
              </button>
            ))}
          </div>
        </div>
      </div>
      <div className="tp-sizes">
        {DEMO_ROWS.map((row) => (
          <button
            key={row.size}
            type="button"
            className={[row.size === recommended?.size ? 'active' : '', row.size === picked ? 'picked' : ''].join(' ').trim()}
            aria-pressed={row.size === picked}
            onClick={() => setPicked(row.size === picked ? null : row.size)}
          >
            {row.size}
          </button>
        ))}
      </div>
      <p className="tp-note" aria-live="polite">{note}</p>
    </>
  );
}

/** ปุ่มคู่ใต้ hero */
export function HomeQuickActions() {
  const navigate = useNavigate();
  return (
    <div className="home-quick-actions">
      <button type="button" className="home-quick-btn" onClick={() => navigate('/products')}>
        {icons.bag} ดูสินค้าทั้งหมด
      </button>
      <button type="button" className="home-quick-btn" onClick={() => navigate('/lucky-color')}>
        {icons.sun} สีมงคลประจำวันเกิด
      </button>
    </div>
  );
}

/** section "รู้จักตัวเองก่อนเลือกซื้อ" + แบนเนอร์ AI */
export default function HomeTools() {
  const navigate = useNavigate();
  const [season, setSeason] = useState('Summer');
  return (
    <div className="home-tools">
      <div className="home-tools-heading">
        <h2>รู้จักตัวเองก่อนเลือกซื้อ</h2>
        <p>2 เครื่องมือที่ช่วยให้ทุกตัวที่ซื้อ ใส่แล้วพอดีและดูดีจริง</p>
      </div>

      <div className="home-tools-grid">
        <ToolCard
          number="01"
          icon={icons.palette}
          title="Personal Color Check"
          description="ตอบคำถามสั้น ๆ 6 ข้อ ระบบหาโทนผิวและซีซันสีที่ทำให้หน้าคุณดูสว่างขึ้น"
          linkText="เช็กสีของฉัน"
          to="/personal-color"
        >
          <ul className="tp-seasons">
            {SEASONS.map((s) => (
              <li key={s.name}>
                <button
                  type="button"
                  className={season === s.name ? 'selected' : ''}
                  aria-pressed={season === s.name}
                  onClick={() => setSeason(s.name)}
                >
                  <span>{s.name}</span>
                  <span className="tp-dots">
                    {s.colors.map((c) => <span key={c} style={{ background: c }} />)}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </ToolCard>

        <ToolCard
          number="02"
          icon={icons.ruler}
          title="Size Chart อัจฉริยะ"
          description="กด “ตารางขนาด” ในหน้าสินค้า ดูรอบอกและความยาวทุกไซซ์ เป็นนิ้วหรือ ซม. แล้วกรอกรอบอกของคุณ ระบบเผื่อหลวมให้ 2 นิ้วและแนะนำไซซ์ที่พอดี"
          linkText="เลือกสินค้าแล้วดูไซซ์"
          to="/products"
        >
          <SizeChartDemo />
        </ToolCard>
      </div>

      <div className="home-ai-banner">
        <div>
          <h3>ทำครบ 2 อย่าง แล้วให้ AI จัดชุดให้</h3>
          <p>บอกว่าวันนี้จะไปไหน ผู้ช่วย AI จะเลือกจากสินค้าที่ตรงสีและตรงไซซ์ของคุณ</p>
        </div>
        <button type="button" className="home-ai-banner-btn" onClick={() => navigate('/assistant')}>
          {icons.sparkle} คุยกับผู้ช่วย AI
        </button>
      </div>
    </div>
  );
}
