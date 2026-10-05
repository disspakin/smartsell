import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ToolCard from './ToolCard';

const SEASONS = [
  { name: 'Spring', colors: ['#E8A27C', '#E8C65A', '#9CC98A'] },
  { name: 'Summer', colors: ['#BFC9E0', '#D9AFC4', '#9AA8B8'] },
  { name: 'Autumn', colors: ['#A8642E', '#7D7A3A', '#C49A4E'] },
  { name: 'Winter', colors: ['#23358F', '#C0265A', '#1A1A1A'] },
];

const SIZES = ['S', 'M', 'L', 'XL'];

// รูปร่าง 5 แบบ วาดเป็นเส้นใน viewBox 24x40
const BODY_SHAPES = [
  { key: 'hourglass', label: 'นาฬิกาทราย', d: 'M5 4 H19 L13 20 L19 36 H5 L11 20 Z', active: true },
  { key: 'pear', label: 'สามเหลี่ยม', d: 'M9 4 H15 L20 36 H4 Z' },
  { key: 'inverted', label: 'สามเหลี่ยมกลับ', d: 'M4 4 H20 L15 36 H9 Z' },
  { key: 'rectangle', label: 'สี่เหลี่ยม', d: 'M7 4 H17 V36 H7 Z' },
  { key: 'oval', label: 'วงรี', d: 'M12 4 C18 4 19 13 19 20 C19 27 18 36 12 36 C6 36 5 27 5 20 C5 13 6 4 12 4 Z' },
];

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
  body: (
    <svg {...svgProps}>
      <circle cx="12" cy="4.5" r="2" /><path d="M8 9h8l-1.5 5.5L16 21M8 9l1.5 5.5L8 21M10 14.5h4" />
    </svg>
  ),
};

/** ปุ่มคู่ใต้ hero */
export function HomeQuickActions() {
  const navigate = useNavigate();
  return (
    <div className="home-quick-actions">
      <button type="button" className="home-quick-btn" onClick={() => navigate('/products')}>
        {icons.bag} ดูสินค้าทั้งหมด
      </button>
      <button type="button" className="home-quick-btn" onClick={() => navigate('/lucky-color')}>
        {icons.sun} สีมงคลประจำวัน
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
        <p>3 เครื่องมือที่ช่วยให้ทุกตัวที่ซื้อ ใส่แล้วพอดีและดูดีจริง</p>
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
          description="กรอกส่วนสูง น้ำหนัก รอบอก ระบบเทียบกับตารางไซซ์ของแต่ละสินค้า แล้วบอกไซซ์ที่พอดีที่สุด"
          linkText="หาไซซ์ของฉัน"
          to="/size-chart"
        >
          <div className="tp-fields">
            <div className="tp-field"><label>ส่วนสูง (ซม.)</label><span>170</span></div>
            <div className="tp-field"><label>น้ำหนัก (กก.)</label><span>62</span></div>
          </div>
          <div className="tp-sizes">
            {SIZES.map((size) => (
              <span key={size} className={size === 'M' ? 'active' : ''}>{size}</span>
            ))}
          </div>
          <p className="tp-note">ไซซ์ที่แนะนำ: <b>M</b> · ใส่สบาย ไม่รัด</p>
        </ToolCard>

        <ToolCard
          number="03"
          icon={icons.body}
          title="Body Analysis"
          description="กรอกสัดส่วนหรือเลือกรูปร่างที่ใกล้เคียง ระบบแนะนำทรงเสื้อและกางเกงที่ช่วยเสริมรูปร่างคุณ"
          linkText="วิเคราะห์รูปร่าง"
          to="/body-analysis"
        >
          <div className="tp-shapes">
            {BODY_SHAPES.map((shape) => (
              <div key={shape.key} className={`tp-shape ${shape.active ? 'active' : ''}`}>
                <svg viewBox="0 0 24 40" width="18" height="30" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round">
                  <path d={shape.d} />
                </svg>
                <span>{shape.label}</span>
              </div>
            ))}
          </div>
        </ToolCard>
      </div>

      <div className="home-ai-banner">
        <div>
          <h3>ทำครบ 3 อย่าง แล้วให้ AI จัดชุดให้</h3>
          <p>บอกว่าวันนี้จะไปไหน ผู้ช่วย AI จะเลือกจากสินค้าที่ตรงสี ตรงไซซ์ และเข้ากับรูปร่างคุณ</p>
        </div>
        <button type="button" className="home-ai-banner-btn" onClick={() => navigate('/assistant')}>
          {icons.sparkle} คุยกับผู้ช่วย AI
        </button>
      </div>
    </div>
  );
}
