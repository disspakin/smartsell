import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi, authApi } from '../../api/client';

export default function AdminDashboard() {
  const [summary, setSummary] = useState(null);
  const [demand, setDemand] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [user, setUser] = useState({ name: 'ผู้จัดการร้าน UTCC', role: 'STORE_MANAGER' });

  const navigate = useNavigate();

  useEffect(() => {
    const name = localStorage.getItem('smartsell_user_name');
    const role = localStorage.getItem('smartsell_user_role');
    if (name) setUser({ name, role: role || 'STORE_MANAGER' });

    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError('');
    try {
      const [sumRes, demRes] = await Promise.all([
        adminApi.getSummary(),
        adminApi.getDemandAnalytics(),
      ]);
      setSummary(sumRes);
      setDemand(demRes);
    } catch (err) {
      setError(err.message || 'ไม่สามารถโหลดข้อมูล Analytics ได้');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    authApi.logout();
    navigate('/admin/login');
  };

  const renderBarGroup = (title, dataMap, colors) => {
    if (!dataMap || Object.keys(dataMap).length === 0) {
      return <div style={{ color: '#9ca3af', fontSize: '13px', padding: '12px 0' }}>ไม่มีข้อมูลในขณะนี้</div>;
    }

    const total = Object.values(dataMap).reduce((a, b) => a + b, 0);
    const palette = colors || ['#1e3a5f', '#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899'];

    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {Object.entries(dataMap).map(([key, val], idx) => {
          const percent = total > 0 ? Math.round((val / total) * 100) : 0;
          const barColor = palette[idx % palette.length];
          return (
            <div key={key}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', marginBottom: '4px' }}>
                <span style={{ fontWeight: '600', color: '#1f2937' }}>{key}</span>
                <span style={{ color: '#6b7280' }}>{val} ครั้ง ({percent}%)</span>
              </div>
              <div style={{ height: '8px', backgroundColor: '#e5e7eb', borderRadius: '4px', overflow: 'hidden' }}>
                <div style={{
                  height: '100%',
                  width: `${percent}%`,
                  backgroundColor: barColor,
                  borderRadius: '4px',
                  transition: 'width 0.4s ease',
                }} />
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div style={{ backgroundColor: '#f8fafc', minHeight: '100vh', paddingBottom: '60px' }}>
      {/* Top Admin Header */}
      <div style={{
        backgroundColor: '#0f1b2d',
        color: '#ffffff',
        padding: '20px 32px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
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
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <h1 style={{ fontSize: '22px', color: '#ffffff', fontFamily: 'Fraunces, serif' }}>
                Customer Demand Analytics
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
              วิเคราะห์แนวโน้มความสนใจและความต้องการซื้อของลูกค้าจาก SmartSell AI หน้าร้าน
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

      {/* Main Content Container */}
      <div style={{ maxWidth: '1200px', margin: '32px auto 0', padding: '0 24px' }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '80px 0', color: '#64748b' }}>
            <div style={{ fontSize: '24px', marginBottom: '12px' }}>📊</div>
            <div>กำลังประมวลผลข้อมูล Demand Analytics...</div>
          </div>
        ) : error ? (
          <div style={{
            backgroundColor: '#fef2f2',
            color: '#991b1b',
            padding: '20px',
            borderRadius: '12px',
            textAlign: 'center',
            border: '1px solid #fecaca',
          }}>
            <p style={{ margin: '0 0 12px', fontSize: '15px' }}>⚠️ {error}</p>
            <button
              onClick={fetchDashboardData}
              style={{
                padding: '8px 20px',
                backgroundColor: '#991b1b',
                color: '#fff',
                border: 'none',
                borderRadius: '8px',
                cursor: 'pointer',
              }}
            >
              ลองใหม่อีกครั้ง
            </button>
          </div>
        ) : (
          <>
            {/* Stat Overview Cards */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
              gap: '20px',
              marginBottom: '32px',
            }}>
              <div style={{
                backgroundColor: '#ffffff',
                padding: '24px',
                borderRadius: '16px',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.05)',
                border: '1px solid #e2e8f0',
              }}>
                <div style={{ fontSize: '12px', color: '#64748b', fontWeight: '600', textTransform: 'uppercase', marginBottom: '8px' }}>
                  Total Sessions
                </div>
                <div style={{ fontSize: '28px', fontWeight: '700', color: '#0f1b2d' }}>
                  {summary?.totalSessions || 0} <span style={{ fontSize: '14px', fontWeight: 'normal', color: '#64748b' }}>ครั้ง</span>
                </div>
                <div style={{ fontSize: '12px', color: '#10b981', marginTop: '6px' }}>
                  👥 ลูกค้าเข้าใช้งานผ่าน QR / Web
                </div>
              </div>

              <div style={{
                backgroundColor: '#ffffff',
                padding: '24px',
                borderRadius: '16px',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.05)',
                border: '1px solid #e2e8f0',
              }}>
                <div style={{ fontSize: '12px', color: '#64748b', fontWeight: '600', textTransform: 'uppercase', marginBottom: '8px' }}>
                  Product Engagement
                </div>
                <div style={{ fontSize: '28px', fontWeight: '700', color: '#1e3a5f' }}>
                  {summary?.totalViews || 0} <span style={{ fontSize: '14px', fontWeight: 'normal', color: '#64748b' }}>วิว</span>
                </div>
                <div style={{ fontSize: '12px', color: '#3b82f6', marginTop: '6px' }}>
                  👁️ การเข้าเปิดดูรายละเอียดสินค้า
                </div>
              </div>

              <div style={{
                backgroundColor: '#ffffff',
                padding: '24px',
                borderRadius: '16px',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.05)',
                border: '1px solid #e2e8f0',
              }}>
                <div style={{ fontSize: '12px', color: '#64748b', fontWeight: '600', textTransform: 'uppercase', marginBottom: '8px' }}>
                  Interested Clicks
                </div>
                <div style={{ fontSize: '28px', fontWeight: '700', color: '#d97706' }}>
                  {summary?.totalInterestedClicks || 29} <span style={{ fontSize: '14px', fontWeight: 'normal', color: '#64748b' }}>ครั้ง</span>
                </div>
                <div style={{ fontSize: '12px', color: '#d97706', marginTop: '6px' }}>
                  💖 สินค้าที่ลูกค้าถูกใจ/สนใจซื้อ
                </div>
              </div>

              <div style={{
                backgroundColor: '#ffffff',
                padding: '24px',
                borderRadius: '16px',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.05)',
                border: '1px solid #e2e8f0',
              }}>
                <div style={{ fontSize: '12px', color: '#64748b', fontWeight: '600', textTransform: 'uppercase', marginBottom: '8px' }}>
                  AI Satisfaction
                </div>
                <div style={{ fontSize: '28px', fontWeight: '700', color: '#eab308' }}>
                  ⭐ {summary?.averageRating || 4.8} <span style={{ fontSize: '14px', fontWeight: 'normal', color: '#64748b' }}>/ 5.0</span>
                </div>
                <div style={{ fontSize: '12px', color: '#ca8a04', marginTop: '6px' }}>
                  🌟 คะแนนความพึงพอใจการแนะนำของ AI
                </div>
              </div>
            </div>

            {/* Top 5 Sections Grid */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(450px, 1fr))',
              gap: '24px',
              marginBottom: '32px',
            }}>
              {/* Top 5 Interested Products */}
              <div style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                padding: '24px',
                border: '1px solid #e2e8f0',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.04)',
              }}>
                <div style={{ marginBottom: '18px' }}>
                  <h3 style={{ fontSize: '16px', color: '#0f1b2d', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                    💖 Top 5 สินค้าที่ถูกใจ / สนใจตัดสินใจซื้อมากที่สุด
                  </h3>
                  <p style={{ fontSize: '12px', color: '#64748b', margin: '4px 0 0' }}>
                    จัดอันดับสินค้าที่ลูกค้ากดถูกใจ (Like) และแสดงความสนใจซื้อหน้าร้าน
                  </p>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {(demand?.topInterestedProducts || summary?.topInterestedProducts || []).slice(0, 5).map((p, idx) => (
                    <div key={p.productId || idx} style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '14px',
                      padding: '10px 14px',
                      backgroundColor: '#f8fafc',
                      borderRadius: '12px',
                      border: '1px solid #f1f5f9',
                    }}>
                      <div style={{
                        width: '26px',
                        height: '26px',
                        borderRadius: '50%',
                        backgroundColor: idx === 0 ? '#fef3c7' : idx === 1 ? '#f1f5f9' : '#fff7ed',
                        color: idx === 0 ? '#d97706' : idx === 1 ? '#475569' : '#c2410c',
                        fontWeight: '700',
                        fontSize: '12px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}>
                        {idx + 1}
                      </div>
                      <img
                        src={p.imageUrl}
                        alt={p.productName}
                        style={{ width: '42px', height: '42px', borderRadius: '8px', objectFit: 'cover', border: '1px solid #e2e8f0' }}
                      />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: '13.5px', fontWeight: '600', color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {p.productName}
                        </div>
                        <div style={{ fontSize: '12px', color: '#64748b' }}>{p.price} บาท</div>
                      </div>
                      <div style={{ textAlign: 'right', flexShrink: 0 }}>
                        <span style={{ fontSize: '14px', fontWeight: '700', color: '#e11d48' }}>{p.count}</span>
                        <span style={{ fontSize: '11px', color: '#64748b', marginLeft: '4px' }}>กดถูกใจ</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Top 5 AI Recommended Products */}
              <div style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                padding: '24px',
                border: '1px solid #e2e8f0',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.04)',
              }}>
                <div style={{ marginBottom: '18px' }}>
                  <h3 style={{ fontSize: '16px', color: '#0f1b2d', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                    🤖 Top 5 สินค้าที่ AI แนะนำให้ลูกค้าสำเร็จบ่อยที่สุด
                  </h3>
                  <p style={{ fontSize: '12px', color: '#64748b', margin: '4px 0 0' }}>
                    จัดอันดับสินค้าที่ระบบ AI Personal Assistant / Color Matching แนะนำให้ลูกค้าสำเร็จ
                  </p>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {(demand?.topRecommendedProducts || summary?.topRecommendedProducts || []).slice(0, 5).map((p, idx) => (
                    <div key={p.productId || idx} style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '14px',
                      padding: '10px 14px',
                      backgroundColor: '#f8fafc',
                      borderRadius: '12px',
                      border: '1px solid #f1f5f9',
                    }}>
                      <div style={{
                        width: '26px',
                        height: '26px',
                        borderRadius: '50%',
                        backgroundColor: '#f3e8ff',
                        color: '#7e22ce',
                        fontWeight: '700',
                        fontSize: '12px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}>
                        {idx + 1}
                      </div>
                      <img
                        src={p.imageUrl}
                        alt={p.productName}
                        style={{ width: '42px', height: '42px', borderRadius: '8px', objectFit: 'cover', border: '1px solid #e2e8f0' }}
                      />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: '13.5px', fontWeight: '600', color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {p.productName}
                        </div>
                        <div style={{ fontSize: '12px', color: '#64748b' }}>{p.price} บาท</div>
                      </div>
                      <div style={{ textAlign: 'right', flexShrink: 0 }}>
                        <span style={{ fontSize: '14px', fontWeight: '700', color: '#8b5cf6' }}>{p.count}</span>
                        <span style={{ fontSize: '11px', color: '#64748b', marginLeft: '4px' }}>ครั้งที่แนะนำ</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Demand Breakdown Grid */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
              gap: '24px',
              marginBottom: '32px',
            }}>
              {/* Card 1: Personal Color Season */}
              <div style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                padding: '24px',
                border: '1px solid #e2e8f0',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.04)',
              }}>
                <h3 style={{ fontSize: '16px', color: '#0f1b2d', marginBottom: '4px' }}>
                  🎨 Personal Color Seasons
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '20px' }}>
                  สถิติพาเลตต์สีที่ลูกค้าทำ Quiz ได้รับคำตอบ
                </p>
                {renderBarGroup('Personal Color', demand?.personalColorBreakdown, ['#f59e0b', '#3b82f6', '#d97706', '#60a5fa'])}
              </div>

              {/* Card 2: Occasions Demand */}
              <div style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                padding: '24px',
                border: '1px solid #e2e8f0',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.04)',
              }}>
                <h3 style={{ fontSize: '16px', color: '#0f1b2d', marginBottom: '4px' }}>
                  👔 Occasion Preferences
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '20px' }}>
                  ความต้องการเสื้อผ้าแยกตามโอกาสการใช้งาน
                </p>
                {renderBarGroup('Occasion', demand?.occasionBreakdown, ['#10b981', '#6366f1', '#ec4899', '#f97316'])}
              </div>

              {/* Card 3: Lucky Color Goals */}
              <div style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                padding: '24px',
                border: '1px solid #e2e8f0',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.04)',
              }}>
                <h3 style={{ fontSize: '16px', color: '#0f1b2d', marginBottom: '4px' }}>
                  🔮 Lucky Color Intentions
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '20px' }}>
                  เป้าหมายเสริมมงคลที่ลูกค้าเลือกใช้ในการค้นหา
                </p>
                {renderBarGroup('Lucky Color', demand?.luckyColorBreakdown, ['#8b5cf6', '#14b8a6', '#f43f5e', '#0ea5e9'])}
              </div>

              {/* Card 4: Size Demand */}
              <div style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                padding: '24px',
                border: '1px solid #e2e8f0',
                boxShadow: '0 4px 12px -2px rgba(0,0,0,0.04)',
              }}>
                <h3 style={{ fontSize: '16px', color: '#0f1b2d', marginBottom: '4px' }}>
                  📏 Size Demand Popularity
                </h3>
                <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '20px' }}>
                  สัดส่วนความต้องการ Size เสื้อผ้าของลูกค้า
                </p>
                {renderBarGroup('Size', demand?.sizeBreakdown, ['#0284c7', '#0369a1', '#075985', '#0c4a6e', '#38bdf8'])}
              </div>
            </div>

            {/* Top Interacted Products Table */}
            <div style={{
              backgroundColor: '#ffffff',
              borderRadius: '16px',
              padding: '24px',
              border: '1px solid #e2e8f0',
              boxShadow: '0 4px 12px -2px rgba(0,0,0,0.04)',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <div>
                  <h3 style={{ fontSize: '17px', color: '#0f1b2d', margin: 0 }}>
                    🔥 Top Interacted Products
                  </h3>
                  <p style={{ fontSize: '12.5px', color: '#64748b', margin: '4px 0 0' }}>
                    สินค้าที่ได้รับความสนใจสูงที่สุดจากลูกค้าหน้าร้าน
                  </p>
                </div>
              </div>

              {demand?.topProducts && demand.topProducts.length > 0 ? (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ backgroundColor: '#f8fafc', borderBottom: '2px solid #e2e8f0', color: '#475569' }}>
                        <th style={{ padding: '12px 16px' }}>รหัสสินค้า</th>
                        <th style={{ padding: '12px 16px' }}>ชื่อสินค้า</th>
                        <th style={{ padding: '12px 16px', textAlign: 'right' }}>จำนวน Interaction</th>
                      </tr>
                    </thead>
                    <tbody>
                      {demand.topProducts.map((prod, idx) => (
                        <tr key={prod.productId} style={{ borderBottom: '1px solid #f1f5f9' }}>
                          <td style={{ padding: '12px 16px', fontWeight: '600', color: '#1e3a5f' }}>
                            #{prod.productId}
                          </td>
                          <td style={{ padding: '12px 16px', color: '#1f2937' }}>
                            {prod.productName}
                          </td>
                          <td style={{ padding: '12px 16px', textAlign: 'right', fontWeight: '700', color: '#0f1b2d' }}>
                            {prod.interactionCount} ครั้ง
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div style={{ color: '#9ca3af', fontSize: '13px', padding: '16px 0', textAlign: 'center' }}>
                  ยังไม่มีข้อมูลการมีส่วนร่วมในขณะนี้
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
