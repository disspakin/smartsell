import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/client';

export default function AdminLogin() {
  const [email, setEmail] = useState('admin@utcc.ac.th');
  const [password, setPassword] = useState('password123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await authApi.login(email, password);
      if (res.token) {
        localStorage.setItem('smartsell_store_token', res.token);
        localStorage.setItem('smartsell_user_name', res.displayName || res.email);
        localStorage.setItem('smartsell_user_role', res.role);
        navigate('/admin/dashboard');
      }
    } catch (err) {
      setError(err.message || 'เข้าสู่ระบบไม่สำเร็จ กรุณาตรวจสอบ Email หรือ Password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '80vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px 16px',
      background: 'linear-gradient(135deg, #0f1b2d 0%, #1e3a5f 100%)',
    }}>
      <div style={{
        width: '100%',
        maxWidth: '420px',
        backgroundColor: '#ffffff',
        borderRadius: '20px',
        boxShadow: '0 20px 40px rgba(0,0,0,0.3)',
        padding: '40px 32px',
      }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '56px',
            height: '56px',
            borderRadius: '16px',
            backgroundColor: '#0f1b2d',
            color: '#ffffff',
            fontSize: '24px',
            marginBottom: '16px',
            fontWeight: 'bold',
          }}>
            🏪
          </div>
          <h2 style={{ fontSize: '24px', color: '#0f1b2d', marginBottom: '8px', fontFamily: 'Fraunces, serif' }}>
            Store Manager Login
          </h2>
          <p style={{ fontSize: '13.5px', color: '#6b7280', margin: 0 }}>
            เข้าสู่ระบบร้านค้า UTCC Shop สำหรับจัดการและดู Analytics
          </p>
        </div>

        {error && (
          <div style={{
            backgroundColor: '#fef2f2',
            border: '1px solid #fecaca',
            color: '#991b1b',
            padding: '12px 16px',
            borderRadius: '10px',
            fontSize: '13.5px',
            marginBottom: '20px',
          }}>
            ⚠️ {error}
          </div>
        )}

        <form onSubmit={handleLogin}>
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '6px' }}>
              อีเมลร้านค้า (Email)
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@utcc.ac.th"
              style={{
                width: '100%',
                padding: '12px 16px',
                borderRadius: '10px',
                border: '1.5px solid #e5e7eb',
                fontSize: '14px',
                outline: 'none',
                boxSizing: 'border-box',
                transition: 'border-color 0.2s',
              }}
              onFocus={(e) => e.target.style.borderColor = '#1e3a5f'}
              onBlur={(e) => e.target.style.borderColor = '#e5e7eb'}
            />
          </div>

          <div style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '6px' }}>
              รหัสผ่าน (Password)
            </label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              style={{
                width: '100%',
                padding: '12px 16px',
                borderRadius: '10px',
                border: '1.5px solid #e5e7eb',
                fontSize: '14px',
                outline: 'none',
                boxSizing: 'border-box',
                transition: 'border-color 0.2s',
              }}
              onFocus={(e) => e.target.style.borderColor = '#1e3a5f'}
              onBlur={(e) => e.target.style.borderColor = '#e5e7eb'}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '14px',
              backgroundColor: '#0f1b2d',
              color: '#ffffff',
              border: 'none',
              borderRadius: '12px',
              fontSize: '15px',
              fontWeight: '600',
              cursor: loading ? 'not-allowed' : 'pointer',
              opacity: loading ? 0.7 : 1,
              transition: 'background-color 0.2s',
            }}
          >
            {loading ? 'กำลังตรวจสอบ...' : 'เข้าสู่ระบบ Dashboard'}
          </button>
        </form>

        {/* Demo Hint */}
        <div style={{
          marginTop: '24px',
          padding: '12px 16px',
          backgroundColor: '#f8fafc',
          borderRadius: '10px',
          fontSize: '12px',
          color: '#64748b',
          border: '1px dashed #cbd5e1',
        }}>
          💡 <b>ข้อมูลเข้าสู่ระบบทดสอบ (Default):</b><br />
          Email: <code style={{ color: '#1e3a5f' }}>admin@utcc.ac.th</code><br />
          Password: <code style={{ color: '#1e3a5f' }}>password123</code>
        </div>
      </div>
    </div>
  );
}
