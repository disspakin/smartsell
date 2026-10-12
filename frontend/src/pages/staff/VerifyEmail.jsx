import { useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/client';
import { STAFF_LOGIN_PATH } from '../../config/staffRoutes';
import StaffAuthLayout, {
  ErrorBanner,
  SuccessBanner,
  focusHandlers,
  inputStyle,
  labelStyle,
  submitButtonStyle,
} from '../../components/StaffAuthLayout';
import OtpInput, {
  OTP_LENGTH,
  RESEND_COOLDOWN_SECONDS,
  ResendOtpButton,
  emptyDigits,
  useCountdown,
} from '../../components/OtpInput';

export default function StaffVerifyEmail() {
  const location = useLocation();
  const navigate = useNavigate();

  // มาจากหน้าสมัคร (justSent) หรือหน้าล็อกอิน (บัญชียังไม่ยืนยัน) ทาง router state
  // ถ้าเปิดหน้านี้ตรงๆ ไม่มีอีเมลติดมา จะให้กรอกอีเมลเอง
  const presetEmail = location.state?.email || '';
  const justSent = Boolean(location.state?.justSent);

  const [email, setEmail] = useState(presetEmail);
  const [digits, setDigits] = useState(emptyDigits);
  // มาจากหน้าสมัคร = เพิ่งส่งรหัสไป ต้องรอก่อน ส่วนมาจากหน้าล็อกอินรหัสเดิมอาจหมดอายุแล้ว จึงให้ขอใหม่ได้ทันที
  const [cooldown, setCooldown] = useCountdown(justSent ? RESEND_COOLDOWN_SECONDS : 0);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState(
    justSent ? `เราส่งรหัส ${OTP_LENGTH} หลักไปที่ ${presetEmail} แล้ว รหัสใช้ได้ 5 นาที` : ''
  );

  const otpRef = useRef(null);

  const resetDigits = () => {
    setDigits(emptyDigits());
    otpRef.current?.focus();
  };

  const submitCode = async (code) => {
    if (loading) return;
    if (!email) {
      setError('กรุณากรอกอีเมลที่ใช้สมัคร');
      return;
    }
    setError('');
    setNotice('');
    setLoading(true);
    try {
      await authApi.verifyOtp(email, code);
      navigate(STAFF_LOGIN_PATH, { replace: true, state: { verifiedEmail: email } });
    } catch (err) {
      setError(err.fieldErrors?.code || err.message || 'ยืนยันรหัสไม่สำเร็จ กรุณาลองใหม่อีกครั้ง');
      resetDigits();
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const code = digits.join('');
    if (code.length !== OTP_LENGTH) {
      setError(`กรุณากรอกรหัสให้ครบ ${OTP_LENGTH} หลัก`);
      return;
    }
    submitCode(code);
  };

  const handleResend = async () => {
    if (!email) {
      setError('กรุณากรอกอีเมลที่ใช้สมัคร');
      return;
    }
    setError('');
    setNotice('');
    setResending(true);
    try {
      const res = await authApi.resendOtp(email);
      setNotice(res.message);
      setCooldown(RESEND_COOLDOWN_SECONDS);
      resetDigits();
    } catch (err) {
      // 429 = ยังไม่ครบ cooldown หรือขอบ่อยเกินโควตา — นับถอยหลังตามที่ backend บอก
      if (err.status === 429 && err.retryAfter) setCooldown(err.retryAfter);
      setError(err.message || 'ส่งรหัสใหม่ไม่สำเร็จ กรุณาลองใหม่อีกครั้ง');
    } finally {
      setResending(false);
    }
  };

  return (
    <StaffAuthLayout
      icon="✉️"
      title="ยืนยันอีเมล"
      subtitle={`กรอกรหัส ${OTP_LENGTH} หลักที่ส่งไปทางอีเมล เพื่อเปิดใช้งานบัญชีร้านค้า`}
    >
      {!error && <SuccessBanner message={notice} />}
      <ErrorBanner message={error} />

      <form onSubmit={handleSubmit}>
        {!presetEmail && (
          <div style={{ marginBottom: '20px' }}>
            <label htmlFor="email" style={labelStyle}>อีเมลที่ใช้สมัคร (Email)</label>
            <input
              id="email"
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              style={inputStyle}
              {...focusHandlers}
            />
          </div>
        )}

        <label style={labelStyle}>รหัสยืนยัน (OTP)</label>
        <div style={{ marginBottom: '24px' }}>
          {/* ใส่ตัวเลขครบ 6 ช่องเมื่อไหร่ส่งตรวจทันที ไม่ต้องกดปุ่ม */}
          <OtpInput
            ref={otpRef}
            digits={digits}
            onChange={setDigits}
            onComplete={submitCode}
            disabled={loading}
            autoFocus={Boolean(presetEmail)}
          />
        </div>

        <button type="submit" disabled={loading} style={submitButtonStyle(loading)}>
          {loading ? 'กำลังตรวจสอบ...' : 'ยืนยันอีเมล'}
        </button>
      </form>

      <p style={{ marginTop: '24px', textAlign: 'center', fontSize: '13px', color: '#6b7280' }}>
        ไม่ได้รับรหัส?{' '}
        <ResendOtpButton cooldown={cooldown} sending={resending} disabled={loading} onClick={handleResend} />
      </p>
      <p style={{ marginTop: '8px', textAlign: 'center', fontSize: '13px', color: '#6b7280' }}>
        <Link to={STAFF_LOGIN_PATH} style={{ color: '#1e3a5f', fontWeight: 600 }}>
          กลับไปหน้าเข้าสู่ระบบ
        </Link>
      </p>
    </StaffAuthLayout>
  );
}
