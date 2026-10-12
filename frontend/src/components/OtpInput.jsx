import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from 'react';

/**
 * ช่องกรอก OTP 6 หลัก และปุ่ม "ส่งรหัสอีกครั้ง" ที่หน้ายืนยันอีเมลกับหน้าตั้งรหัสผ่านใหม่ใช้ร่วมกัน
 */

export const OTP_LENGTH = 6;
// ต้องตรงกับ EmailOtpService.RESEND_COOLDOWN ฝั่ง backend
export const RESEND_COOLDOWN_SECONDS = 60;

export const emptyDigits = () => Array(OTP_LENGTH).fill('');

// ช่องหดได้ตามความกว้างการ์ด จอมือถือแคบๆ จึงไม่ล้น แต่ไม่กว้างเกิน 48px บนจอใหญ่
const digitBoxStyle = {
  flex: '1 1 0',
  minWidth: 0,
  maxWidth: '48px',
  height: '56px',
  textAlign: 'center',
  fontSize: '22px',
  fontWeight: '700',
  color: '#0f1b2d',
  borderRadius: '10px',
  border: '1.5px solid #e5e7eb',
  outline: 'none',
  boxSizing: 'border-box',
  transition: 'border-color 0.2s',
};

/**
 * digits เป็น array 6 ช่องที่ผู้เรียกถือ state ไว้เอง
 * onComplete ถูกเรียกเมื่อกรอกครบทุกช่อง — ไม่ส่งมาก็ได้ถ้าต้องรอกดปุ่มเอง
 * ref.focus() ใช้โฟกัสช่องแรกหลังล้างรหัส เช่นตอนกรอกผิด
 */
const OtpInput = forwardRef(function OtpInput({ digits, onChange, onComplete, disabled, autoFocus }, ref) {
  const inputRefs = useRef([]);

  const focusBox = (index) => inputRefs.current[index]?.focus();

  useImperativeHandle(ref, () => ({ focus: () => focusBox(0) }), []);

  const update = (next) => {
    onChange(next);
    if (onComplete && next.every((d) => d !== '')) {
      onComplete(next.join(''));
    }
  };

  const handleChange = (index) => (e) => {
    const value = e.target.value.replace(/\D/g, '');
    if (!value) {
      const next = [...digits];
      next[index] = '';
      onChange(next);
      return;
    }

    // บางคีย์บอร์ดมือถือเติมรหัสจาก SMS/อีเมลให้ทีเดียวทั้งก้อน — กระจายลงช่องถัดๆ ไป
    const next = [...digits];
    value.slice(0, OTP_LENGTH - index).split('').forEach((d, offset) => {
      next[index + offset] = d;
    });
    focusBox(Math.min(index + value.length, OTP_LENGTH - 1));
    update(next);
  };

  const handleKeyDown = (index) => (e) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      const next = [...digits];
      next[index - 1] = '';
      onChange(next);
      focusBox(index - 1);
    } else if (e.key === 'ArrowLeft' && index > 0) {
      focusBox(index - 1);
    } else if (e.key === 'ArrowRight' && index < OTP_LENGTH - 1) {
      focusBox(index + 1);
    }
  };

  const handlePaste = (e) => {
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
    if (!pasted) return;
    e.preventDefault();
    const next = emptyDigits();
    pasted.split('').forEach((d, i) => { next[i] = d; });
    focusBox(Math.min(pasted.length, OTP_LENGTH - 1));
    update(next);
  };

  return (
    <div
      role="group"
      aria-label={`รหัส OTP ${OTP_LENGTH} หลัก`}
      style={{ display: 'flex', justifyContent: 'center', gap: '8px' }}
    >
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(el) => { inputRefs.current[index] = el; }}
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          // ให้ iOS/Android เสนอรหัสจากอีเมล/SMS ให้กรอกอัตโนมัติ
          autoComplete={index === 0 ? 'one-time-code' : 'off'}
          autoFocus={index === 0 && autoFocus}
          maxLength={OTP_LENGTH}
          aria-label={`หลักที่ ${index + 1}`}
          value={digit}
          disabled={disabled}
          onChange={handleChange(index)}
          onKeyDown={handleKeyDown(index)}
          onPaste={handlePaste}
          onFocus={(e) => { e.target.select(); e.target.style.borderColor = '#1e3a5f'; }}
          onBlur={(e) => { e.target.style.borderColor = '#e5e7eb'; }}
          style={digitBoxStyle}
        />
      ))}
    </div>
  );
});

export default OtpInput;

/** นับถอยหลังทีละวินาทีจนถึง 0 — คืน [วินาทีที่เหลือ, ตั้งค่าใหม่] */
export function useCountdown(initialSeconds = 0) {
  const [seconds, setSeconds] = useState(initialSeconds);

  useEffect(() => {
    if (seconds <= 0) return undefined;
    const timer = setTimeout(() => setSeconds((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [seconds]);

  return [seconds, setSeconds];
}

/** ปุ่มขอรหัสใหม่ — ระหว่างนับถอยหลังกดไม่ได้ และบอกว่าต้องรออีกกี่วินาที */
export function ResendOtpButton({ cooldown, sending, disabled, onClick }) {
  const isDisabled = cooldown > 0 || sending || disabled;
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={isDisabled}
      style={{
        background: 'none',
        border: 'none',
        padding: 0,
        font: 'inherit',
        fontWeight: 600,
        color: isDisabled ? '#9ca3af' : '#1e3a5f',
        cursor: isDisabled ? 'not-allowed' : 'pointer',
        textDecoration: isDisabled ? 'none' : 'underline',
      }}
    >
      {sending
        ? 'กำลังส่ง...'
        : cooldown > 0
          ? `ส่งรหัสอีกครั้งได้ใน ${cooldown} วินาที`
          : 'ส่งรหัสอีกครั้ง'}
    </button>
  );
}
