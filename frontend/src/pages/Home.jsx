import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';

export default function Home() {
  const [products, setProducts] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    api.getProducts().then(setProducts).catch(console.error);
  }, []);

  const featured = products.slice(0, 1);

  return (
    <div className="app">
      <div style={{ padding: '72px 32px 60px', textAlign: 'center' }}>
        <h1 style={{ fontSize: 40, lineHeight: 1.2, margin: '14px auto 20px', maxWidth: 560 }}>
          แต่งตัวให้เข้ากับคุณ ไม่ใช่แค่เข้ากับเทรนด์
        </h1>
        <p style={{ fontSize: 15, color: 'var(--text-dim)', maxWidth: 420, margin: '0 auto 30px', lineHeight: 1.7 }}>
          บอก AI ว่าวันนี้ต้องใส่ไปไหน แล้วให้ระบบแนะนำเสื้อผ้าที่เข้ากับโทนสีผิว โอกาส และงบของคุณ
        </p>
        <div className="btn primary" style={{ display: 'inline-flex' }} onClick={() => navigate('/assistant')}>
          ✨ ให้ผู้ช่วย AI เลือกให้คุณ
        </div>
      </div>

      <div style={{ padding: '0 32px 60px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 22 }}>
          <h2 style={{ fontSize: 26 }}>สินค้าแนะนำสำหรับคุณ</h2>
          <a
            onClick={() => navigate('/products')}
            style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink)', cursor: 'pointer' }}
          >
            ดูสินค้าทั้งหมด →
          </a>
        </div>

        <div className="home-featured-grid">
          {featured.map((p) => {
            const colors = p.variants
              ? Array.from(new Map(p.variants.map((v) => [v.color, v])).values())
              : [];

            return (
              <div key={p.id} className="pcard" onClick={() => navigate(`/products/${p.id}`)} style={{ cursor: 'pointer' }}>
                <div className="pimg">
                  {p.imageUrl ? <img src={p.imageUrl} alt={p.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : 'IMAGE'}
                </div>
                <div className="pbody">
                  {p.category && (
                    <div className="tone-tag"><span className="dot"></span>{p.category}</div>
                  )}
                  <h3>{p.name}</h3>
                  <div className="price">{p.price} บาท</div>

                  {colors.length > 0 && (
                    <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
                      {colors.map((v) => (
                        <span
                          key={v.color}
                          style={{
                            width: 20, height: 20, borderRadius: '50%',
                            background: v.colorHex || '#ccc',
                            boxShadow: 'inset 0 0 0 1px rgba(0,0,0,.15)',
                          }}
                        />
                      ))}
                    </div>
                  )}
                </div>
              </div>
            );
          })}

          <div className="pcard-cta" onClick={() => navigate('/products')}>
            เลือกดูสินค้าทั้งหมด
          </div>

          {products.length === 0 && (
            <p style={{ color: 'var(--text-dim)' }}>
              ยังไม่มีสินค้า — ลองเพิ่มข้อมูลผ่าน POST /api/products หรือ insert ตรง ๆ ใน DB ก่อน
            </p>
          )}
        </div>
      </div>

      <div className="contact-fab" onClick={() => navigate('/support')}>
        <div className="av">🎧</div>
        <div className="pill">แชทกับเรา</div>
      </div>
    </div>
  );
}