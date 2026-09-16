import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

export default function Navbar() {
  const { pathname } = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const isActive = (path) => (pathname === path ? 'active' : '');

  return (
    <>
      <div className="nav">
        <Link to="/" className="brand" onClick={() => setMenuOpen(false)}>
          <div className="color-mark"></div>
          <span className="brand-name">SmartSell AI</span>
        </Link>

        <div className="links">
          <Link to="/" className={isActive('/')}>หน้าแรก</Link>
          <Link to="/assistant" className={isActive('/assistant')}>ผู้ช่วย AI</Link>
          <Link to="/products" className={isActive('/products')}>สินค้า</Link>
          <Link to="/lucky-color" className={isActive('/lucky-color')}>🔮 สีมงคล</Link>
          <Link
            to="/personal-color"
            style={{
              background: '#fff', color: 'var(--ink)', fontSize: 13, fontWeight: 600,
              padding: '9px 18px', borderRadius: 30, textDecoration: 'none',
              display: 'flex', alignItems: 'center', gap: 8,
            }}
          >
            <div className="color-mark" style={{ width: 20, height: 20, background: 'var(--parchment-deep)' }}></div>
            Personal Color
          </Link>
        </div>

        {/* ปุ่ม hamburger — โชว์เฉพาะจอมือถือ (ควบคุมด้วย CSS) */}
        <div className="hamburger" onClick={() => setMenuOpen(!menuOpen)}>
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>

      {/* เมนูมือถือ — เปิด/ปิดด้วย state */}
      <div className={`mobile-menu ${menuOpen ? 'open' : ''}`}>
        <Link to="/" onClick={() => setMenuOpen(false)}>หน้าแรก</Link>
        <Link to="/assistant" onClick={() => setMenuOpen(false)}>ผู้ช่วย AI</Link>
        <Link to="/products" onClick={() => setMenuOpen(false)}>สินค้า</Link>
        <Link to="/lucky-color" onClick={() => setMenuOpen(false)}>🔮 สีมงคล</Link>
        <Link to="/personal-color" onClick={() => setMenuOpen(false)}>🎨 Personal Color</Link>
      </div>
    </>
  );
}