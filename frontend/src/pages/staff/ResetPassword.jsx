import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../../api/client';
import { STAFF_LOGIN_PATH } from '../../config/staffRoutes';
import StaffAuthLayout, {
  ErrorBanner,
  FieldError,
  SuccessBanner,
  focusHandlers,
  inputStyle,
  labelStyle,
  submitButtonStyle,
  PasswordInput,
} from '../../components/StaffAuthLayout';

const MIN_PASSWORD_LENGTH = 8;

export default function StaffResetPassword() {
  const [form, setForm] = useState({
    email: '',
    inviteCode: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const updateField = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleReset = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setFieldErrors({});

    // ตรวจฝั่งหน้าเว็บเพื่อให้ผู้ใช้รู้ผลทันทีเท่านั้น backend ตรวจซ้ำเสมอ
    if (form.newPassword !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'รหัสผ่านทั้งสองช่องไม่ตรงกัน' });
      return;
    }
    if (form.newPassword.length < MIN_PASSWORD_LENGTH) {
      setFieldErrors({ newPassword: `รหัสผ่านต้องยาวอย่างน้อย ${MIN_PASSWORD_LENGTH} ตัวอักษร` });
      return;
    }

    setLoading(true);
    try {
      // backend ตอบข้อความเดียวกันไม่ว่าอีเมลจะมีบัญชีหรือไม่ จึงแสดงข้อความนั้นตรงๆ
      const result = await authApi.resetPassword(form.email, form.inviteCode, form.newPassword);
      setSuccess(result.message);
      setForm({ ...form, inviteCode: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      if (err.fieldErrors) setFieldErrors(err.fieldErrors);
      setError(err.message || 'รีเซ็ตรหัสผ่านไม่สำเร็จ กรุณาลองใหม่อีกครั้ง');
    } finally {
      setLoading(false);
    }
  };

  return (
    <StaffAuthLayout
      icon="🔑"
      title="ตั้งรหัสผ่านใหม่"
      subtitle="ยืนยันตัวตนด้วยรหัสเชิญของร้าน แล้วตั้งรหัสผ่านใหม่สำหรับบัญชีของคุณ"
    >
      {success && !error && <SuccessBanner message={success} />}
      <ErrorBanner message={error} />

      <form onSubmit={handleReset}>
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

        <div style={{ marginBottom: '20px' }}>
          <label htmlFor="newPassword" style={labelStyle}>รหัสผ่านใหม่ (New Password)</label>
          <PasswordInput
            id="newPassword"
            required
            minLength={MIN_PASSWORD_LENGTH}
            autoComplete="new-password"
            value={form.newPassword}
            onChange={updateField('newPassword')}
            placeholder={`อย่างน้อย ${MIN_PASSWORD_LENGTH} ตัวอักษร`}
          />
          <FieldError message={fieldErrors.newPassword} />
        </div>

        <div style={{ marginBottom: '24px' }}>
          <label htmlFor="confirmPassword" style={labelStyle}>ยืนยันรหัสผ่านใหม่</label>
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

        <button type="submit" disabled={loading} style={submitButtonStyle(loading)}>
          {loading ? 'กำลังบันทึก...' : 'ตั้งรหัสผ่านใหม่'}
        </button>
      </form>

      <p style={{ marginTop: '24px', textAlign: 'center', fontSize: '13px', color: '#6b7280' }}>
        <Link to={STAFF_LOGIN_PATH} style={{ color: '#1e3a5f', fontWeight: 600 }}>
          กลับไปหน้าเข้าสู่ระบบ
        </Link>
      </p>
    </StaffAuthLayout>
  );
}
