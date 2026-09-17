import { useState, useRef, useEffect } from 'react';
import { api } from '../api/client';

const THAI_COLOR_HEX = {
  'แดง': '#c0392b', 'เขียว': '#4caf50', 'เหลือง': '#f1c40f', 'ฟ้า': '#3498db',
  'ม่วง': '#8e44ad', 'ชมพู': '#e91e8c', 'ส้ม': '#e67e22', 'ดำ': '#1c1c1c',
  'ขาว': '#f5f5f5', 'น้ำเงิน': '#1e3a5f', 'เทา': '#95a5a6', 'น้ำตาล': '#8b5e3c',
};

const DAYS = ['จันทร์', 'อังคาร', 'พุธ', 'พฤหัส', 'ศุกร์', 'เสาร์', 'อาทิตย์'];
const GOALS = [
  { key: 'การเรียน', label: '📚 การเรียน' },
  { key: 'การงาน', label: '💼 การงาน' },
  { key: 'การเงิน', label: '💰 การเงิน' },
  { key: 'ความรัก', label: '💕 ความรัก' },
  { key: 'โชคลาภ', label: '🍀 โชคลาภ' },
  { key: 'ความมั่นใจ', label: '✨ ความมั่นใจ' },
];

function getSessionToken() {
  let token = localStorage.getItem('pc_session_token');
  if (!token) {
    token = 'anon-' + Math.random().toString(36).slice(2);
    localStorage.setItem('pc_session_token', token);
  }
  return token;
}

export default function LuckyColor() {
  const [day, setDay] = useState('จันทร์');
  const [goal, setGoal] = useState('การงาน');
  const [result, setResult] = useState(null);
  const [luckyRatingSubmitted, setLuckyRatingSubmitted] = useState(false);
  const [luckyHoveredStar, setLuckyHoveredStar] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const resultRef = useRef(null);
  const scrollRef = useRef(null);

  useEffect(() => {
    if (result && resultRef.current) {
      resultRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [result]);

  function scrollProducts(direction) {
    if (scrollRef.current) {
      scrollRef.current.scrollBy({ left: direction * 170, behavior: 'smooth' });
    }
  }

  async function handleSubmit() {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch('/api/lucky-color', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionToken: getSessionToken(), birthWeekday: day, luckyGoal: goal }),
      });
      const data = await res.json();
      setResult(data);
    } catch (e) {
      setError('เกิดข้อผิดพลาด ลองใหม่อีกครั้ง');
    } finally {
      setLoading(false);
    }
  }

  function handleLuckyRating(star) {
    if (luckyRatingSubmitted) return;
    setLuckyRatingSubmitted(true);
    api.submitRating({
      eventType: 'LUCKY_COLOR_RATING',
      rating: star,
    });
  }

  return (
    <div className="app" style={{ padding: '56px 24px 60px', maxWidth: 640, margin: '0 auto', textAlign: 'center' }}>
      <div className="eyebrow">สีมงคลวันนี้</div>
      <h1 style={{ fontSize: 30, marginTop: 12 }}>🔮 เสริมดวงด้วยสีมงคล</h1>
      <p style={{ color: 'var(--text-dim)', fontSize: 14, marginTop: 12, lineHeight: 1.7 }}>
        บอก AI ว่าเกิดวันอะไร และอยากเสริมด้านไหน แล้วให้ระบบแนะนำเสื้อผ้าสีมงคลที่เข้ากับคุณ
      </p>

      <div style={{ background: '#fff', border: '1px solid var(--line)', borderRadius: 16, padding: 28, marginTop: 24, textAlign: 'left' }}>
        <div style={{ fontSize: 12.5, fontWeight: 700, color: 'var(--ink)', marginBottom: 10 }}>เกิดวันอะไร?</div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
          {DAYS.map((d) => (
            <div
              key={d}
              onClick={() => setDay(d)}
              style={{
                padding: '9px 16px', borderRadius: 10, cursor: 'pointer', fontSize: 13, fontWeight: 600,
                border: day === d ? '1.5px solid var(--ink)' : '1.5px solid var(--line)',
                background: day === d ? 'var(--ink)' : '#fff',
                color: day === d ? '#fff' : 'var(--text)',
              }}
            >
              {d}
            </div>
          ))}
        </div>

        <div style={{ fontSize: 12.5, fontWeight: 700, color: 'var(--ink)', marginBottom: 10 }}>วันนี้อยากเสริมด้านอะไร</div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {GOALS.map((g) => (
            <div
              key={g.key}
              onClick={() => setGoal(g.key)}
              style={{
                padding: '9px 16px', borderRadius: 20, cursor: 'pointer', fontSize: 12.5, fontWeight: 600,
                border: goal === g.key ? '1.5px solid var(--accent)' : '1.5px solid var(--line)',
                background: goal === g.key ? 'var(--accent)' : '#fff',
                color: goal === g.key ? '#fff' : 'var(--text)',
              }}
            >
              {g.label}
            </div>
          ))}
        </div>
      </div>

      <div className="btn primary" style={{ display: 'inline-flex', marginTop: 24 }} onClick={handleSubmit}>
        {loading ? 'กำลังคำนวณ...' : '✨ ดูสีมงคลของฉัน'}
      </div>

      {error && <p style={{ color: '#b5652f', fontSize: 13, marginTop: 16 }}>{error}</p>}

      {result && (
        <div
          ref={resultRef}
          style={{
            marginTop: 20, background: 'linear-gradient(120deg,#fff,var(--parchment-deep))',
            border: '1px solid var(--line)', borderRadius: 22, padding: '20px 24px',
            scrollMarginTop: 80,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'center', gap: 10, marginBottom: 10 }}>
            {result.color.split('/').map((name, i) => (
              <div
                key={i}
                title={name.trim()}
                style={{
                  width: 36, height: 36, borderRadius: '50%',
                  background: THAI_COLOR_HEX[name.trim()] || result.colorHex || '#ccc',
                  boxShadow: '0 0 0 2px #fff, 0 0 0 3px var(--line)',
                }}
              />
            ))}
          </div>
          <div className="eyebrow">สีมงคลของคุณวันนี้</div>
          <h2 style={{ fontSize: 24, marginTop: 8 }}>{result.color}</h2>
          <p style={{ fontSize: 13.5, color: 'var(--text-dim)', marginTop: 12, lineHeight: 1.7 }}>
            {result.reasoning}
          </p>

          {result.products?.length > 0 && (
            <div style={{ position: 'relative' }}>
              <button
                onClick={() => scrollProducts(-1)}
                style={{
                  position: 'absolute', left: -6, top: '40%', zIndex: 5,
                  width: 30, height: 30, borderRadius: '50%',
                  border: '1px solid var(--line)', background: '#fff', cursor: 'pointer',
                  boxShadow: '0 2px 6px rgba(0,0,0,.15)', fontSize: 16,
                }}
              >
                ‹
              </button>
              <button
                onClick={() => scrollProducts(1)}
                style={{
                  position: 'absolute', right: -6, top: '40%', zIndex: 5,
                  width: 30, height: 30, borderRadius: '50%',
                  border: '1px solid var(--line)', background: '#fff', cursor: 'pointer',
                  boxShadow: '0 2px 6px rgba(0,0,0,.15)', fontSize: 16,
                }}
              >
                ›
              </button>
              <div
                ref={scrollRef}
                style={{
                  display: 'flex', gap: 14, marginTop: 22, overflowX: 'auto', paddingBottom: 0,
                  scrollSnapType: 'x mandatory', WebkitOverflowScrolling: 'touch',
                  scrollbarWidth: 'none', msOverflowStyle: 'none',
                }}
              >
                {result.products.map((p) => (
                  <div
                    key={p.id}
                    style={{
                      width: 150, flex: '0 0 auto', scrollSnapAlign: 'start',
                      background: '#fff', border: '1px solid var(--line)', borderRadius: 14,
                      overflow: 'hidden', textAlign: 'left',
                    }}
                  >
                    <div style={{ height: 110, overflow: 'hidden', background: 'var(--parchment-deep)' }}>
                      {p.imageUrl
                        ? <img src={p.imageUrl} alt={p.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        : <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28 }}>👕</div>}
                    </div>
                    <div style={{ padding: '10px 12px' }}>
                      <div style={{ fontFamily: "'Fraunces','Noto Serif Thai',serif", fontSize: 12, color: 'var(--ink)', lineHeight: 1.4 }}>{p.name}</div>
                      <div style={{ fontSize: 10.5, color: 'var(--text-dim)', marginTop: 4 }}>{p.price} บาท</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div style={{
            marginTop: 24,
            padding: '14px 18px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 14,
            display: 'inline-block',
          }}>
            <div style={{ fontSize: 12.5, fontWeight: 600, color: '#475569', marginBottom: 6 }}>
              {luckyRatingSubmitted ? 'ขอบคุณสำหรับคะแนนประเมิน! ⭐' : 'ให้คะแนนคำแนะนำสีมงคลนี้ (1-5 ดาว)'}
            </div>
            {!luckyRatingSubmitted && (
              <div style={{ display: 'flex', gap: 8, fontSize: 22, justifyContent: 'center', cursor: 'pointer' }}>
                {[1, 2, 3, 4, 5].map((star) => (
                  <span
                    key={star}
                    onClick={() => handleLuckyRating(star)}
                    onMouseEnter={() => setLuckyHoveredStar(star)}
                    onMouseLeave={() => setLuckyHoveredStar(0)}
                    style={{
                      transition: 'transform 0.15s',
                      transform: luckyHoveredStar >= star ? 'scale(1.25)' : 'scale(1)',
                      display: 'inline-block',
                    }}
                  >
                    ⭐
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}