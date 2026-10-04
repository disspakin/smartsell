import { useLocation } from 'react-router-dom';
import Navbar from './Navbar';
import BottomNav from './BottomNav';
import { STAFF_DASHBOARD_PATH, STAFF_LOGIN_PATH, STAFF_REGISTER_PATH, STAFF_RESET_PASSWORD_PATH, STAFF_PRODUCT_ANALYTICS_PATH } from '../config/staffRoutes';

/**
 * หน้าของฝั่งร้านค้าทั้งหมด — ไม่ต้องมีเมนูของลูกค้าครอบ
 * Dashboard มีแถบหัวของตัวเองพร้อมชื่อผู้ใช้และปุ่มออกจากระบบอยู่แล้ว
 */
const CHROME_FREE_PATHS = [STAFF_LOGIN_PATH, STAFF_REGISTER_PATH, STAFF_RESET_PASSWORD_PATH, STAFF_DASHBOARD_PATH, STAFF_PRODUCT_ANALYTICS_PATH];

/**
 * ครอบหน้าเว็บด้วยเมนูบน/ล่างของฝั่งลูกค้า ยกเว้นหน้าเข้าสู่ระบบของร้านค้า
 *
 * นอกจากทำให้พื้นหลังของหน้า staff เต็มจอจริงแล้ว ยังตรงกับเจตนาของ hidden route
 * ด้วย — หน้าที่ลูกค้าไม่ควรเห็น ก็ไม่ควรมีเมนูของลูกค้าครอบอยู่
 */
export default function SiteChrome({ children }) {
  const { pathname } = useLocation();
  const hideChrome = CHROME_FREE_PATHS.includes(pathname);

  return (
    <>
      {!hideChrome && <Navbar />}
      {children}
      {!hideChrome && <BottomNav />}
    </>
  );
}
