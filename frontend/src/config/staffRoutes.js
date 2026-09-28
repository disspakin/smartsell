/**
 * เส้นทางลับสำหรับผู้ใช้ร้านค้า — ไม่มีลิงก์จากหน้าลูกค้าชี้มาที่นี่
 *
 * ⚠️ การซ่อน URL เป็นเรื่องความสะดวก ไม่ใช่มาตรการความปลอดภัย
 * โค้ด React ทั้งหมดถูกส่งไปที่เบราว์เซอร์ของผู้ใช้ ใครเปิด DevTools ก็เห็น path เหล่านี้
 * ด่านจริงคือ SecurityConfig ฝั่ง backend ที่บังคับ role STORE_MANAGER กับ /api/admin/**
 * ทุกคำขอ ไม่ว่าคำขอนั้นจะมาจากหน้าเว็บของเราหรือ curl ก็ตาม
 *
 * รวมไว้ที่ไฟล์เดียวเพื่อให้เปลี่ยน path ได้จุดเดียวเวลาต้องการหมุนเส้นทาง
 */
export const STAFF_LOGIN_PATH = '/staff-login-x7k2';
export const STAFF_REGISTER_PATH = '/staff-register-x7k2';
export const STAFF_RESET_PASSWORD_PATH = '/staff-reset-x7k2';
export const STAFF_DASHBOARD_PATH = '/admin/dashboard';
