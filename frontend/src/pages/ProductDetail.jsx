import { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api/client';

export default function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [selectedColor, setSelectedColor] = useState(null);
  const [selectedSize, setSelectedSize] = useState(null);
  const [isSaved, setIsSaved] = useState(false);
  const [showToast, setShowToast] = useState(false);
  const [toastVisible, setToastVisible] = useState(false);

  useEffect(() => {
    api.getProduct(id).then((p) => {
      setProduct(p);
      const firstVariant = p.variants?.[0];
      if (firstVariant) {
        setSelectedColor(firstVariant.color);
        setSelectedSize(firstVariant.size);
      }
    }).catch(console.error);
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

  // variant ปัจจุบันที่ตรงกับทั้งสีและไซส์ที่เลือก
  const currentVariant = useMemo(() => {
    if (!product) return null;
    return product.variants.find((v) => v.color === selectedColor && v.size === selectedSize)
      || product.variants.find((v) => v.color === selectedColor);
  }, [product, selectedColor, selectedSize]);

  function handleColorClick(color) {
    setSelectedColor(color);
    // ถ้าไซส์เดิมไม่มีในสีใหม่ ให้สลับไปไซส์แรกที่มีของสีนั้นแทน
    const availableSizes = product.variants.filter((v) => v.color === color).map((v) => v.size);
    if (!availableSizes.includes(selectedSize)) {
      setSelectedSize(availableSizes[0]);
    }
  }

  function handleSaveClick() {
    setIsSaved((prev) => !prev);
    // ขั้นตอนเข้า: mount ก่อน แล้วค่อย trigger transition ให้ fade เข้า
    setShowToast(true);
    requestAnimationFrame(() => setToastVisible(true));

    // ขั้นตอนออก: fade ออกก่อน แล้วค่อย unmount จริง
    setTimeout(() => setToastVisible(false), 1800);
    setTimeout(() => setShowToast(false), 2100);
  }

  if (!product) return <div className="app" style={{ padding: 32 }}>กำลังโหลด...</div>;

  const ALL_SIZES = ['S', 'M', 'L', 'XL'];

  return (
    <div className="app" style={{ padding: '36px 32px 60px' }}>
      <span
        style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-dim)', cursor: 'pointer' }}
        onClick={() => navigate('/')}
      >
        ‹ กลับไปหน้าแรก
      </span>

      <div className="pd-layout">
        <div className="pd-image-box">
          {currentVariant?.imageUrl
            ? <img src={currentVariant.imageUrl} alt={product.name} style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
            : <span style={{ color: 'var(--text-dim)' }}>IMAGE</span>}
        </div>

        <div>
          <h1 style={{ fontSize: 22, lineHeight: 1.4, marginBottom: 18 }}>{product.name}</h1>

          {/* ===== เลือกสี ===== */}
          <div style={{ fontSize: 12.5, fontWeight: 700, color: 'var(--ink)', marginBottom: 10 }}>
            สี: {currentVariant?.color?.toUpperCase()}
          </div>
          <div style={{ display: 'flex', gap: 10, marginBottom: 24 }}>
            {colors.map((v) => (
              <span
                key={v.color}
                onClick={() => handleColorClick(v.color)}
                title={v.color}
                style={{
                  width: 34, height: 34, borderRadius: '50%', cursor: 'pointer',
                  background: v.colorHex || '#ccc',
                  boxShadow: selectedColor === v.color
                    ? '0 0 0 2px #fff, 0 0 0 3px var(--ink)'
                    : 'inset 0 0 0 1px rgba(0,0,0,.12)',
                }}
              />
            ))}
          </div>

          {/* ===== เลือกไซส์ ===== */}
          <div style={{ fontSize: 12.5, fontWeight: 700, color: 'var(--ink)', marginBottom: 10 }}>
            ไซส์: {selectedSize || '-'}
          </div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
            {ALL_SIZES.map((size) => {
              const available = sizesForSelectedColor.includes(size);
              const isSelected = selectedSize === size;
              return (
                <div
                  key={size}
                  onClick={() => available && setSelectedSize(size)}
                  style={{
                    width: 42, height: 42, borderRadius: 8,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 13, fontWeight: 600,
                    cursor: available ? 'pointer' : 'not-allowed',
                    border: isSelected ? '2px solid var(--ink)' : '1.5px solid var(--line)',
                    color: available ? 'var(--text)' : 'var(--text-dim)',
                    background: available ? '#fff' : 'var(--parchment-deep)',
                    opacity: available ? 1 : 0.4,
                    textDecoration: available ? 'none' : 'line-through',
                  }}
                >
                  {size}
                </div>
              );
            })}
          </div>

          {currentVariant && (
            <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--ink)', marginTop: 20, marginBottom: 20 }}>
              {product.price} บาท
            </div>
          )}

          <div
            onClick={handleSaveClick}
            className={isSaved ? 'btn' : 'btn ghost'}
            style={{
              display: 'inline-flex', width: '100%', justifyContent: 'center',
              background: isSaved ? 'var(--ink)' : '#fff',
              color: isSaved ? '#fff' : 'var(--ink)',
            }}
          >
            {isSaved ? '❤️ บันทึกไว้ในรายการที่ชอบแล้ว' : '🤍 กดถูกใจสินค้า (สนใจสินค้าชิ้นนี้)'}
          </div>
        </div>
      </div>

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
          
        </div>
      )}
    </div>
  );
}