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
    <div className="app" style={{ padding: '48px 32px 60px' }}>
      <div style={{ textAlign: 'center', marginBottom: 36 }}>
        <div className="eyebrow">All Products</div>
        <h1 style={{ fontSize: 30, marginTop: 12 }}>สินค้าทั้งหมด</h1>
      </div>

      {loading && <p style={{ textAlign: 'center', color: 'var(--text-dim)' }}>กำลังโหลด...</p>}

      {!loading && products.length === 0 && (
        <p style={{ textAlign: 'center', color: 'var(--text-dim)' }}>
          ยังไม่มีสินค้าในระบบ — ลองเพิ่มข้อมูลผ่าน database ก่อน
        </p>
      )}

      <div className="grid3">
        {products.map((p) => (
          <div key={p.id} className="pcard" onClick={() => navigate(`/products/${p.id}`)}>
            <div className="pimg">
              {p.imageUrl ? <img src={p.imageUrl} alt={p.name} /> : 'IMAGE'}
            </div>
            <div className="pbody">
              {p.category && (
                <div className="tone-tag"><span className="dot"></span>{p.category}</div>
              )}
              <h3>{p.name}</h3>
              <div className="price">{p.price} บาท</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}