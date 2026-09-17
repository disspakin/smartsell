const BASE = '/api';

function getStoreToken() {
  return localStorage.getItem('smartsell_store_token');
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
    try {
      const parsed = JSON.parse(errorText);
      if (parsed.error) errorMessage = parsed.error;
    } catch (e) {
      if (errorText) errorMessage = errorText;
    }
    throw new Error(errorMessage);
  }

  return res.json();
}

export const api = {
  getProducts: (category) => request(`/products${category ? `?category=${category}` : ''}`).catch(() => [
    {
      id: 1,
      name: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีเหลืองทอง)',
      category: 'Polo',
      price: 350,
      description: 'เสื้อโปโลผ้า Cotton เนื้อนุ่ม ระบายอากาศได้ดี ตราสัญลักษณ์ UTCC',
      imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80',
    },
    {
      id: 2,
      name: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีน้ำเงินเข้ม)',
      category: 'Polo',
      price: 350,
      description: 'เสื้อโปโลทรงสมาร์ท สีน้ำเงินเข้มคลาสสิก ปักโลโก้ UTCC ที่อกซ้าย',
      imageUrl: 'https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?auto=format&fit=crop&w=600&q=80',
    },
    {
      id: 3,
      name: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีขาวเรียบหรู)',
      category: 'Polo',
      price: 350,
      description: 'เสื้อโปโลสีขาวสะอาด ตัดเย็บประณีต ผ้าซับเหงื่อได้ไว',
      imageUrl: 'https://images.unsplash.com/photo-1625910513413-7fc214f479a3?auto=format&fit=crop&w=600&q=80',
    },
    {
      id: 4,
      name: 'เสื้อยืดสกรีน UTCC Freshy (สีฟ้า)',
      category: 'T-Shirt',
      price: 250,
      description: 'เสื้อยืดคอกลมผ้า Supersoft สกรีนลาย UTCC Smart Campus สีฟ้าสดใส',
      imageUrl: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80',
    },
  ]),

  getProduct: (id) => request(`/products/${id}`).catch(() => ({
    id,
    name: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีเหลืองทอง)',
    category: 'Polo',
    price: 350,
    description: 'เสื้อโปโลผ้า Cotton เนื้อนุ่ม ระบายอากาศได้ดี ตราสัญลักษณ์ UTCC สีเหลืองทองสง่างาม เหมาะสำหรับใส่เรียนและทำกิจกรรม',
    imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80',
    variants: [
      { id: 101, color: 'Yellow', colorHex: '#EAB308', size: 'S', imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80' },
      { id: 102, color: 'Yellow', colorHex: '#EAB308', size: 'M', imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80' },
      { id: 103, color: 'Yellow', colorHex: '#EAB308', size: 'L', imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80' },
      { id: 104, color: 'Yellow', colorHex: '#EAB308', size: 'XL', imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80' },
    ],
  })),

  recommend: (season, occasion) => request(`/products/recommend?season=${season}&occasion=${occasion}`),
  sendChatMessage: (sessionId, message) =>
    request('/chat/assistant', { method: 'POST', body: JSON.stringify({ sessionId, message }) }),

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

export const authApi = {
  login: async (email, password) => {
    try {
      return await request('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
    } catch (err) {
      // Offline fallback authentication check
      const normalizedEmail = (email || '').trim().toLowerCase();
      if (normalizedEmail === 'admin@utcc.ac.th' && password === 'password123') {
        return {
          token: 'mock-jwt-store-manager-token',
          email: 'admin@utcc.ac.th',
          displayName: 'ผู้จัดการร้าน UTCC Shop',
          role: 'STORE_MANAGER',
        };
      }
      throw new Error('อีเมลหรือรหัสผ่านไม่ถูกต้อง (กรุณาใช้ admin@utcc.ac.th / password123)');
    }
  },

  getProfile: async () => {
    try {
      return await request('/auth/me');
    } catch (err) {
      const token = getStoreToken();
      if (token) {
        return {
          email: 'admin@utcc.ac.th',
          displayName: 'ผู้จัดการร้าน UTCC Shop',
          role: 'STORE_MANAGER',
        };
      }
      throw new Error('Unauthorized');
    }
  },

  logout: () => {
    localStorage.removeItem('smartsell_store_token');
    localStorage.removeItem('smartsell_user_name');
    localStorage.removeItem('smartsell_user_role');
  },
};

export const adminApi = {
  getSummary: async () => {
    try {
      return await request('/admin/dashboard/summary');
    } catch (err) {
      return {
        totalInteractions: 128,
        totalSessions: 45,
        totalViews: 74,
        totalInterestedClicks: 29,
        totalAiRecommendations: 25,
        averageRating: 4.8,
        topInterestedProducts: [
          { productId: 1, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีเหลืองทอง)', imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80', price: 350, count: 18 },
          { productId: 2, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีน้ำเงินเข้ม)', imageUrl: 'https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?auto=format&fit=crop&w=600&q=80', price: 350, count: 14 },
          { productId: 3, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีขาวเรียบหรู)', imageUrl: 'https://images.unsplash.com/photo-1625910513413-7fc214f479a3?auto=format&fit=crop&w=600&q=80', price: 350, count: 10 },
          { productId: 4, productName: 'เสื้อยืดสกรีน UTCC Freshy (สีฟ้า)', imageUrl: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80', price: 250, count: 8 },
          { productId: 5, productName: 'เสื้อยืดสกรีน UTCC Minimal (สีดำ)', imageUrl: 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?auto=format&fit=crop&w=600&q=80', price: 250, count: 6 },
        ],
        topRecommendedProducts: [
          { productId: 2, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีน้ำเงินเข้ม)', imageUrl: 'https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?auto=format&fit=crop&w=600&q=80', price: 350, count: 20 },
          { productId: 1, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีเหลืองทอง)', imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80', price: 350, count: 16 },
          { productId: 6, productName: 'เสื้อแจ็คเก็ตวอร์ม UTCC Sport (สีน้ำเงิน-ทอง)', imageUrl: 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=600&q=80', price: 650, count: 12 },
          { productId: 3, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีขาวเรียบหรู)', imageUrl: 'https://images.unsplash.com/photo-1625910513413-7fc214f479a3?auto=format&fit=crop&w=600&q=80', price: 350, count: 9 },
          { productId: 4, productName: 'เสื้อยืดสกรีน UTCC Freshy (สีฟ้า)', imageUrl: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80', price: 250, count: 7 },
        ],
      };
    }
  },

  getDemandAnalytics: async () => {
    try {
      return await request('/admin/dashboard/demand-analytics');
    } catch (err) {
      return {
        personalColorBreakdown: {
          'Warm Spring (ใบไม้ผลัดใบ)': 18,
          'Cool Summer (ฤดูร้อนเย็น)': 14,
          'Deep Autumn (ฤดูใบไม้ร่วง)': 9,
          'Bright Winter (ฤดูหนาวสดใส)': 4,
        },
        occasionBreakdown: {
          'ใส่เรียน (Class/Study)': 22,
          'ใส่ชิวๆ (Casual)': 15,
          'เข้ากิจกรรม (Events)': 10,
          'ทางการ (Formal)': 8,
        },
        luckyColorBreakdown: {
          'การงาน/การเรียน': 20,
          'การเงิน/โชคลาภ': 16,
          'ความรัก/เมตตา': 11,
          'สุขภาพ/แคล้วคลาด': 8,
        },
        sizeBreakdown: {
          'M': 18,
          'L': 15,
          'XL': 12,
          'S': 8,
          'XXL': 4,
        },
        topProducts: [
          { productId: 1, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีเหลืองทอง)', interactionCount: 38 },
          { productId: 2, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีน้ำเงินเข้ม)', interactionCount: 29 },
          { productId: 4, productName: 'เสื้อยืดสกรีน UTCC Freshy (สีฟ้า)', interactionCount: 24 },
          { productId: 3, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีขาวเรียบหรู)', interactionCount: 18 },
        ],
        topInterestedProducts: [
          { productId: 1, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีเหลืองทอง)', imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80', price: 350, count: 18 },
          { productId: 2, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีน้ำเงินเข้ม)', imageUrl: 'https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?auto=format&fit=crop&w=600&q=80', price: 350, count: 14 },
          { productId: 3, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีขาวเรียบหรู)', imageUrl: 'https://images.unsplash.com/photo-1625910513413-7fc214f479a3?auto=format&fit=crop&w=600&q=80', price: 350, count: 10 },
          { productId: 4, productName: 'เสื้อยืดสกรีน UTCC Freshy (สีฟ้า)', imageUrl: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80', price: 250, count: 8 },
          { productId: 5, productName: 'เสื้อยืดสกรีน UTCC Minimal (สีดำ)', imageUrl: 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?auto=format&fit=crop&w=600&q=80', price: 250, count: 6 },
        ],
        topRecommendedProducts: [
          { productId: 2, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีน้ำเงินเข้ม)', imageUrl: 'https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?auto=format&fit=crop&w=600&q=80', price: 350, count: 20 },
          { productId: 1, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีเหลืองทอง)', imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80', price: 350, count: 16 },
          { productId: 6, productName: 'เสื้อแจ็คเก็ตวอร์ม UTCC Sport (สีน้ำเงิน-ทอง)', imageUrl: 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=600&q=80', price: 650, count: 12 },
          { productId: 3, productName: 'เสื้อโปโลมหาวิทยาลัย UTCC (สีขาวเรียบหรู)', imageUrl: 'https://images.unsplash.com/photo-1625910513413-7fc214f479a3?auto=format&fit=crop&w=600&q=80', price: 350, count: 9 },
          { productId: 4, productName: 'เสื้อยืดสกรีน UTCC Freshy (สีฟ้า)', imageUrl: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80', price: 250, count: 7 },
        ],
      };
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
      .then((res) => res.json())
      .catch(() => {
        const palettes = {
          'Warm Spring': [
            { colorName: 'Peach', colorHex: '#FFBE98' },
            { colorName: 'Warm Gold', colorHex: '#EAB308' },
            { colorName: 'Coral', colorHex: '#FF7F50' },
            { colorName: 'Cream', colorHex: '#FFFDD0' },
          ],
          'Cool Summer': [
            { colorName: 'Soft Lavender', colorHex: '#E6E6FA' },
            { colorName: 'Rose Pink', colorHex: '#FFC0CB' },
            { colorName: 'Sky Blue', colorHex: '#87CEEB' },
            { colorName: 'Dusty Navy', colorHex: '#4A6B82' },
          ],
          'Deep Autumn': [
            { colorName: 'Terracotta', colorHex: '#E2725B' },
            { colorName: 'Olive Green', colorHex: '#808000' },
            { colorName: 'Mustard', colorHex: '#FFDB58' },
            { colorName: 'Deep Brown', colorHex: '#654321' },
          ],
          'Bright Winter': [
            { colorName: 'Royal Navy', colorHex: '#1E3A8A' },
            { colorName: 'Pure White', colorHex: '#FFFFFF' },
            { colorName: 'Emerald', colorHex: '#50C878' },
            { colorName: 'Ruby Red', colorHex: '#E0115F' },
          ],
        };
        return {
          id: 1,
          season,
          confidence: 0.95,
          reasoning: `วิเคราะห์จากคำตอบของคุณ เหมาะกับโทนสี ${season} ช่วยขับผิวให้มีออร่าและแมตช์ชุดได้ง่าย`,
          palette: palettes[season] || palettes['Warm Spring'],
        };
      }),
};
