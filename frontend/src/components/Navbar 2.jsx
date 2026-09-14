import { Link, useLocation } from 'react-router-dom';

export default function Navbar() {
  const { pathname } = useLocation();
  const isActive = (path) => (pathname === path ? 'active' : '');

  return (
    <div className="nav">
      <Link to="/" className="brand">
        <div className="color-mark"></div>
        <span className="brand-name">SmartSell AI</span>
      </Link>
      <div className="links">
        <Link to="/" className={isActive('/')}>หน้าแรก</Link>
        <Link to="/assistant" className={isActive('/assistant')}>ผู้ช่วย AI</Link>
        <Link to="/products" className={isActive('/products')}>สินค้า</Link>
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
    </div>
  );
}
