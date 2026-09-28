import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/client';
import { STAFF_LOGIN_PATH } from '../../config/staffRoutes';
import StaffAuthLayout, {
  ErrorBanner,
  FieldError,
  focusHandlers,
  inputStyle,
  labelStyle,
  submitButtonStyle,
  PasswordInput,
} from '../../components/StaffAuthLayout';

const MIN_PASSWORD_LENGTH = 8;

export default function StaffRegister() {
  const [form, setForm] = useState({
    displayName: '',
    email: '',
    password: '',
    confirmPassword: '',
    inviteCode: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  // ข้อความ validation รายฟิลด์ที่ backend ส่งกลับมา แสดงใต้ช่องกรอกที่เกี่ยวข้อง
  const [fieldErrors, setFieldErrors] = useState({});

  const navigate = useNavigate();

  const updateField = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});

    // ตรวจฝั่งหน้าเว็บเพื่อให้ผู้ใช้รู้ผลทันทีเท่านั้น
    // backend ตรวจซ้ำอีกรอบเสมอ (@Valid ที่ RegisterRequest) เพราะฝั่งนี้ถูกข้ามได้
    if (form.password !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'รหัสผ่านทั้งสองช่องไม่ตรงกัน' });
      return;
    }
    if (form.password.length < MIN_PASSWORD_LENGTH) {
      setFieldErrors({ password: `รหัสผ่านต้องยาวอย่างน้อย ${MIN_PASSWORD_LENGTH} ตัวอักษร` });
      return;
    }

    setLoading(true);
    try {
      const created = await authApi.register(form.email, form.password, form.displayName, form.inviteCode);

      // ไม่ล็อกอินให้อัตโนมัติ — ส่งไปหน้าล็อกอินพร้อมอีเมลที่เพิ่งสมัคร
      // เพื่อให้กรอกรหัสผ่านช่องเดียวก็เข้าได้ ไม่ต้องพิมพ์อีเมลซ้ำ
      navigate(STAFF_LOGIN_PATH, {
        replace: true,
        state: { registeredEmail: created.email },
      });
    } catch (err) {
      if (err.fieldErrors) setFieldErrors(err.fieldErrors);
      setError(err.message || 'สมัครบัญชีไม่สำเร็จ กรุณาลองใหม่อีกครั้ง');
    } finally {
      setLoading(false);
    }
  };

  return (
    <StaffAuthLayout
      icon="📝"
      title="สมัครบัญชีร้านค้า"
      subtitle="สร้างบัญชีพนักงาน/เจ้าของร้าน สมัครเสร็จแล้วเข้าสู่ระบบอีกครั้งเพื่อใช้งาน"
    >
      <ErrorBanner message={error} />

      <form onSubmit={handleRegister}>
        <div style={{ marginBottom: '20px' }}>
          <label htmlFor="displayName" style={labelStyle}>ชื่อที่ใช้แสดง (Display Name)</label>
          <input
            id="displayName"
            type="text"
            required
            maxLength={100}
            autoComplete="name"
            value={form.displayName}
            onChange={updateField('displayName')}
            placeholder="เช่น ผู้จัดการร้าน UTCC Shop"
            style={inputStyle}
            {...focusHandlers}
          />
          <FieldError message={fieldErrors.displayName} />
        </div>

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
          <FieldError message={fieldErrors.email} />
        </div>

        <div style={{ marginBottom: '20px' }}>
          <label htmlFor="password" style={labelStyle}>รหัสผ่าน (Password)</label>
          <PasswordInput
            id="password"
            required
            minLength={MIN_PASSWORD_LENGTH}
            autoComplete="new-password"
            value={form.password}
            onChange={updateField('password')}
            placeholder={`อย่างน้อย ${MIN_PASSWORD_LENGTH} ตัวอักษร`}
          />
          <FieldError message={fieldErrors.password} />
        </div>

        <div style={{ marginBottom: '20px' }}>
          <label htmlFor="confirmPassword" style={labelStyle}>ยืนยันรหัสผ่าน</label>
          <PasswordInput
            id="confirmPassword"
            required
            autoComplete="new-password"
            value={form.confirmPassword}
            onChange={updateField('confirmPassword')}
            placeholder="••••••••"
          />
          <FieldError message={fieldErrors.confirmPassword} />
        </div>

        <div style={{ marginBottom: '24px' }}>
          <label htmlFor="inviteCode" style={labelStyle}>รหัสเชิญ (Invite Code)</label>
          <PasswordInput
            id="inviteCode"
            required
            autoComplete="off"
            value={form.inviteCode}
            onChange={updateField('inviteCode')}
            placeholder="ขอรหัสจากผู้ดูแลร้าน"
          />
          <FieldError message={fieldErrors.inviteCode} />
        </div>

        <button type="submit" disabled={loading} style={submitButtonStyle(loading)}>
          {loading ? 'กำลังสร้างบัญชี...' : 'สมัครบัญชี'}
        </button>
      </form>

      <p style={{ marginTop: '24px', textAlign: 'center', fontSize: '13px', color: '#6b7280' }}>
        มีบัญชีอยู่แล้ว?{' '}
        <Link to={STAFF_LOGIN_PATH} style={{ color: '#1e3a5f', fontWeight: 600 }}>
          เข้าสู่ระบบ
        </Link>
      </p>
    </StaffAuthLayout>
  );
}
