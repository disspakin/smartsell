import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';

export default function Home() {
  const [products, setProducts] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    api.getProducts().then(setProducts).catch(console.error);
  }, []);

  return (
    <div className="app">
      <div style={{ padding: '72px 32px 60px', textAlign: 'center' }}>
        <h1 style={{ fontSize: 40, lineHeight: 1.2, margin: '14px auto 20px' }}>
          แต่งตัวให้เข้ากับคุณไม่ใช่ แค่เข้ากับเทรนด์
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
        </div>
        <div className="grid3">
          {products.slice(0, 2).map((p) => (
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

          {products.length === 0 && (
            <p style={{ color: 'var(--text-dim)' }}>
              ยังไม่มีสินค้า — ลองเพิ่มข้อมูลผ่าน POST /api/products หรือ insert ตรง ๆ ใน DB ก่อน
            </p>
          )}

          {products.length > 0 && (
            <div
              className="pcard"
              onClick={() => navigate('/products')}
              style={{
                background: 'var(--ink)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
              }}
            >
              <p style={{ color: '#fff', fontSize: 16, fontWeight: 600, margin: 0, textAlign: 'center' }}>
                เลือกดูสินค้าทั้งหมด
              </p>
            </div>
          )}
        </div>
      </div>

      {/* ปุ่มลอย "แชทกับเรา" มุมขวาล่าง */}
      <div
        onClick={() => navigate('/assistant')}
        style={{
          position: 'fixed', bottom: 26, right: 32, zIndex: 20,
          display: 'flex', alignItems: 'center', cursor: 'pointer',
          filter: 'drop-shadow(0 10px 24px rgba(15,27,45,.3))',
        }}
      >
        <div style={{
          width: 52, height: 52, borderRadius: '50%', background: 'var(--ink)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 20, border: '3px solid var(--parchment)', position: 'relative', zIndex: 2,
        }}>
          🎧
          <div style={{
            position: 'absolute', top: 1, right: 1, width: 11, height: 11, borderRadius: '50%',
            background: 'var(--sage)', border: '2px solid var(--parchment)',
          }} />
        </div>
        <div style={{
          background: 'var(--accent-deep)', color: '#fff', fontSize: 12, fontWeight: 700,
          padding: '9px 16px 9px 32px', marginLeft: -22, borderRadius: 20,
        }}>
          แชทกับเรา
        </div>
      </div>
    </div>
  );
}