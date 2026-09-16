import { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';

export default function BottomNav() {
  const { pathname } = useLocation();
  const [isMobile, setIsMobile] = useState(
    window.innerWidth <= 768 || window.innerHeight <= 500
  );

  // เช็คขนาดจอด้วย JS โดยตรง ไม่พึ่ง CSS media query
  // ครอบคลุมทั้งมือถือแนวตั้ง (กว้าง ≤768) และแนวนอน (สูง ≤500)
  useEffect(() => {
    function handleResize() {
      setIsMobile(window.innerWidth <= 768 || window.innerHeight <= 500);
    }
    window.addEventListener('resize', handleResize);
    handleResize();
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const isActive = (path) => (pathname === path ? 'active' : '');

  const items = [
    { path: '/', label: 'หน้าแรก', icon: '🏠' },
    { path: '/assistant', label: 'ผู้ช่วย AI', icon: '✨' },
    { path: '/products', label: 'สินค้า', icon: '🛍️' },
    { path: '/lucky-color', label: 'สีมงคล', icon: '🔮' },
    { path: '/personal-color', label: 'Personal Color', icon: '🎨' },
  ];

  // ถ้าไม่ใช่มือถือ ไม่ต้อง render อะไรเลย (ชัวร์กว่า CSS display:none)
  if (!isMobile) return null;

  return (
    <div
      style={{
        display: 'flex',
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        background: '#fff',
        borderTop: '1px solid var(--line)',
        padding: '8px 0 calc(8px + env(safe-area-inset-bottom))',
        zIndex: 9999,
        boxShadow: '0 -2px 10px rgba(0,0,0,.08)',
      }}
    >
      {items.map((item) => {
        const active = isActive(item.path);
        return (
          <Link
            key={item.path}
            to={item.path}
            style={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 2,
              textDecoration: 'none',
              color: active ? 'var(--ink)' : 'var(--text-dim)',
              fontSize: 10,
              fontWeight: active ? 700 : 400,
            }}
          >
            <span style={{ fontSize: 20 }}>{item.icon}</span>
            <span>{item.label}</span>
          </Link>
        );
      })}
    </div>
  );
}   