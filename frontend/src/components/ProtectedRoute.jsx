import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { authApi } from '../api/client';

export default function ProtectedRoute({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('smartsell_store_token');
    if (!token) {
      setIsAuthenticated(false);
      return;
    }

    authApi.getProfile()
      .then(() => setIsAuthenticated(true))
      .catch(() => {
        authApi.logout();
        setIsAuthenticated(false);
      });
  }, []);

  if (isAuthenticated === null) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <div style={{ fontSize: '16px', color: '#6b7280' }}>กำลังตรวจสอบสิทธิ์เข้าใช้งาน...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" replace />;
  }

  return children;
}
