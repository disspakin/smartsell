import { BrowserRouter, Routes, Route } from 'react-router-dom';
import SiteChrome from './components/SiteChrome';
import Home from './pages/Home';
import Products from './pages/Products';
import ProductDetail from './pages/ProductDetail';
import Assistant from './pages/Assistant';
import ChatSupport from './pages/ChatSupport';
import PersonalColorCheck from './pages/PersonalColorCheck';
import LuckyColor from './pages/LuckyColor';
import StaffLogin from './pages/staff/Login';
import StaffRegister from './pages/staff/Register';
import StaffResetPassword from './pages/staff/ResetPassword';
import AdminDashboard from './pages/admin/Dashboard';
import AdminProductAnalytics from './pages/admin/AdminProductAnalytics';
import ProtectedRoute from './components/ProtectedRoute';
import {
  STAFF_DASHBOARD_PATH,
  STAFF_LOGIN_PATH,
  STAFF_REGISTER_PATH,
  STAFF_RESET_PASSWORD_PATH,
  STAFF_PRODUCT_ANALYTICS_PATH,
} from './config/staffRoutes';

export default function App() {
  return (
    <BrowserRouter>
      <SiteChrome>
        <Routes>
          {/* ---- ส่วนหน้าร้านสำหรับลูกค้า ---- */}
          <Route path="/" element={<Home />} />
          <Route path="/products" element={<Products />} />
          <Route path="/products/:id" element={<ProductDetail />} />
          <Route path="/assistant" element={<Assistant />} />
          <Route path="/support" element={<ChatSupport />} />
          <Route path="/chat" element={<ChatSupport />} />
          <Route path="/personal-color" element={<PersonalColorCheck />} />
          <Route path="/lucky-color" element={<LuckyColor />} />

          {/* ---- ส่วนผู้ใช้ร้านค้า: ไม่มีลิงก์จาก Navbar/BottomNav ชี้มา ต้องรู้ URL ---- */}
          <Route path={STAFF_LOGIN_PATH} element={<StaffLogin />} />
          <Route path={STAFF_REGISTER_PATH} element={<StaffRegister />} />
          <Route path={STAFF_RESET_PASSWORD_PATH} element={<StaffResetPassword />} />
          <Route
            path={STAFF_DASHBOARD_PATH}
            element={
              <ProtectedRoute>
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path={STAFF_PRODUCT_ANALYTICS_PATH}
            element={
              <ProtectedRoute>
                <AdminProductAnalytics />
              </ProtectedRoute>
            }
          />
        </Routes>
      </SiteChrome>
    </BrowserRouter>
  );
}
