const BASE = '/api';

// รวมชื่อคีย์ไว้ที่เดียว กันสะกดไม่ตรงกันเวลาอ่าน/ลบจากหลายไฟล์
const STORAGE_KEYS = {
  token: 'smartsell_store_token',
  name: 'smartsell_user_name',
  role: 'smartsell_user_role',
};

function getStoreToken() {
  return localStorage.getItem(STORAGE_KEYS.token);
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const token = getStoreToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(BASE + path, {
    ...options,
    headers,
  });

  if (!res.ok) {
    const errorText = await res.text();
    let errorMessage = `API error ${res.status}`;
    let fieldErrors = null;
    try {
      const parsed = JSON.parse(errorText);
      if (parsed.error) errorMessage = parsed.error;
      if (parsed.fieldErrors) fieldErrors = parsed.fieldErrors;
    } catch (e) {
      if (errorText) errorMessage = errorText;
    }

    // แนบ status ไว้กับ error เพื่อให้ผู้เรียกแยกได้ว่า "เซิร์ฟเวอร์ล่ม" (ควร fallback)
    // ต่างจาก "ไม่มีสิทธิ์" (ห้าม fallback ต้องเด้งไปหน้าล็อกอิน)
    const error = new Error(errorMessage);
    error.status = res.status;
    error.fieldErrors = fieldErrors;
    throw error;
  }

  return res.json();
}

/** 401 = ยังไม่ล็อกอิน / token ใช้ไม่ได้, 403 = ล็อกอินแล้วแต่ role ไม่ถึง */
function isAuthError(err) {
  return err?.status === 401 || err?.status === 403;
}

export const api = {
  getProducts: (category) => request(`/products${category ? `?category=${category}` : ''}`),

  getProduct: (id) => request(`/products/${id}`),

  sendChatMessage: (sessionId, message) =>
    request('/chat/assistant', { method: 'POST', body: JSON.stringify({ sessionId, message }) }),
  sendSupportMessage: (sessionId, message) =>
    request('/chat/support', { method: 'POST', body: JSON.stringify({ sessionId, message }) }),

  // บันทึก Event การกดถูกใจ / AI แนะนำสำเร็จ
  logInteraction: (payload) =>
    request('/interactions', {
      method: 'POST',
      body: JSON.stringify(payload),
    }).catch(() => ({ ok: true })), // offline-safe: ไม่ throw error

  // ส่งคะแนนดาวความพึงพอใจ AI (1-5)
  submitRating: (payload) =>
    request('/interactions/rating', {
      method: 'POST',
      body: JSON.stringify(payload),
    }).catch(() => ({ ok: true })), // offline-safe: ไม่ throw error
};

/**
 * Auth ของผู้ใช้ร้านค้า
 *
 * ไม่มี offline fallback ในส่วนนี้โดยตั้งใจ — ของเดิมเคยปล่อยให้ผ่านเมื่อ backend
 * ไม่ตอบสนอง ซึ่งเท่ากับใครก็ใส่ token มั่วๆ ลง localStorage แล้วเข้า Dashboard ได้
 * การตัดสินว่าใครเข้าได้ต้องมาจาก backend เท่านั้น
 */
export const authApi = {
  /** คืนข้อมูลผู้ใช้ที่สร้าง ไม่มี token — ผู้สมัครต้องไปล็อกอินเองอีกครั้ง */
  register: (email, password, displayName, inviteCode) =>
    request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, displayName, inviteCode }),
    }),

  login: (email, password) =>
    request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),

  /** ตั้งรหัสผ่านใหม่ด้วย invite code — คืน { message } ข้อความเดียวกันเสมอเมื่อ code ถูก */
  resetPassword: (email, inviteCode, newPassword) =>
    request('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ email, inviteCode, newPassword }),
    }),

  getProfile: () => request('/auth/me'),

  /** เก็บผลลัพธ์จาก login/register ลง localStorage */
  saveSession: ({ token, email, displayName, role }) => {
    localStorage.setItem(STORAGE_KEYS.token, token);
    localStorage.setItem(STORAGE_KEYS.name, displayName || email);
    localStorage.setItem(STORAGE_KEYS.role, role);
  },

  getSession: () => ({
    token: localStorage.getItem(STORAGE_KEYS.token),
    name: localStorage.getItem(STORAGE_KEYS.name),
    role: localStorage.getItem(STORAGE_KEYS.role),
  }),

  /**
   * ออกจากระบบ — ลบ token ออกจากเครื่อง
   *
   * ข้อจำกัดที่ควรรู้: JWT เป็น stateless ตัว token ที่ออกไปแล้วจะยังใช้ได้จนหมดอายุ
   * (app.jwt.expiration-ms) ถ้าใครคัดลอกเก็บไว้ก่อนกดออกจากระบบ
   * ถ้าต้องการให้ตายทันทีต้องทำ blacklist ฝั่ง server เพิ่ม
   */
  logout: () => {
    Object.values(STORAGE_KEYS).forEach((key) => localStorage.removeItem(key));
  },
};

export const adminApi = {
  getSummary: async () => {
    try {
      return await request('/admin/dashboard/summary');
    } catch (err) {
      // ลบ mock data ทิ้ง ให้แสดง error ตามความจริงเมื่อ backend พัง
      throw err;
    }
  },

  getDemandAnalytics: async () => {
    try {
      return await request('/admin/dashboard/demand-analytics');
    } catch (err) {
      throw err;
    }
  },

  getProductAnalytics: async () => {
    try {
      return await request('/admin/dashboard/products/analytics');
    } catch (err) {
      if (isAuthError(err)) throw err;
      return [];
    }
  },
};

function getPcSessionToken() {
  let token = localStorage.getItem('pc_session_token');
  if (!token) {
    token = 'anon-' + Math.random().toString(36).slice(2);
    localStorage.setItem('pc_session_token', token);
  }
  return token;
}

export const personalColorApi = {
  chooseManual: (season) =>
    fetch('/api/personal-color/manual', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionToken: getPcSessionToken(), season }),
    })
      .then((res) => res.json()),
};
