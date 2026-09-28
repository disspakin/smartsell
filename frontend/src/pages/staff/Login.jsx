import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/client';
import {
  STAFF_DASHBOARD_PATH,
  STAFF_REGISTER_PATH,
  STAFF_RESET_PASSWORD_PATH,
} from '../../config/staffRoutes';
import StaffAuthLayout, {
  ErrorBanner,
  SuccessBanner,
  focusHandlers,
  inputStyle,
  labelStyle,
  submitButtonStyle,
  PasswordInput,
} from '../../components/StaffAuthLayout';

export default function StaffLogin() {
  const location = useLocation();
  // หน้า Register ส่งอีเมลที่เพิ่งสมัครมาทาง router state
  const registeredEmail = location.state?.registeredEmail;

  const [form, setForm] = useState({ email: registeredEmail || '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const updateField = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const session = await authApi.login(form.email, form.password);
      authApi.saveSession(session);
      navigate(STAFF_DASHBOARD_PATH, { replace: true });
    } catch (err) {
      setError(err.message || 'เข้าสู่ระบบไม่สำเร็จ กรุณาตรวจสอบอีเมลหรือรหัสผ่าน');
    } finally {
      setLoading(false);
    }
  };

  return (
    <StaffAuthLayout
      icon="🏪"
      title="Store Manager Login"
      subtitle="เข้าสู่ระบบร้านค้า UTCC Shop สำหรับจัดการและดู Analytics"
    >
      {/* ซ่อนข้อความยินดีต้อนรับเมื่อมี error แล้ว เพื่อไม่ให้สองกล่องขัดกันเอง */}
      {registeredEmail && !error && (
        <SuccessBanner message="สมัครบัญชีสำเร็จแล้ว กรุณาเข้าสู่ระบบเพื่อเริ่มใช้งาน" />
      )}
      <ErrorBanner message={error} />

      <form onSubmit={handleLogin}>
        <div style={{ marginBottom: '20px' }}>
          <label htmlFor="email" style={labelStyle}>อีเมลร้านค้า (Email)</label>
          <input
            id="email"
            type="email"
            required
            autoComplete="email"
            value={form.email}
            onChange={updateField('email')}
            placeholder="you@example.com"
            style={inputStyle}
            {...focusHandlers}
          />
        </div>

        <div style={{ marginBottom: '24px' }}>
          <label htmlFor="password" style={labelStyle}>รหัสผ่าน (Password)</label>
          <PasswordInput
            id="password"
            required
            // มาจากหน้าสมัคร อีเมลถูกเติมให้แล้ว จึงโฟกัสที่ช่องรหัสผ่านเลย
            autoFocus={Boolean(registeredEmail)}
            autoComplete="current-password"
            value={form.password}
            onChange={updateField('password')}
            placeholder="••••••••"
          />
        </div>

        <button type="submit" disabled={loading} style={submitButtonStyle(loading)}>
          {loading ? 'กำลังตรวจสอบ...' : 'เข้าสู่ระบบ Dashboard'}
        </button>
      </form>

      <p style={{ marginTop: '24px', textAlign: 'center', fontSize: '13px', color: '#6b7280' }}>
        ยังไม่มีบัญชีพนักงาน?{' '}
        <Link to={STAFF_REGISTER_PATH} style={{ color: '#1e3a5f', fontWeight: 600 }}>
          สมัครบัญชีร้านค้า
        </Link>
      </p>
      <p style={{ marginTop: '8px', textAlign: 'center', fontSize: '13px', color: '#6b7280' }}>
        <Link to={STAFF_RESET_PASSWORD_PATH} style={{ color: '#1e3a5f', fontWeight: 600 }}>
          ลืมรหัสผ่าน?
        </Link>
      </p>
    </StaffAuthLayout>
  );
}
