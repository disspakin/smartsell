import { useEffect, useState } from 'react';
import '../styles/staff_auth.css';

/**
 * กรอบหน้าจอและสไตล์ฟอร์มที่หน้า Login กับ Register ของฝั่งร้านค้าใช้ร่วมกัน
 */

export const labelStyle = {
  display: 'block',
  fontSize: '13px',
  fontWeight: '600',
  color: '#374151',
  marginBottom: '6px',
};

export const inputStyle = {
  width: '100%',
  padding: '12px 16px',
  borderRadius: '10px',
  border: '1.5px solid #e5e7eb',
  fontSize: '14px',
  outline: 'none',
  boxSizing: 'border-box',
  transition: 'border-color 0.2s',
};

export const focusHandlers = {
  onFocus: (e) => { e.target.style.borderColor = '#1e3a5f'; },
  onBlur: (e) => { e.target.style.borderColor = '#e5e7eb'; },
};

const EyeIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
    <circle cx="12" cy="12" r="3" />
  </svg>
);

const EyeOffIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-6.5 0-10-7-10-7a18.45 18.45 0 0 1 5.06-5.94" />
    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c6.5 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19" />
    <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
    <line x1="2" y1="2" x2="22" y2="22" />
  </svg>
);

/** ช่องรหัสผ่านพร้อมปุ่มไอคอนตาสำหรับสลับแสดง/ซ่อนรหัสผ่าน */
export function PasswordInput(props) {
  const [visible, setVisible] = useState(false);

  return (
    <div style={{ position: 'relative' }}>
      <input
        {...props}
        type={visible ? 'text' : 'password'}
        style={{ ...inputStyle, paddingRight: '46px' }}
        {...focusHandlers}
      />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        aria-label={visible ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน'}
        aria-pressed={visible}
        title={visible ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน'}
        style={{
          position: 'absolute',
          top: '50%',
          right: '6px',
          transform: 'translateY(-50%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '36px',
          height: '36px',
          padding: 0,
          border: 'none',
          borderRadius: '8px',
          background: 'transparent',
          color: '#6b7280',
          cursor: 'pointer',
        }}
      >
        {visible ? <EyeOffIcon /> : <EyeIcon />}
      </button>
    </div>
  );
}

export function submitButtonStyle(loading) {
  return {
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
  };
}

export function ErrorBanner({ message }) {
  if (!message) return null;
  return (
    <div style={{
      backgroundColor: '#fef2f2',
      border: '1px solid #fecaca',
      color: '#991b1b',
      padding: '12px 16px',
      borderRadius: '10px',
      fontSize: '13.5px',
      marginBottom: '20px',
    }}>
      ⚠️ {message}
    </div>
  );
}

export function SuccessBanner({ message }) {
  if (!message) return null;
  return (
    <div style={{
      backgroundColor: '#f0fdf4',
      border: '1px solid #bbf7d0',
      color: '#166534',
      padding: '12px 16px',
      borderRadius: '10px',
      fontSize: '13.5px',
      marginBottom: '20px',
    }}>
      ✅ {message}
    </div>
  );
}

/** แสดงข้อความ validation รายฟิลด์ที่ backend ส่งกลับมาใน fieldErrors */
export function FieldError({ message }) {
  if (!message) return null;
  return (
    <div style={{ color: '#b91c1c', fontSize: '12px', marginTop: '6px' }}>{message}</div>
  );
}

export default function StaffAuthLayout({ icon, title, subtitle, children }) {
  // ทาพื้นหลังถึงระดับ <body> ระหว่างอยู่หน้านี้ แล้วคืนค่าเดิมเมื่อออกจากหน้า
  // จำเป็นเพราะตอนดึงจอเกินขอบบนมือถือจะเห็นสีของ body ไม่ใช่สีของ div
  useEffect(() => {
    document.body.classList.add('staff-auth-page');
    return () => document.body.classList.remove('staff-auth-page');
  }, []);

  return (
    <div className="staff-auth">
      <div className="staff-auth__card">
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
            {icon}
          </div>
          <h2 style={{ fontSize: '24px', color: '#0f1b2d', marginBottom: '8px', fontFamily: 'Fraunces, serif' }}>
            {title}
          </h2>
          <p style={{ fontSize: '13.5px', color: '#6b7280', margin: 0 }}>
            {subtitle}
          </p>
        </div>

        {children}
      </div>
    </div>
  );
}
