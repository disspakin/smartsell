import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';

export default function Products() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    api.getProducts()
      .then(setProducts)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="app">
      <div style={{ padding: '44px 32px 0' }}>
        <div className="eyebrow">แคตตาล็อก</div>
        <h2 style={{ fontSize: 26, marginTop: 8 }}>สินค้าทั้งหมด</h2>
      </div>

      {loading && <p style={{ padding: '0 32px', color: 'var(--text-dim)' }}>กำลังโหลด...</p>}

      {!loading && products.length === 0 && (
        <p style={{ padding: '0 32px', color: 'var(--text-dim)' }}>
          ยังไม่มีสินค้าในระบบ — ลองเพิ่มข้อมูลผ่าน database ก่อน
        </p>
      )}

      <div className="browse-grid">
        {products.map((p) => (
          <div key={p.id} className="browse-card" onClick={() => navigate(`/products/${p.id}`)}>
            <div className="bimg">
              {p.imageUrl && (
                <img src={p.imageUrl} alt={p.name} style={{ width: '100%', height: '100%', objectFit: 'contain', borderRadius: 10, padding: 8 }} />
              )}
            </div>
            <h3 style={{ fontSize: 14, margin: '0 0 4px' }}>{p.name}</h3>
            <div style={{ fontSize: 12, color: 'var(--text-dim)' }}>{p.price} บาท</div>
          </div>
        ))}
      </div>
    </div>
  );
}