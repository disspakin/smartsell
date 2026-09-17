import { useState } from 'react';
import { api } from '../api/client';

export default function Assistant() {
  const [sessionId, setSessionId] = useState(null);
  const [messages, setMessages] = useState([
    { sender: 'AI', content: 'ขอถาม Personal Color ก่อนนะ — โทนสีของคุณเป็นแบบไหน?', products: [] },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [ratingSubmitted, setRatingSubmitted] = useState(false);
  const [hoveredStar, setHoveredStar] = useState(0);

  async function send(text) {
    if (!text.trim()) return;
    setMessages((m) => [...m, { sender: 'USER', content: text, products: [] }]);
    setInput('');
    setLoading(true);
    try {
      const res = await api.sendChatMessage(sessionId, text);
      setSessionId(res.sessionId);
      setMessages((m) => [...m, { sender: 'AI', content: res.reply, products: res.products || [] }]);
    } catch (e) {
      setMessages((m) => [...m, { sender: 'AI', content: 'ขออภัย เชื่อมต่อ AI ไม่สำเร็จ ลองใหม่อีกครั้งนะคะ', products: [] }]);
    } finally {
      setLoading(false);
    }
  }

  function handleRating(star) {
    if (ratingSubmitted) return;
    setRatingSubmitted(true);
    // ส่งคะแนนไปยัง Backend
    api.submitRating({
      sessionId,
      eventType: 'AI_RATING',
      rating: star,
    });
  }

  return (
    <div className="app" style={{ maxWidth: 640, padding: '56px 24px 40px', textAlign: 'center' }}>
      <div className="eyebrow">Personal Shopping Assistant</div>
      <h1 style={{ fontSize: 24, marginTop: 12 }}>ให้ฉันช่วยแนะนำเสื้อผ้าที่เหมาะกับคุณดีไหม?</h1>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, margin: '28px 0', textAlign: 'left' }}>
        {messages.map((m, i) => (
          <div key={i} style={{ display: 'flex', flexDirection: 'column', alignSelf: m.sender === 'USER' ? 'flex-end' : 'flex-start', maxWidth: '85%' }}>
            {/* Chat bubble */}
            <div
              style={{
                padding: '14px 18px',
                borderRadius: 16,
                fontSize: 14,
                lineHeight: 1.6,
                background: m.sender === 'USER' ? 'var(--ink)' : '#fff',
                color: m.sender === 'USER' ? '#fff' : 'var(--text)',
                border: m.sender === 'USER' ? 'none' : '1px solid var(--line)',
                borderLeft: m.sender === 'AI' ? '3px solid var(--accent)' : 'none',
              }}
            >
              {m.content}
            </div>

            {/* Product cards — shown after AI bubble only, when products exist */}
            {m.sender === 'AI' && m.products && m.products.length > 0 && (
              <div style={{ marginTop: 10 }}>
                <div style={{ fontSize: 11.5, fontWeight: 600, color: 'var(--text-dim)', marginBottom: 8 }}>
                  สินค้าแนะนำสำหรับคุณ
                </div>
                <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                  {m.products.map((p, idx) => (
                    <div key={p.id} style={{
                      width: 130, background: '#fff', border: '1px solid var(--line)',
                      borderRadius: 12, overflow: 'hidden', position: 'relative',
                    }}>
                      <div style={{
                        position: 'absolute', top: 6, left: 6, fontSize: 16,
                        filter: 'drop-shadow(0 1px 2px rgba(0,0,0,.15))',
                      }}>
                        {['🥇', '🥈', '🥉'][idx]}
                      </div>
                      <div style={{ height: 90, overflow: 'hidden', background: 'var(--parchment-deep)' }}>
                        {p.imageUrl
                          ? <img src={p.imageUrl} alt={p.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                          : <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24 }}>👕</div>}
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
        {loading && <div style={{ color: 'var(--text-dim)', fontSize: 13 }}>AI กำลังพิมพ์...</div>}

        {/* ===== AI Rating Block ===== */}
        {messages.length > 1 && !loading && (
          <div style={{
            alignSelf: 'flex-start',
            marginTop: 8,
            padding: '12px 16px',
            backgroundColor: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: 12,
            fontSize: 12.5,
          }}>
            <div style={{ fontWeight: 600, color: '#475569', marginBottom: 6 }}>
              {ratingSubmitted ? 'ขอบคุณสำหรับคะแนนประเมิน AI! ⭐' : 'ให้คะแนนการแนะนำของ AI ครั้งนี้ (1-5 ดาว)'}
            </div>
            {!ratingSubmitted && (
              <div style={{ display: 'flex', gap: 6, fontSize: 22, cursor: 'pointer' }}>
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
                  >
                    ⭐
                  </span>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      <div style={{ display: 'flex', gap: 8, border: '1.5px solid var(--line)', borderRadius: 30, padding: '6px 6px 6px 18px' }}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && send(input)}
          placeholder="พิมพ์คำตอบของคุณ..."
          style={{ flex: 1, border: 'none', outline: 'none', fontSize: 14, fontFamily: 'inherit' }}
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
