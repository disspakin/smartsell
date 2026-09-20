import { useState, useEffect, useRef } from 'react';
import { api } from '../api/client';

const SESSION_KEY = 'smartsell_support_session';
const SESSION_TTL_MS = 30 * 60 * 1000; // 30 minutes

function loadPersistedSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const { sessionId, messages, savedAt } = JSON.parse(raw);
    if (Date.now() - savedAt > SESSION_TTL_MS) {
      localStorage.removeItem(SESSION_KEY);
      return null;
    }
    return { sessionId, messages };
  } catch {
    return null;
  }
}

function saveSession(sessionId, messages) {
  localStorage.setItem(SESSION_KEY, JSON.stringify({
    sessionId, messages, savedAt: Date.now(),
  }));
}

const INITIAL_MESSAGE = {
  sender: 'AI',
  content: 'สวัสดีค่ะ! ยินดีต้อนรับสู่บริการแชทสอบถามข้อมูลสินค้าและบริการ UTCC Shop ตลอด 24 ชม. มีข้อมูลใดให้หนูช่วยดูแล สามารถสอบถามได้เลยนะคะ 😊',
};

export default function ChatSupport() {
  const persisted = loadPersistedSession();

  const [sessionId, setSessionId] = useState(persisted?.sessionId ?? null);
  const [messages, setMessages] = useState(persisted?.messages ?? [INITIAL_MESSAGE]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [ratingSubmitted, setRatingSubmitted] = useState(false);
  const [hoveredStar, setHoveredStar] = useState(0);

  // Persist session on every change
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  useEffect(() => {
    if (sessionId) saveSession(sessionId, messages);
  }, [sessionId, messages]);

  async function send(text) {
    if (!text || !text.trim() || loading) return;
    const userText = text.trim();
    setMessages((m) => [...m, { sender: 'USER', content: userText }]);
    setInput('');
    setLoading(true);

    try {
      const res = await api.sendSupportMessage(sessionId, userText);
      setSessionId(res.sessionId);
      setMessages((m) => [...m, { sender: 'AI', content: res.reply }]);
    } catch (err) {
      setMessages((m) => [
        ...m,
        {
          sender: 'AI',
          content: 'ขออภัยค่ะ ระบบแชทขัดข้องชั่วคราว สามารถติดต่อสอบถามโดยตรงกับร้านค้า UTCC Shop ได้ที่เบอร์ 02-697-6000 นะคะ',
        },
      ]);
    } finally {
      setLoading(false);
    }
  }

  function handleRating(star) {
    if (ratingSubmitted) return;
    setRatingSubmitted(true);
    api.submitRating({
      sessionId,
      eventType: 'SUPPORT_RATING',
      rating: star,
    });
  }

  return (
    <div className="app" style={{ maxWidth: 680, margin: '0 auto', padding: '48px 20px 40px' }}>
      {/* Header */}
      <div style={{ textAlign: 'center', marginBottom: 28 }}>
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 6,
            background: '#ecfdf5',
            color: '#065f46',
            padding: '4px 12px',
            borderRadius: 20,
            fontSize: 12,
            fontWeight: 600,
            marginBottom: 10,
          }}
        >
          <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#10b981' }}></span>
          เจ้าหน้าที่ AI ออนไลน์ พร้อมบริการ 24 ชม.
        </div>
        <h1 style={{ fontFamily: "'Fraunces','Noto Serif Thai',serif", fontSize: 28, margin: '0 0 8px', color: 'var(--ink)' }}>
          แชทกับเรา 🎧
        </h1>
        <p style={{ color: 'var(--text-dim)', fontSize: 13.5, margin: 0 }}>
          สอบถามข้อมูลสินค้า ไซส์ หรือข้อมูลร้านค้า UTCC Shop จากฐานข้อมูลได้ทันที
        </p>
      </div>

      {/* Messages Container */}
      <div
        style={{
          background: 'var(--parchment)',
          borderRadius: 20,
          padding: '24px 20px',
          minHeight: 380,
          maxHeight: 520,
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          gap: 16,
          marginBottom: 18,
          border: '1px solid var(--line)',
        }}
      >
        {messages.map((m, idx) => (
          <div
            key={idx}
            style={{
              alignSelf: m.sender === 'USER' ? 'flex-end' : 'flex-start',
              maxWidth: '82%',
              display: 'flex',
              flexDirection: 'column',
              alignItems: m.sender === 'USER' ? 'flex-end' : 'flex-start',
            }}
          >
            {m.sender === 'AI' && (
              <span style={{ fontSize: 11, color: 'var(--text-dim)', marginBottom: 4, fontWeight: 600 }}>
                🤖 UTCC Support AI
              </span>
            )}
            <div
              style={{
                padding: '12px 18px',
                borderRadius: 16,
                fontSize: 14,
                lineHeight: 1.6,
                background: m.sender === 'USER' ? 'var(--ink)' : '#fff',
                color: m.sender === 'USER' ? '#fff' : 'var(--text)',
                border: m.sender === 'USER' ? 'none' : '1px solid var(--line)',
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                wordBreak: 'break-word',
              }}
            >
              {m.content}
            </div>
          </div>
        ))}
        {loading && (
          <div style={{ alignSelf: 'flex-start', color: 'var(--text-dim)', fontSize: 12.5, fontStyle: 'italic' }}>
            เจ้าหน้าที่ AI กำลังพิมพ์...
          </div>
        )}
        <div ref={bottomRef} />

        {/* Feedback rating block */}
        {messages.length > 2 && !loading && (
          <div
            style={{
              alignSelf: 'center',
              marginTop: 12,
              padding: '10px 18px',
              backgroundColor: '#fff',
              border: '1px solid #e2e8f0',
              borderRadius: 12,
              fontSize: 12,
              textAlign: 'center',
            }}
          >
            <div style={{ fontWeight: 600, color: '#475569', marginBottom: 4 }}>
              {ratingSubmitted ? 'ขอบคุณสำหรับคะแนนประเมินบริการค่ะ! ⭐' : 'ให้คะแนนความพึงพอใจการบริการนี้'}
            </div>
            {!ratingSubmitted && (
              <div style={{ display: 'flex', gap: 6, fontSize: 20, justifyContent: 'center', cursor: 'pointer' }}>
                {[1, 2, 3, 4, 5].map((star) => (
                  <span
                    key={star}
                    onClick={() => handleRating(star)}
                    onMouseEnter={() => setHoveredStar(star)}
                    onMouseLeave={() => setHoveredStar(0)}
                    style={{
                      transform: hoveredStar >= star ? 'scale(1.2)' : 'scale(1)',
                      transition: 'transform 0.15s',
                    }}
                  >
                    ⭐
                  </span>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Input box */}
      <div
        style={{
          display: 'flex',
          gap: 8,
          border: '1.5px solid var(--line)',
          borderRadius: 30,
          padding: '6px 6px 6px 18px',
          background: '#fff',
          boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
        }}
      >
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && send(input)}
          placeholder="พิมพ์คำถามของคุณที่นี่..."
          disabled={loading}
          style={{ flex: 1, border: 'none', outline: 'none', fontSize: 14, fontFamily: 'inherit' }}
        />
        <button
          onClick={() => send(input)}
          disabled={loading || !input.trim()}
          style={{
            width: 38,
            height: 38,
            borderRadius: '50%',
            background: input.trim() ? 'var(--ink)' : 'var(--line)',
            color: '#fff',
            border: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: input.trim() ? 'pointer' : 'default',
            fontSize: 16,
            transition: 'background 0.2s',
          }}
        >
          ↑
        </button>
      </div>
    </div>
  );
}
