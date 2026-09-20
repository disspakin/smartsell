import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';

const SESSION_KEY = 'smartsell_assistant_session';
const SESSION_TTL_MS = 30 * 60 * 1000; // 30 minutes

function loadPersistedSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const { sessionId, messages, preferences, savedAt } = JSON.parse(raw);
    if (Date.now() - savedAt > SESSION_TTL_MS) {
      localStorage.removeItem(SESSION_KEY);
      return null;
    }
    return { sessionId, messages, preferences };
  } catch {
    return null;
  }
}

function saveSession(sessionId, messages, preferences) {
  localStorage.setItem(SESSION_KEY, JSON.stringify({
    sessionId, messages, preferences, savedAt: Date.now(),
  }));
}

function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

const INITIAL_MESSAGE = {
  sender: 'AI',
  content: 'สวัสดีค่ะ! ฉันคือ SmartSell AI ✨ เพื่อช่วยแนะนำชุดที่เหมาะกับคุณที่สุด ขอถาม Personal Color ก่อนนะคะ — โทนสีของคุณเป็นแบบไหนคะ?',
  products: [],
};

export default function Assistant() {
  const persisted = loadPersistedSession();

  const [sessionId, setSessionId] = useState(persisted?.sessionId ?? null);
  const [preferences, setPreferences] = useState(persisted?.preferences ?? {});
  const [messages, setMessages] = useState(persisted?.messages ?? [INITIAL_MESSAGE]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [ratingSubmitted, setRatingSubmitted] = useState(false);
  const [hoveredStar, setHoveredStar] = useState(0);
  const [showColorHelp, setShowColorHelp] = useState(false); // show 2-choice card when user doesn't know personal color

  const bottomRef = useRef(null);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  // Persist session on every change
  useEffect(() => {
    if (sessionId) saveSession(sessionId, messages, preferences);
  }, [sessionId, messages, preferences]);

  async function send(text) {
    if (!text.trim() || loading) return;
    setShowColorHelp(false); // hide help card on new message
    const userMsg = { sender: 'USER', content: text, products: [] };
    setMessages((m) => [...m, userMsg]);
    setInput('');
    setLoading(true);
    try {
      const res = await api.sendChatMessage(sessionId, text);
      setSessionId(res.sessionId);
      if (res.preferences) setPreferences(res.preferences);
      setMessages((m) => [...m, { sender: 'AI', content: res.reply, products: res.products || [] }]);
    } catch {
      setMessages((m) => [...m, { sender: 'AI', content: 'ขออภัยค่ะ เชื่อมต่อ AI ไม่สำเร็จ ลองใหม่อีกครั้งนะคะ', products: [] }]);
    } finally {
      setLoading(false);
    }
  }

  function handleRating(star) {
    if (ratingSubmitted) return;
    setRatingSubmitted(true);
    api.submitRating({ sessionId, eventType: 'AI_RATING', rating: star });
  }

  function handleRestart() {
    clearSession();
    setSessionId(null);
    setPreferences({});
    setMessages([INITIAL_MESSAGE]);
    setInput('');
    setRatingSubmitted(false);
    setHoveredStar(0);
    setShowColorHelp(false);
  }

  // ─── Quick Replies Logic ──────────────────────────────────────────────────
  const lastMessage = messages[messages.length - 1];
  const isAILast = lastMessage?.sender === 'AI' && !loading;
  const hasRecommendedProducts = messages.some((m) => m.products && m.products.length > 0);

  let quickReplies = [];

  if (isAILast && !hasRecommendedProducts && !showColorHelp) {
    if (!preferences?.personalColor) {
      quickReplies = ['Spring', 'Summer', 'Autumn', 'Winter', 'ไม่รู้'];
    } else if (!preferences?.occasion) {
      quickReplies = ['ทั่วไป', 'เข้ากิจกรรม', 'ทางการ'];
    } else if (!preferences?.budget) {
      quickReplies = ['ต่ำกว่า 350', '300 - 500', 'มากกว่า 500'];
    } else if (!preferences?.size) {
      quickReplies = ['S', 'M', 'L', 'XL', 'XXL'];
    }
  }

  return (
    <div className="app" style={{ maxWidth: 640, padding: '56px 16px 100px', display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* Header */}
      <div style={{ textAlign: 'center', marginBottom: 24 }}>
        <div style={{ fontSize: 11, letterSpacing: 2, textTransform: 'uppercase', color: 'var(--accent)', fontWeight: 600, marginBottom: 8 }}>
          Personal Shopping Assistant
        </div>
        <h1 style={{ fontSize: 24, margin: 0 }}>ให้ฉันช่วยแนะนำเสื้อผ้าที่เหมาะกับคุณดีไหม?</h1>
      </div>

      {/* Session info bar — shown when restored */}
      {persisted && messages.length > 1 && (
        <div style={{
          fontSize: 11.5, color: 'var(--text-dim)', textAlign: 'center', marginBottom: 12,
          padding: '6px 14px', background: '#f1f5f9', borderRadius: 20, display: 'inline-flex',
          alignSelf: 'center', gap: 8, alignItems: 'center',
        }}>
          <span>📌 กลับมาต่อจากบทสนทนาเดิมค่ะ</span>
          <button
            onClick={handleRestart}
            style={{ fontSize: 11, color: '#ef4444', background: 'none', border: 'none', cursor: 'pointer', padding: 0, fontWeight: 600 }}
          >
            เริ่มใหม่
          </button>
        </div>
      )}

      {/* Chat messages */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 14, marginBottom: 20 }}>
        {messages.map((m, idx) => (
          <div
            key={idx}
            style={{
              alignSelf: m.sender === 'USER' ? 'flex-end' : 'flex-start',
              maxWidth: '85%',
              display: 'flex',
              flexDirection: 'column',
              alignItems: m.sender === 'USER' ? 'flex-end' : 'flex-start',
            }}
          >
            <div style={{
              padding: '12px 16px',
              borderRadius: 16,
              fontSize: 14,
              lineHeight: 1.65,
              background: m.sender === 'USER' ? 'var(--ink)' : '#fff',
              color: m.sender === 'USER' ? '#fff' : 'var(--text)',
              border: m.sender === 'USER' ? 'none' : '1px solid var(--line)',
              borderLeft: m.sender === 'AI' ? '3px solid var(--accent)' : 'none',
            }}>
              {m.content}
            </div>

            {/* Product cards */}
            {m.sender === 'AI' && m.products && m.products.length > 0 && (
              <div style={{ marginTop: 10, alignSelf: 'flex-start' }}>
                <div style={{ fontSize: 11.5, fontWeight: 600, color: 'var(--text-dim)', marginBottom: 8 }}>
                  สินค้าแนะนำสำหรับคุณ
                </div>
                <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                  {m.products.map((p, i) => (
                    <div 
                      key={p.id} 
                      onClick={() => {
                        api.logInteraction({ sessionId, productId: p.id, eventType: 'INTERESTED_CLICK' });
                        // Optionally open product details or link to it
                        // window.location.href = `/products/${p.id}`;
                      }}
                      style={{ width: 130, background: '#fff', border: '1px solid var(--line)', borderRadius: 12, overflow: 'hidden', position: 'relative', cursor: 'pointer' }}
                    >
                      <div style={{ position: 'absolute', top: 6, left: 6, fontSize: 16, filter: 'drop-shadow(0 1px 2px rgba(0,0,0,.15))' }}>
                        {['🥇', '🥈', '🥉'][i]}
                      </div>
                      <div style={{ height: 90, overflow: 'hidden', background: 'var(--parchment-deep)' }}>
                        {p.imageUrl
                          ? <img src={p.imageUrl} alt={p.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                          : <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24 }}>👕</div>
                        }
                      </div>
                      <div style={{ padding: '8px 10px' }}>
                        <div style={{ fontFamily: "'Fraunces','Noto Serif Thai',serif", fontSize: 10.5, color: 'var(--ink)', lineHeight: 1.4 }}>{p.name}</div>
                        <div style={{ fontSize: 10, color: 'var(--text-dim)', marginTop: 3 }}>{p.price} บาท</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div style={{ color: 'var(--text-dim)', fontSize: 13, alignSelf: 'flex-start' }}>
            AI กำลังพิมพ์...
          </div>
        )}



        {/* ===== Rating Block ===== */}
        {hasRecommendedProducts && !loading && (
          <div style={{
            alignSelf: 'center',
            width: '100%',
            marginTop: 12,
            padding: '16px 18px',
            backgroundColor: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: 14,
            fontSize: 12.5,
            textAlign: 'center',
          }}>
            <div style={{ fontWeight: 600, color: '#475569', marginBottom: 8 }}>
              {ratingSubmitted ? 'ขอบคุณสำหรับคะแนนประเมิน AI! ⭐' : 'ให้คะแนนการแนะนำของ AI ครั้งนี้ (1-5 ดาว)'}
            </div>
            {!ratingSubmitted && (
              <div style={{ display: 'flex', gap: 6, fontSize: 22, justifyContent: 'center', cursor: 'pointer', marginBottom: 12 }}>
                {[1, 2, 3, 4, 5].map((star) => (
                  <span
                    key={star}
                    onClick={() => handleRating(star)}
                    onMouseEnter={() => setHoveredStar(star)}
                    onMouseLeave={() => setHoveredStar(0)}
                    style={{
                      transition: 'transform 0.15s',
                      transform: hoveredStar >= star ? 'scale(1.25)' : 'scale(1)',
                      filter: hoveredStar >= star ? 'brightness(1)' : 'grayscale(0.3)',
                    }}
                  >⭐</span>
                ))}
              </div>
            )}
            {/* Restart button after recommendation */}
            <button
              onClick={handleRestart}
              style={{
                marginTop: 4,
                padding: '8px 20px',
                borderRadius: 20,
                border: '1.5px solid var(--accent)',
                background: '#fff',
                color: 'var(--accent-deep, #7c3aed)',
                fontSize: 13,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              🔄 เริ่มค้นหาชุดใหม่อีกครั้ง
            </button>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* ===== Quick Replies ===== */}
      {quickReplies.length > 0 && (
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16, justifyContent: 'center' }}>
          {quickReplies.map(reply => (
            <button
              key={reply}
              onClick={() => reply === 'ไม่รู้' ? setShowColorHelp(true) : send(reply)}
              style={{
                padding: '8px 16px',
                borderRadius: 20,
                border: '1px solid var(--accent)',
                background: '#fff',
                color: 'var(--accent-deep)',
                fontSize: 13,
                cursor: 'pointer',
                fontWeight: 500,
                boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
                transition: 'all 0.2s ease-in-out',
              }}
              onMouseEnter={(e) => { e.target.style.background = 'var(--parchment)'; }}
              onMouseLeave={(e) => { e.target.style.background = '#fff'; }}
            >
              {reply}
            </button>
          ))}
        </div>
      )}

      {/* ===== Personal Color Help Card — ขึ้นทันทีเมื่อกด ไม่รู้ ===== */}
      {showColorHelp && (
        <div style={{
          marginBottom: 16,
          padding: '16px 18px',
          background: '#fffbeb',
          border: '1.5px solid #fcd34d',
          borderRadius: 16,
          fontSize: 13.5,
          lineHeight: 1.65,
        }}>
          <div style={{ fontWeight: 600, marginBottom: 8, color: '#92400e' }}>
            🎨 ยังไม่รู้ Personal Color ของตัวเองใช่ไหมคะ?
          </div>
          <div style={{ color: '#78350f', marginBottom: 14, fontSize: 13 }}>
            แนะนำให้ลองทำ <strong>Personal Color Check</strong> ก่อนเลยค่ะ
            เพื่อให้ได้สินค้าที่ตรงโทนสีผิวมากที่สุด ✨
          </div>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <Link
              to="/personal-color"
              style={{
                padding: '9px 18px',
                borderRadius: 20,
                background: 'var(--accent-deep, #7c3aed)',
                color: '#fff',
                fontSize: 13,
                fontWeight: 600,
                textDecoration: 'none',
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
              }}
            >
              🔍 ไปทำ Personal Color Check
            </Link>
            <button
              onClick={() => send('ข้ามไปคำถามต่อไปได้เลยค่ะ')}
              style={{
                padding: '9px 18px',
                borderRadius: 20,
                border: '1.5px solid #d97706',
                background: '#fff',
                color: '#92400e',
                fontSize: 13,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              ⏭ ข้ามไปคำถามต่อไป
            </button>
          </div>
        </div>
      )}

      {/* Input bar */}
      <div style={{ display: 'flex', gap: 8, border: '1.5px solid var(--line)', borderRadius: 30, padding: '6px 6px 6px 18px', background: '#fff' }}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && send(input)}
          placeholder="พิมพ์คำตอบของคุณ..."
          style={{ flex: 1, border: 'none', outline: 'none', fontSize: 14, fontFamily: 'inherit', background: 'transparent' }}
        />
        <div
          onClick={() => send(input)}
          style={{ width: 38, height: 38, borderRadius: '50%', background: 'var(--accent-deep)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flex: 'none' }}
        >
          ↑
        </div>
      </div>
    </div>
  );
}
