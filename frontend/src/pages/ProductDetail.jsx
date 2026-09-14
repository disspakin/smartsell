import { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api/client';

export default function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [selectedColor, setSelectedColor] = useState(null);
  const [selectedSize, setSelectedSize] = useState(null);
  const [liked, setLiked] = useState(false);
  const [toast, setToast] = useState('');

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

  function handleLike() {
    if (liked) return;
    setLiked(true);
    // ส่ง INTERESTED_CLICK ไปยัง Backend
    api.logInteraction({
      productId: product.id,
      eventType: 'INTERESTED_CLICK',
      size: selectedSize,
    });
    setToast('❤️ บันทึกในรายการที่ชอบแล้ว!');
    setTimeout(() => setToast(''), 2500);
  }

  if (!product) return <div className="app" style={{ padding: 32 }}>กำลังโหลด...</div>;

  const ALL_SIZES = ['S', 'M', 'L', 'XL'];

  return (
    <div className="app" style={{ padding: '36px 32px 60px', position: 'relative' }}>
      {/* Toast Notification */}
      {toast && (
        <div style={{
          position: 'fixed', top: 24, left: '50%', transform: 'translateX(-50%)',
          background: '#0f172a', color: '#fff', padding: '12px 24px',
          borderRadius: 50, fontSize: 13.5, fontWeight: 600, zIndex: 9999,
          boxShadow: '0 8px 24px rgba(0,0,0,0.18)',
        }}>
          {toast}
        </div>
      )}

      <span
        style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-dim)', cursor: 'pointer' }}
        onClick={() => navigate('/')}
      >
        ‹ กลับไปหน้าแรก
      </span>

      <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: 56, marginTop: 20 }}>
        <div style={{ height: 420, background: '#fff', border: '1px solid var(--line)', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
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

          <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--ink)', marginBottom: 20 }}>
            {product.price} บาท
          </div>

          <div
            className="btn"
            onClick={handleLike}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              padding: '14px 24px',
              fontSize: 14,
              fontWeight: 600,
              borderRadius: 12,
              cursor: liked ? 'default' : 'pointer',
              transition: 'all 0.2s ease',
              backgroundColor: liked ? '#e11d48' : '#0f172a',
              color: '#ffffff',
            }}
          >
            {liked ? '❤️ บันทึกไว้ในรายการที่ชอบแล้ว' : '🤍 กดถูกใจสินค้า (สนใจสินค้าชิ้นนี้)'}
          </div>
        </div>
      </div>
    </div>
  );
}