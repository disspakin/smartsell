import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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
import OtpInput, {
  OTP_LENGTH,
  RESEND_COOLDOWN_SECONDS,
  ResendOtpButton,
  emptyDigits,
  useCountdown,
} from '../../components/OtpInput';

const MIN_PASSWORD_LENGTH = 8;

/**
 * ตั้งรหัสผ่านใหม่ 2 ขั้น
 * 1. กรอกอีเมล → backend ส่ง OTP ไปที่อีเมลนั้น (ถ้ามีบัญชี)
 * 2. กรอก OTP + รหัสผ่านใหม่
 *
 * ต้องเปิดกล่องจดหมายของบัญชีได้จริงเท่านั้น — เดิมใช้ invite code ที่พนักงานทุกคนรู้
 * ซึ่งทำให้ใครก็ตั้งรหัสใหม่ให้บัญชีคนอื่นได้
 */
export default function StaffResetPassword() {
  const navigate = useNavigate();

  const [step, setStep] = useState('email'); // 'email' | 'code'
  const [email, setEmail] = useState('');
  const [digits, setDigits] = useState(emptyDigits);
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' });
  const [cooldown, setCooldown] = useCountdown(0);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const otpRef = useRef(null);

  const updateField = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const clearMessages = () => {
    setError('');
    setNotice('');
    setFieldErrors({});
  };

  /**
   * ขอ OTP — ใช้ทั้งขั้นที่ 1 และปุ่มส่งรหัสอีกครั้ง
   * ได้ 429 ก็ยังพาไปขั้นที่ 2 เพราะแปลว่าเพิ่งส่งรหัสไปไม่นาน รหัสนั้นอาจยังใช้ได้อยู่
   */
  const requestCode = async () => {
    clearMessages();
    try {
      // backend ตอบข้อความเดียวกันไม่ว่าอีเมลจะมีบัญชีหรือไม่ จึงแสดงข้อความนั้นตรงๆ
      const res = await authApi.forgotPassword(email);
      setNotice(res.message);
      setCooldown(RESEND_COOLDOWN_SECONDS);
      setStep('code');
      setDigits(emptyDigits());
      otpRef.current?.focus();
    } catch (err) {
      if (err.status === 429) {
        if (err.retryAfter) setCooldown(err.retryAfter);
        setStep('code');
      }
      if (err.fieldErrors) setFieldErrors(err.fieldErrors);
      setError(err.message || 'ส่งรหัสไม่สำเร็จ กรุณาลองใหม่อีกครั้ง');
    }
  };

  const handleRequestCode = async (e) => {
    e.preventDefault();
    setLoading(true);
    await requestCode();
    setLoading(false);
  };

  const handleResend = async () => {
    setResending(true);
    await requestCode();
    setResending(false);
  };

  const handleReset = async (e) => {
    e.preventDefault();
    clearMessages();

    const code = digits.join('');
    // ตรวจฝั่งหน้าเว็บเพื่อให้ผู้ใช้รู้ผลทันทีเท่านั้น backend ตรวจซ้ำเสมอ
    if (code.length !== OTP_LENGTH) {
      setError(`กรุณากรอกรหัสให้ครบ ${OTP_LENGTH} หลัก`);
      return;
    }
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
      await authApi.resetPassword(email, code, form.newPassword);
      navigate(STAFF_LOGIN_PATH, { replace: true, state: { passwordResetEmail: email } });
    } catch (err) {
      if (err.fieldErrors) setFieldErrors(err.fieldErrors);
      setError(err.fieldErrors?.code || err.message || 'ตั้งรหัสผ่านใหม่ไม่สำเร็จ กรุณาลองใหม่อีกครั้ง');
      // รหัสผ่านใหม่ไม่ผ่านเงื่อนไขก็แก้แค่ช่องรหัสผ่าน ไม่ต้องกรอก OTP ใหม่
      if (!err.fieldErrors?.newPassword) {
        setDigits(emptyDigits());
        otpRef.current?.focus();
      }
    } finally {
      setLoading(false);
    }
  };

  const backToEmailStep = () => {
    clearMessages();
    setStep('email');
    setDigits(emptyDigits());
  };

  return (
    <StaffAuthLayout
      icon="🔑"
      title="ตั้งรหัสผ่านใหม่"
      subtitle={step === 'email'
        ? 'กรอกอีเมลของบัญชี เราจะส่งรหัสสำหรับตั้งรหัสผ่านใหม่ไปให้'
        : `กรอกรหัส ${OTP_LENGTH} หลักที่ส่งไปที่ ${email} แล้วตั้งรหัสผ่านใหม่`}
    >
      {!error && <SuccessBanner message={notice} />}
      <ErrorBanner message={error} />

      {step === 'email' ? (
        <form onSubmit={handleRequestCode}>
          <div style={{ marginBottom: '24px' }}>
            <label htmlFor="email" style={labelStyle}>อีเมลร้านค้า (Email)</label>
            <input
              id="email"
              type="email"
              required
              autoFocus
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              style={inputStyle}
              {...focusHandlers}
            />
            <FieldError message={fieldErrors.email} />
          </div>

          <button type="submit" disabled={loading} style={submitButtonStyle(loading)}>
            {loading ? 'กำลังส่งรหัส...' : 'ส่งรหัสไปที่อีเมล'}
          </button>
        </form>
      ) : (
        <form onSubmit={handleReset}>
          <label style={labelStyle}>รหัสจากอีเมล (OTP)</label>
          <div style={{ marginBottom: '20px' }}>
            {/* ไม่ส่งตรวจเองเมื่อกรอกครบ เพราะยังต้องกรอกรหัสผ่านใหม่ต่อ */}
            <OtpInput ref={otpRef} digits={digits} onChange={setDigits} disabled={loading} autoFocus />
            <FieldError message={fieldErrors.code} />
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

          <p style={{ marginTop: '24px', textAlign: 'center', fontSize: '13px', color: '#6b7280' }}>
            ไม่ได้รับรหัส?{' '}
            <ResendOtpButton cooldown={cooldown} sending={resending} disabled={loading} onClick={handleResend} />
          </p>
          <p style={{ marginTop: '8px', textAlign: 'center', fontSize: '13px', color: '#6b7280' }}>
            <button
              type="button"
              onClick={backToEmailStep}
              style={{ background: 'none', border: 'none', padding: 0, font: 'inherit', fontWeight: 600, color: '#1e3a5f', cursor: 'pointer' }}
            >
              เปลี่ยนอีเมล
            </button>
          </p>
        </form>
      )}

      <p style={{ marginTop: '8px', textAlign: 'center', fontSize: '13px', color: '#6b7280' }}>
        <Link to={STAFF_LOGIN_PATH} style={{ color: '#1e3a5f', fontWeight: 600 }}>
          กลับไปหน้าเข้าสู่ระบบ
        </Link>
      </p>
    </StaffAuthLayout>
  );
}
