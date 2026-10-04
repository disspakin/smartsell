import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi, authApi } from '../../api/client';
import { STAFF_LOGIN_PATH, STAFF_DASHBOARD_PATH } from '../../config/staffRoutes';

export default function AdminProductAnalytics() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [user, setUser] = useState({ name: 'ผู้จัดการร้าน', role: 'STORE_MANAGER' });

  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('recommendedDesc');

  const navigate = useNavigate();

  useEffect(() => {
    const { name, role } = authApi.getSession();
    if (name) setUser({ name, role: role || 'STORE_MANAGER' });

    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await adminApi.getProductAnalytics();
      setProducts(data);
    } catch (err) {
      if (err.status === 401 || err.status === 403) {
        authApi.logout();
        navigate(STAFF_LOGIN_PATH, { replace: true });
        return;
      }
      setError(err.message || 'ไม่สามารถโหลดข้อมูลสินค้าได้');
    } finally {
      setLoading(false);
    }
  };

  const filteredAndSortedProducts = useMemo(() => {
    let result = [...products];
    
    if (search.trim()) {
      const query = search.toLowerCase();
      result = result.filter(p => p.productName.toLowerCase().includes(query));
    }

    result.sort((a, b) => {
      switch (sortBy) {
        case 'recommendedDesc':
          return (b.recommendedCount || 0) - (a.recommendedCount || 0);
        case 'recommendedAsc':
          return (a.recommendedCount || 0) - (b.recommendedCount || 0);
        case 'interestedDesc':
          return (b.interestedCount || 0) - (a.interestedCount || 0);
        case 'interestedAsc':
          return (a.interestedCount || 0) - (b.interestedCount || 0);
        case 'viewDesc':
          return (b.viewCount || 0) - (a.viewCount || 0);
        case 'viewAsc':
          return (a.viewCount || 0) - (b.viewCount || 0);
        default:
          return 0;
      }
    });

    return result;
  }, [products, search, sortBy]);

  const handleLogout = () => {
    authApi.logout();
    navigate('/', { replace: true });
  };

  return (
    <div style={{ backgroundColor: '#f8fafc', minHeight: '100vh', paddingBottom: '60px' }}>
      {/* Top Admin Header */}
      <div style={{
        backgroundColor: '#0f1b2d',
        color: '#ffffff',
        padding: '20px 32px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        position: 'sticky',
        top: 0,
        zIndex: 10
      }}>
        <div style={{
          maxWidth: '1200px',
          margin: '0 auto',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '16px',
        }}>
          <button 
            onClick={() => navigate(STAFF_DASHBOARD_PATH)}
            style={{
              background: '#1e293b', border: '1px solid #334155', color: '#94a3b8', cursor: 'pointer',
              fontSize: '13px', fontWeight: '600', padding: '7px 14px', borderRadius: '20px',
              display: 'inline-flex', alignItems: 'center', gap: '6px', transition: 'all 0.2s',
              flexShrink: 0,
            }}
            onMouseOver={(e) => { e.currentTarget.style.backgroundColor = '#334155'; e.currentTarget.style.color = '#fff'; }}
            onMouseOut={(e) => { e.currentTarget.style.backgroundColor = '#1e293b'; e.currentTarget.style.color = '#94a3b8'; }}
          >
            ← กลับ
          </button>

          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <h1 style={{ fontSize: '22px', color: '#ffffff', fontFamily: 'Fraunces, serif', margin: 0 }}>
                All Products Analytics
              </h1>
              <span style={{
                fontSize: '11px',
                fontWeight: '700',
                backgroundColor: '#1e3a5f',
                color: '#60a5fa',
                padding: '3px 10px',
                borderRadius: '12px',
                letterSpacing: '0.05em',
              }}>
                UTCC SHOP
              </span>
            </div>
            <p style={{ fontSize: '13px', color: '#94a3b8', margin: '4px 0 0' }}>
              ข้อมูลสถิติของสินค้าทั้งหมด แยกตามยอดวิว, กดสนใจ และ AI แนะนำ
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '14px', fontWeight: '600', color: '#f8fafc' }}>{user.name}</div>
              <div style={{ fontSize: '11.5px', color: '#94a3b8' }}>{user.role}</div>
            </div>
            <button
              onClick={handleLogout}
              style={{
                backgroundColor: 'transparent',
                border: '1px solid #475569',
                color: '#cbd5e1',
                padding: '8px 16px',
                borderRadius: '20px',
                fontSize: '13px',
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
              onMouseOver={(e) => { e.target.style.backgroundColor = '#1e293b'; e.target.style.color = '#fff'; }}
              onMouseOut={(e) => { e.target.style.backgroundColor = 'transparent'; e.target.style.color = '#cbd5e1'; }}
            >
              ออกจากระบบ 🚪
            </button>
          </div>
        </div>
      </div>

      <div style={{ maxWidth: '1200px', margin: '32px auto 0', padding: '0 24px' }}>
        {/* Controls Section */}
        <div style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: '16px',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px',
          backgroundColor: '#ffffff',
          padding: '16px 24px',
          borderRadius: '16px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 4px 12px -2px rgba(0,0,0,0.02)'
        }}>
          <div style={{ flex: '1 1 300px', position: 'relative' }}>
            <span style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }}>🔍</span>
            <input 
              type="text" 
              placeholder="ค้นหาชื่อสินค้า..." 
              value={search}
              onChange={e => setSearch(e.target.value)}
              style={{
                width: '100%',
                padding: '12px 16px 12px 42px',
                borderRadius: '12px',
                border: '1px solid #cbd5e1',
                fontSize: '14px',
                outline: 'none',
                color: '#0f1b2d',
                boxSizing: 'border-box'
              }}
            />
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: '14px', color: '#64748b', fontWeight: '500' }}>จัดเรียงตาม:</span>
            <select
              value={sortBy}
              onChange={e => setSortBy(e.target.value)}
              style={{
                padding: '12px 16px',
                borderRadius: '12px',
                border: '1px solid #cbd5e1',
                fontSize: '14px',
                color: '#0f1b2d',
                backgroundColor: '#f8fafc',
                cursor: 'pointer',
                outline: 'none',
                minWidth: '220px'
              }}
            >
              <option value="recommendedDesc">🤖 AI แนะนำ (มากไปน้อย)</option>
              <option value="recommendedAsc">🤖 AI แนะนำ (น้อยไปมาก)</option>
              <option value="interestedDesc">💖 กดสนใจ (มากไปน้อย)</option>
              <option value="interestedAsc">💖 กดสนใจ (น้อยไปมาก)</option>
              <option value="viewDesc">👁️ เข้าชม (มากไปน้อย)</option>
              <option value="viewAsc">👁️ เข้าชม (น้อยไปมาก)</option>
            </select>
          </div>
        </div>

        {/* Content Section */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '80px 0', color: '#64748b' }}>
            <div style={{ fontSize: '24px', marginBottom: '12px' }}>📊</div>
            <div>กำลังโหลดข้อมูลสถิติสินค้าทั้งหมด...</div>
          </div>
        ) : error ? (
          <div style={{
            backgroundColor: '#fef2f2', color: '#991b1b', padding: '20px',
            borderRadius: '12px', textAlign: 'center', border: '1px solid #fecaca',
          }}>
            <p style={{ margin: '0 0 12px', fontSize: '15px' }}>⚠️ {error}</p>
            <button onClick={fetchData} style={{ padding: '8px 20px', backgroundColor: '#991b1b', color: '#fff', border: 'none', borderRadius: '8px', cursor: 'pointer' }}>
              ลองใหม่อีกครั้ง
            </button>
          </div>
        ) : filteredAndSortedProducts.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '80px 0', color: '#64748b', backgroundColor: '#fff', borderRadius: '16px', border: '1px solid #e2e8f0' }}>
            ไม่พบสินค้าที่ตรงกับการค้นหา "{search}"
          </div>
        ) : (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
            gap: '24px'
          }}>
            {filteredAndSortedProducts.map(p => (
              <div key={p.productId} style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                overflow: 'hidden',
                border: '1px solid #e2e8f0',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.05)',
                display: 'flex',
                flexDirection: 'column'
              }}>
                <div style={{ position: 'relative' }}>
                  <img src={p.imageUrl} alt={p.productName} style={{ width: '100%', height: '220px', objectFit: 'cover' }} />
                  <div style={{
                    position: 'absolute', top: '12px', left: '12px',
                    backgroundColor: 'rgba(255,255,255,0.9)', padding: '4px 10px',
                    borderRadius: '8px', fontSize: '12px', fontWeight: '700', color: '#0f1b2d',
                    backdropFilter: 'blur(4px)'
                  }}>
                    #{p.productId}
                  </div>
                  <div style={{
                    position: 'absolute', bottom: '12px', right: '12px',
                    backgroundColor: '#10b981', color: '#fff', padding: '4px 10px',
                    borderRadius: '12px', fontSize: '13px', fontWeight: '600'
                  }}>
                    ฿{p.price}
                  </div>
                </div>
                
                <div style={{ padding: '16px', flex: 1, display: 'flex', flexDirection: 'column' }}>
                  <h3 style={{ margin: '0 0 16px', fontSize: '15px', color: '#0f1b2d', lineHeight: '1.4' }}>
                    {p.productName}
                  </h3>
                  
                  <div style={{ marginTop: 'auto', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px', backgroundColor: '#f8fafc', borderRadius: '8px' }}>
                      <span style={{ fontSize: '13px', color: '#64748b' }}>🤖 AI แนะนำ</span>
                      <span style={{ fontSize: '14px', fontWeight: '700', color: '#8b5cf6' }}>{p.recommendedCount || 0} ครั้ง</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px', backgroundColor: '#f8fafc', borderRadius: '8px' }}>
                      <span style={{ fontSize: '13px', color: '#64748b' }}>💖 กดสนใจ</span>
                      <span style={{ fontSize: '14px', fontWeight: '700', color: '#e11d48' }}>{p.interestedCount || 0} ครั้ง</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px', backgroundColor: '#f8fafc', borderRadius: '8px' }}>
                      <span style={{ fontSize: '13px', color: '#64748b' }}>👁️ เข้าชม</span>
                      <span style={{ fontSize: '14px', fontWeight: '700', color: '#3b82f6' }}>{p.viewCount || 0} วิว</span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
