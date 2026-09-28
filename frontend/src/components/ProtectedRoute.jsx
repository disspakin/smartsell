import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { authApi } from '../api/client';
import { STAFF_LOGIN_PATH } from '../config/staffRoutes';

const CHECKING = 'CHECKING';
const ALLOWED = 'ALLOWED';
const DENIED = 'DENIED';

/**
 * กันหน้าเว็บที่ต้องล็อกอิน
 *
 * ยืนยันสิทธิ์ด้วยการถาม backend (/api/auth/me) ไม่ใช่ด้วยการอ่าน role จาก localStorage
 * เพราะค่าใน localStorage ผู้ใช้แก้เองได้ทั้งหมด การกันตรงนี้เป็นเรื่อง UX
 * (พาไปหน้าล็อกอินแทนที่จะเห็นหน้าเปล่า) ส่วนการกันข้อมูลจริงอยู่ที่ role check ของ backend
 */
export default function ProtectedRoute({ children, allowedRoles = ['STORE_MANAGER', 'ADMIN'] }) {
  const [state, setState] = useState(CHECKING);

  useEffect(() => {
    let active = true;

    if (!authApi.getSession().token) {
      setState(DENIED);
      return undefined;
    }

    authApi.getProfile()
      .then((profile) => {
        if (!active) return;
        setState(allowedRoles.includes(profile.role) ? ALLOWED : DENIED);
      })
      .catch(() => {
        if (!active) return;
        authApi.logout();
        setState(DENIED);
      });

    // กัน setState หลัง component ถูก unmount ไปแล้ว
    return () => { active = false; };
  }, []);

  if (state === CHECKING) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <div style={{ fontSize: '16px', color: '#6b7280' }}>กำลังตรวจสอบสิทธิ์เข้าใช้งาน...</div>
      </div>
    );
  }

  if (state === DENIED) {
    return <Navigate to={STAFF_LOGIN_PATH} replace />;
  }

  return children;
}
