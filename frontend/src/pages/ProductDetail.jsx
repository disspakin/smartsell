import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import SizeChartModal from '../components/SizeChartModal';
import { formatBaht, formatPriceRange } from '../utils/price';

// เก็บไว้ว่ากดถูกใจสินค้าไหนไปแล้ว — รีเฟรช/กดซ้ำจะได้ไม่นับ INTERESTED_CLICK เพิ่ม
const LIKED_KEY = 'smartsell_liked_products';
const COUNTED_KEY = 'smartsell_interested_counted';
// ลำดับไซส์สำหรับเรียงปุ่ม ไซส์ที่ไม่อยู่ในนี้จะไปต่อท้าย
const SIZE_ORDER = ['SS', 'XS', 'S', 'M', 'L', 'XL', '2XL', '3XL', '4XL', '5XL'];

const sectionLabelStyle = { fontSize: 13, fontWeight: 700, color: 'var(--ink)', marginBottom: 12 };
const sectionValueStyle = { fontWeight: 500, color: 'var(--text-dim)', marginLeft: 4 };

// 6 วง + ปุ่ม "+N" พอดีแถวเดียวบนมือถือจอ 360px (คอลัมน์กว้างราว 260px)
const COLLAPSED_COLOR_COUNT = 6;
// SS S M L XL แล้วตามด้วย "+N" — 5 ปุ่มพอดีแถวเดียวบนจอ 360px
const COLLAPSED_SIZE_COUNT = 5;

/**
 * ตัดรายการให้เหลือแถวเดียว — ถ้าตัวที่เลือกอยู่หลุดไปอยู่ส่วนที่ซ่อน (เช่นมาจากหน้าสีมงคล)
 * ให้เอามาแทนช่องสุดท้าย ลูกค้าจะได้เห็นว่ากำลังเลือกอะไรอยู่
 */
function collapse(items, limit, expanded, isSelected) {
  if (expanded || items.length <= limit) return items;
  const visible = items.slice(0, limit);
  const selected = items.find(isSelected);
  if (selected !== undefined && !visible.includes(selected)) {
    return [...visible.slice(0, limit - 1), selected];
  }
  return visible;
}

/** ปุ่ม "+N" / "ย่อ" ท้ายแถวสีและไซส์ */
function MoreButton({ expanded, hiddenCount, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-expanded={expanded}
      style={{
        height: 26, padding: '0 10px', borderRadius: 13, cursor: 'pointer',
        border: '1px solid var(--line)', background: '#fff',
        fontSize: 12, fontWeight: 600, color: 'var(--text-dim)', fontFamily: 'inherit',
      }}
    >
      {expanded ? 'ย่อ' : `+${hiddenCount}`}
    </button>
  );
}

/** ปุ่มไซส์ — ทรงแคปซูลแบบเดียวกับปุ่ม "+N" ตัวที่เลือกเน้นแค่ขอบเข้มกับตัวหนา ไซส์ที่หมดเป็นสีเทากดไม่ได้ */
function sizeButtonStyle(selected, soldOut) {
  if (soldOut) {
    return {
      height: 30, minWidth: 38, padding: '0 10px', borderRadius: 15,
      fontSize: 12.5, fontWeight: 500, fontFamily: 'inherit', cursor: 'not-allowed',
      border: '1px dashed var(--line)', background: 'var(--parchment-deep)',
      color: 'var(--text-dim)', textDecoration: 'line-through',
    };
  }
  return {
    height: 30,
    minWidth: 38,
    padding: '0 10px',
    borderRadius: 15,
    fontSize: 12.5,
    fontWeight: selected ? 700 : 500,
    fontFamily: 'inherit',
    cursor: 'pointer',
    border: selected ? '1.5px solid var(--ink)' : '1px solid var(--line)',
    background: '#fff',
    color: 'var(--ink)',
  };
}

const sizeRank = (size) => {
  const i = SIZE_ORDER.indexOf(size);
  return i === -1 ? SIZE_ORDER.length : i;
};

/**
 * ไซส์เริ่มต้นของสีนี้ — M ถ้ามีของ ไม่งั้นไซส์เล็กสุดที่มีของ (ไม่พึ่งลำดับ variants จาก API ที่ไม่แน่นอน)
 * ถ้าทุกไซส์หมด ค่อยเลือกจากไซส์ทั้งหมดแบบเดิม
 */
function defaultSizeFor(variants, color) {
  const ofColor = variants.filter((v) => v.color === color);
  const inStock = ofColor.filter((v) => v.stockQty > 0).map((v) => v.size);
  const sizes = inStock.length > 0 ? inStock : ofColor.map((v) => v.size);
  if (sizes.includes('M')) return 'M';
  return [...sizes].sort((a, b) => sizeRank(a) - sizeRank(b))[0];
}

function loadIds(key) {
  try {
    return JSON.parse(localStorage.getItem(key)) || [];
  } catch {
    return [];
  }
}

export default function ProductDetail() {
  const { id } = useParams();
  // หน้า Lucky Color ส่งสีมงคลมาทาง ?color= ให้เลือกสีนั้นไว้ตั้งแต่เปิดหน้า
  const [searchParams] = useSearchParams();
  const preferredColor = searchParams.get('color');
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [selectedColor, setSelectedColor] = useState(null);
  const [selectedSize, setSelectedSize] = useState(null);
  // สินค้าบางตัวมี 16 สี — แสดงแถวเดียวก่อน กด "+N" ถึงจะเห็นทั้งหมด
  const [showAllColors, setShowAllColors] = useState(false);
  const [showAllSizes, setShowAllSizes] = useState(false);
  const [isSaved, setIsSaved] = useState(() => loadIds(LIKED_KEY).includes(String(id)));
  const [showToast, setShowToast] = useState(false);
  const [toastVisible, setToastVisible] = useState(false);
  // null = สินค้านี้ไม่มีตารางไซส์ (หรือยังโหลดไม่เสร็จ) → ไม่แสดงลิงก์ "ตารางขนาด"
  const [sizeChart, setSizeChart] = useState(null);
  const [showSizeChart, setShowSizeChart] = useState(false);

  useEffect(() => {
    setIsSaved(loadIds(LIKED_KEY).includes(String(id)));
    let sessionId = null;
    try {
      const sessionRaw = localStorage.getItem('smartsell_assistant_session');
      if (sessionRaw) {
        const data = JSON.parse(sessionRaw);
        if (data && data.sessionId) {
          sessionId = data.sessionId;
        }
      }
    } catch (e) {
      console.error(e);
    }

    api.getProduct(id).then((p) => {
      setProduct(p);
      // ไม่มี ?color= หรือสีนั้นไม่มีในสินค้านี้แล้ว ก็ใช้ variant แรกเหมือนเดิม
      const firstVariant = p.variants?.find((v) => v.color === preferredColor) || p.variants?.[0];
      if (firstVariant) {
        setSelectedColor(firstVariant.color);
        setSelectedSize(defaultSizeFor(p.variants, firstVariant.color));
      }
      
      // บันทึกสถิติว่ามีการเข้ามา "VIEW" สินค้านี้
      api.logInteraction({
        sessionId,
        productId: id,
        eventType: 'VIEW'
      }).catch(console.error);

    }).catch(console.error);

    setSizeChart(null);
    api.getSizeChart(id).then(setSizeChart).catch(console.error);
  }, [id]);

  // สีทั้งหมดที่มี (ไม่ซ้ำ) — ใช้ทำปุ่มวงกลมสี
  const colors = useMemo(() => {
    if (!product) return [];
    const seen = new Map();
    product.variants.forEach((v) => {
      if (!seen.has(v.color)) seen.set(v.color, v);
    });
    return Array.from(seen.values());
  }, [product]);

  // ไซส์ทั้งหมดที่มีสำหรับ "สีที่เลือกอยู่ตอนนี้" เท่านั้น
  const sizesForSelectedColor = useMemo(() => {
    if (!product) return [];
    return product.variants
      .filter((v) => v.color === selectedColor)
      .map((v) => v.size);
  }, [product, selectedColor]);

  // ไซส์ของสีที่เลือกที่ stock_qty = 0 — ปุ่มเป็นสีเทากดไม่ได้
  const soldOutSizes = useMemo(() => {
    if (!product) return new Set();
    return new Set(product.variants
      .filter((v) => v.color === selectedColor && v.stockQty === 0)
      .map((v) => v.size));
  }, [product, selectedColor]);

  // variant ปัจจุบันที่ตรงกับทั้งสีและไซส์ที่เลือก
  const currentVariant = useMemo(() => {
    if (!product) return null;
    return product.variants.find((v) => v.color === selectedColor && v.size === selectedSize)
      || product.variants.find((v) => v.color === selectedColor);
  }, [product, selectedColor, selectedSize]);

  // ราคาตามไซส์: ใช้ effectivePrice ของ variant ที่ตรงทั้งสีและไซส์เท่านั้น
  // (currentVariant ถอยไปใช้ variant แรกของสีได้ ซึ่งอาจเป็นคนละไซส์ ราคาจะผิด) — ยังไม่ได้เลือกไซส์ให้แสดงเป็นช่วง
  const selectedVariant = useMemo(() => {
    if (!product || !selectedSize) return null;
    return product.variants.find((v) => v.color === selectedColor && v.size === selectedSize) || null;
  }, [product, selectedColor, selectedSize]);

  function handleColorClick(color) {
    setSelectedColor(color);
    // ถ้าไซส์เดิมไม่มีหรือหมดในสีใหม่ ให้สลับไปไซส์ที่มีของของสีนั้นแทน
    const availableSizes = product.variants
      .filter((v) => v.color === color && v.stockQty > 0)
      .map((v) => v.size);
    if (!availableSizes.includes(selectedSize)) {
      setSelectedSize(defaultSizeFor(product.variants, color));
    }
  }

  // useCallback ให้ฟังก์ชันเดิมทุก render — modal ใช้เป็น dependency ของ useEffect ที่ผูกปุ่ม Esc
  const closeSizeChart = useCallback(() => setShowSizeChart(false), []);

  function handleSaveClick() {
    const willSave = !isSaved;
    setIsSaved(willSave);

    const liked = loadIds(LIKED_KEY).filter((x) => x !== String(id));
    localStorage.setItem(LIKED_KEY, JSON.stringify(willSave ? [...liked, String(id)] : liked));

    // นับ INTERESTED_CLICK แค่ครั้งแรกของสินค้านี้ — ยกเลิกแล้วกดใหม่ไม่นับซ้ำ
    const counted = loadIds(COUNTED_KEY);
    const shouldLog = willSave && !counted.includes(String(id));
    if (shouldLog) localStorage.setItem(COUNTED_KEY, JSON.stringify([...counted, String(id)]));

    if (shouldLog) {
      let sessionId = null;
      try {
        const sessionRaw = localStorage.getItem('smartsell_assistant_session');
        if (sessionRaw) {
          const data = JSON.parse(sessionRaw);
          if (data && data.sessionId) {
            sessionId = data.sessionId;
          }
        }
      } catch (e) {
        console.error(e);
      }

      // บันทึกสถิติเมื่อลูกค้ากดถูกใจ (สนใจ)
      api.logInteraction({
        sessionId,
        productId: id,
        eventType: 'INTERESTED_CLICK'
      }).catch(console.error);
    }

    if (willSave) {
      // ขั้นตอนเข้า: mount ก่อน แล้วค่อย trigger transition ให้ fade เข้า
      setShowToast(true);
      requestAnimationFrame(() => setToastVisible(true));

      // ขั้นตอนออก: fade ออกก่อน แล้วค่อย unmount จริง
      setTimeout(() => setToastVisible(false), 1800);
      setTimeout(() => setShowToast(false), 2100);
    }
  }

  if (!product) return <div className="app" style={{ padding: 32 }}>กำลังโหลด...</div>;

  const availableSizes = [...new Set(sizesForSelectedColor)].sort((a, b) => sizeRank(a) - sizeRank(b));

  const visibleColors = collapse(colors, COLLAPSED_COLOR_COUNT, showAllColors,
    (v) => v.color === selectedColor);
  const sizeLimit = COLLAPSED_SIZE_COUNT;
  const visibleSizes = collapse(availableSizes, sizeLimit, showAllSizes, (size) => size === selectedSize);

  return (
    <div className="app" style={{ padding: '36px 32px 60px' }}>
      <span
        role="link"
        tabIndex={0}
        style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-dim)', cursor: 'pointer' }}
        onClick={() => navigate('/products')}
        onKeyDown={(e) => { if (e.key === 'Enter') navigate('/products'); }}
      >
        ‹ กลับไปหน้าสินค้า
      </span>

      <div className="pd-layout">
        <div className="pd-image-box">
          {currentVariant?.imageUrl
            ? <img src={currentVariant.imageUrl} alt={product.name} style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
            : <span style={{ color: 'var(--text-dim)' }}>IMAGE</span>}
        </div>

        <div>
          <h1 style={{ fontSize: 22, lineHeight: 1.4, marginBottom: 6 }}>{product.name}</h1>
          <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--ink)', marginBottom: 28 }}>
            {selectedVariant?.effectivePrice != null ? `${formatBaht(selectedVariant.effectivePrice)} บาท` : formatPriceRange(product)}
          </div>

          {/* ===== เลือกสี ===== */}
          <div style={sectionLabelStyle}>
            สี <span style={sectionValueStyle}>· {currentVariant?.color || '-'}</span>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 8, marginBottom: 26 }}>
            {visibleColors.map((v) => {
              const isSelected = selectedColor === v.color;
              return (
                <button
                  key={v.color}
                  type="button"
                  onClick={() => handleColorClick(v.color)}
                  aria-pressed={isSelected}
                  aria-label={`สี${v.color}`}
                  title={v.color}
                  style={{
                    width: 26, height: 26, padding: 0, borderRadius: '50%', cursor: 'pointer',
                    border: 'none', flexShrink: 0,
                    background: v.colorHex || '#ccc',
                    boxShadow: isSelected
                      ? '0 0 0 2px #fff, 0 0 0 3.5px var(--ink)'
                      : 'inset 0 0 0 1px rgba(0,0,0,.14)',
                  }}
                />
              );
            })}
            {colors.length > COLLAPSED_COLOR_COUNT && (
              <MoreButton
                expanded={showAllColors}
                hiddenCount={colors.length - visibleColors.length}
                onClick={() => setShowAllColors((v) => !v)}
              />
            )}
          </div>

          {/* ===== เลือกไซส์ — แสดงเฉพาะไซส์ที่สีนี้มีของ แถวเดียวเหมือนสี ===== */}
          {/* หัวข้อไซส์ + ปุ่มตารางขนาดชิดขวาในแถวเดียวกัน (ปุ่มแสดงเฉพาะสินค้าที่มี size chart) */}
          <div style={{ ...sectionLabelStyle, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
            <span>ไซส์ <span style={sectionValueStyle}>· {selectedSize || '-'}</span></span>
            {sizeChart && (
              <button type="button" className="pd-size-chart-btn" onClick={() => setShowSizeChart(true)}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <rect x="2" y="7" width="20" height="10" rx="2" />
                  <path d="M6 7v3M10 7v4M14 7v3M18 7v4" />
                </svg>
                ตารางขนาด
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M9 6l6 6-6 6" />
                </svg>
              </button>
            )}
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 6, marginBottom: 28 }}>
            {visibleSizes.map((size) => {
              const isSelected = selectedSize === size;
              const soldOut = soldOutSizes.has(size);
              return (
                <button
                  key={size}
                  type="button"
                  onClick={() => setSelectedSize(size)}
                  disabled={soldOut}
                  aria-pressed={isSelected}
                  aria-label={soldOut ? `ไซส์ ${size} สินค้าหมด` : undefined}
                  title={soldOut ? 'สินค้าหมด' : undefined}
                  style={sizeButtonStyle(isSelected, soldOut)}
                >
                  {size}
                </button>
              );
            })}
            {availableSizes.length > sizeLimit && (
              <MoreButton
                expanded={showAllSizes}
                hiddenCount={availableSizes.length - visibleSizes.length}
                onClick={() => setShowAllSizes((v) => !v)}
              />
            )}
          </div>

          <div
            onClick={handleSaveClick}
            // ยังไม่กดถูกใจ = ปุ่มสีน้ำเงินชวนกด / บันทึกแล้ว = ปุ่มขาวขอบเทา
            className={isSaved ? 'btn ghost' : 'btn'}
            style={{
              display: 'inline-flex', width: '100%', justifyContent: 'center',
              background: isSaved ? '#fff' : 'var(--accent)',
              color: isSaved ? 'var(--ink)' : '#fff',
            }}
          >
            {isSaved ? '❤️ บันทึกไว้ในรายการที่ชอบแล้ว' : '🤍 กดถูกใจสินค้า (สนใจสินค้าชิ้นนี้)'}
          </div>
        </div>
      </div>

      {showSizeChart && sizeChart && (
        <SizeChartModal
          chart={sizeChart}
          selectedSize={selectedSize}
          // ไซส์ที่สีนี้ไม่มีขาย หรือมีแต่ของหมด แนะนำได้แต่กดเลือกไม่ได้
          isSoldOut={(size) => !availableSizes.includes(size) || soldOutSizes.has(size)}
          onPickSize={(size) => { setSelectedSize(size); setShowSizeChart(false); }}
          onClose={closeSizeChart}
        />
      )}

      {/* Toast แจ้งเตือนบันทึกสำเร็จ */}
      {showToast && (
        <div style={{
          position: 'fixed', top: 20, left: '50%',
          transform: toastVisible ? 'translate(-50%, 0)' : 'translate(-50%, -20px)',
          opacity: toastVisible ? 1 : 0,
          transition: 'opacity .3s ease, transform .3s ease',
          background: 'var(--ink)', color: '#fff', fontSize: 13, fontWeight: 600,
          padding: '12px 22px', borderRadius: 30, zIndex: 2000,
          boxShadow: '0 10px 24px rgba(0,0,0,.25)',
          whiteSpace: 'nowrap',
        }}>
          ❤️ บันทึกไว้ในรายการที่ชอบแล้ว
        </div>
      )}
    </div>
  );
}