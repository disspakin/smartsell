import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { personalColorApi } from '../api/client';

/* ===== illustrations for the quiz questions ===== */
function WristIllustration({ veinColor }) {
  return (
    <svg width="110" height="80" viewBox="0 0 120 90" fill="none">
      <path d="M20 85 C20 60 25 40 30 25 C33 15 45 10 50 20 C53 28 50 40 48 50 L48 85 Z" fill="#f0d5bc" />
      <path d="M70 85 C70 60 75 40 80 25 C83 15 95 10 100 20 C103 28 100 40 98 50 L98 85 Z" fill="#f0d5bc" />
      <path d="M33 78 Q34 60 36 45" stroke={veinColor} strokeWidth="2" fill="none" opacity="0.75" />
      <path d="M40 80 Q41 62 42 48" stroke={veinColor} strokeWidth="2" fill="none" opacity="0.75" />
      <path d="M83 78 Q84 60 86 45" stroke={veinColor} strokeWidth="2" fill="none" opacity="0.75" />
      <path d="M90 80 Q91 62 92 48" stroke={veinColor} strokeWidth="2" fill="none" opacity="0.75" />
    </svg>
  );
}
function HairIllustration({ colors }) {
  return (
    <svg width="110" height="80" viewBox="0 0 120 90" fill="none">
      <path d="M30 15 C15 20 12 45 18 75 C22 82 30 84 34 78 C30 60 32 40 38 22 C40 16 36 13 30 15 Z" fill={colors[0]} />
      <path d="M45 12 C30 18 28 45 34 78 C38 85 46 86 50 80 C46 60 48 38 54 20 C56 14 51 10 45 12 Z" fill={colors[1]} />
      <path d="M60 12 C45 18 43 45 49 78 C53 85 61 86 65 80 C61 60 63 38 69 20 C71 14 66 10 60 12 Z" fill={colors[2]} />
    </svg>
  );
}
function SkinSunIllustration({ tone }) {
  return (
    <svg width="110" height="80" viewBox="0 0 120 90" fill="none">
      <circle cx="88" cy="22" r="14" fill="#f4c454" />
      <path d="M88 2v6M88 36v6M68 22h6M104 22h6M74 8l4 4M98 30l4 4M74 36l4-4M98 14l4-4" stroke="#f4c454" strokeWidth="2.5" strokeLinecap="round" />
      <circle cx="40" cy="55" r="30" fill={tone} />
    </svg>
  );
}
function JewelryIllustration({ metal }) {
  return (
    <svg width="110" height="80" viewBox="0 0 120 90" fill="none">
      <ellipse cx="60" cy="60" rx="34" ry="10" fill="#f0d5bc" opacity="0.5" />
      <circle cx="45" cy="45" r="16" fill="none" stroke={metal} strokeWidth="5" />
      <circle cx="75" cy="45" r="16" fill="none" stroke={metal} strokeWidth="5" />
      <circle cx="45" cy="45" r="4" fill={metal} />
      <circle cx="75" cy="45" r="4" fill={metal} />
    </svg>
  );
}
function ContrastIllustration({ high }) {
  return (
    <svg width="110" height="80" viewBox="0 0 120 90" fill="none">
      <circle cx="40" cy="35" r="20" fill="#f0d5bc" />
      <path d="M22 30 C22 15 58 15 58 30 C58 22 22 22 22 30Z" fill={high ? '#1a120c' : '#d9bd8e'} />
      <circle cx="88" cy="35" r="20" fill="#f0d5bc" />
      <path d="M70 30 C70 15 106 15 106 30 C106 22 70 22 70 30Z" fill={high ? '#e8d3ab' : '#c9a876'} />
      <rect x="10" y="62" width="45" height="14" rx="3" fill={high ? '#1a120c' : '#d9bd8e'} />
      <rect x="65" y="62" width="45" height="14" rx="3" fill={high ? '#fff' : '#e8d3ab'} stroke={high ? 'var(--line)' : 'none'} />
    </svg>
  );
}

const QUESTIONS = [
  {
    q: 'เส้นเลือดที่ข้อมือของคุณ ดูโดยรวมทั้ง 2 ข้างเห็นเป็นสีอะไรเด่นชัดมากที่สุด?',
    axisType: 'tone',
    options: [
      { label: 'สีเขียวเด่นชัดมากที่สุด', axis: 'warm', illustration: <WristIllustration veinColor="#6a9b5e" /> },
      { label: 'สีม่วงหรือน้ำเงินเด่นชัดมากที่สุด', axis: 'cool', illustration: <WristIllustration veinColor="#7a7ec9" /> },
    ],
  },
  {
    q: 'ผิวของคุณเป็นยังไงเวลาโดนแดด?',
    axisType: 'tone',
    options: [
      { label: 'แทนง่าย ผิวออกสีน้ำผึ้งอมทอง', axis: 'warm', illustration: <SkinSunIllustration tone="#d9a56a" /> },
      { label: 'ไหม้ง่าย ผิวแดงหรือซีดแบบเย็น', axis: 'cool', illustration: <SkinSunIllustration tone="#e8b8a8" /> },
    ],
  },
  {
    q: 'สีผมธรรมชาติของคุณโทนไหนใกล้เคียงที่สุด?',
    axisType: 'depth',
    options: [
      { label: 'โทนอ่อน สว่าง (บลอนด์ / น้ำตาลอ่อน)', axis: 'light', illustration: <HairIllustration colors={['#c9a876', '#d9bd8e', '#e8d3ab']} /> },
      { label: 'โทนเข้ม (น้ำตาลเข้ม / ดำ)', axis: 'deep', illustration: <HairIllustration colors={['#3a2a1e', '#2a1d14', '#1a120c']} /> },
    ],
  },
  {
    q: 'เครื่องประดับแบบไหนใส่แล้วหน้าดูสดใสกว่า?',
    axisType: 'tone',
    options: [
      { label: 'สีทอง', axis: 'warm', illustration: <JewelryIllustration metal="#c9a43f" /> },
      { label: 'สีเงิน', axis: 'cool', illustration: <JewelryIllustration metal="#a8adb5" /> },
    ],
  },
  {
    q: 'สีผมกับสีผิวของคุณ ตัดกันชัดเจน หรือกลมกลืนกัน?',
    axisType: 'depth',
    options: [
      { label: 'ตัดกันชัดเจน (คอนทราสต์สูง)', axis: 'deep', illustration: <ContrastIllustration high /> },
      { label: 'กลมกลืนกัน (คอนทราสต์ต่ำ)', axis: 'light', illustration: <ContrastIllustration high={false} /> },
    ],
  },
];

function computeSeasonFromAnswers(answers) {
  const toneAnswers = QUESTIONS.map((q, i) => (q.axisType === 'tone' ? answers[i] : null)).filter(Boolean);
  const depthAnswers = QUESTIONS.map((q, i) => (q.axisType === 'depth' ? answers[i] : null)).filter(Boolean);
  const warmCount = toneAnswers.filter((a) => a === 'warm').length;
  const tone = warmCount > toneAnswers.length / 2 ? 'warm' : 'cool';
  const lightCount = depthAnswers.filter((a) => a === 'light').length;
  const depth = lightCount > depthAnswers.length / 2 ? 'light' : 'deep';
  if (tone === 'warm' && depth === 'light') return 'Spring';
  if (tone === 'warm' && depth === 'deep') return 'Autumn';
  if (tone === 'cool' && depth === 'light') return 'Summer';
  return 'Winter';
}

const SEASON_CARDS = [
  { key: 'Spring', bg: 'linear-gradient(135deg,#fdf6df,#fbe9b8)', swatches: ['#e8d5ec', '#f7cfd8', '#d9ecc7', '#fdf1b8', '#fbdcc4', '#c9a0dc', '#f2a6bb', '#a8d18a', '#f4d35e', '#f4977a'] },
  { key: 'Summer', bg: 'linear-gradient(135deg,#eef3fa,#d6e3f2)', swatches: ['#d7dee6', '#a9c9e6', '#b7dcc4', '#f3e2b4', '#f2b8c6', '#b7c3d1', '#7fa8cf', '#7fbfa0', '#e2c765', '#e58fa8'] },
  { key: 'Autumn', bg: 'linear-gradient(135deg,#fbe8cf,#f3c98e)', swatches: ['#efe0c0', '#cfe0c0', '#e0d090', '#e0c090', '#eec9a8', '#b58a4a', '#6fa88f', '#c9a83f', '#b5793a', '#c98a6a'] },
  { key: 'Winter', bg: 'linear-gradient(135deg,#e9e3f4,#d2c6e8)', swatches: ['#ffffff', '#a9c9e6', '#a8d1a0', '#f0e04f', '#e6a8c9', '#d9d9d9', '#4f80c9', '#4fae4f', '#cfc94f', '#c94f8f'] },
];

export default function PersonalColorCheck() {
  const navigate = useNavigate();
  const [view, setView] = useState('landing'); // landing | quiz | result
  const [step, setStep] = useState(0);
  const [answers, setAnswers] = useState([]);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  function pickQuizAnswer(axis) {
    const next = [...answers, axis];
    setAnswers(next);
    if (step + 1 < QUESTIONS.length) {
      setStep(step + 1);
    } else {
      submitQuiz(next);
    }
  }

  async function submitQuiz(finalAnswers) {
    const season = computeSeasonFromAnswers(finalAnswers);
    setLoading(true);
    setError(null);
    try {
      const res = await personalColorApi.chooseManual(season);
      setResult(res);
      setView('result');
    } catch (e) {
      setError('บันทึกผลไม่สำเร็จ ลองใหม่อีกครั้ง');
    } finally {
      setLoading(false);
    }
  }

  function restart() {
    setView('landing');
    setStep(0);
    setAnswers([]);
    setResult(null);
    setError(null);
  }

  /* ===== RESULT VIEW ===== */
  if (view === 'result' && result) {
    return (
      <div className="app pcc-result-wrap" style={{ maxWidth: 640, margin: '0 auto', textAlign: 'center' }}>
        <div className="pcc-card" style={{
          background: 'linear-gradient(120deg,#fff,var(--parchment-deep))',
          border: '1px solid var(--line)', borderRadius: 22, padding: 32,
        }}>
          <div className="eyebrow">ผลลัพธ์ของคุณ</div>
          <h2 style={{ fontSize: 26, marginTop: 8 }}>โทนสีของคุณคือ {result.season}</h2>
          {result.reasoning && (
            <p style={{ fontSize: 13.5, color: 'var(--text-dim)', marginTop: 12, lineHeight: 1.7 }}>{result.reasoning}</p>
          )}
          <div className="pcc-palette">
            {result.palette.map((c) => (
              <div key={c.hex} title={c.name} className="pcc-palette-dot" style={{ background: c.hex }} />
            ))}
          </div>
          <div className="btn ghost" style={{ display: 'inline-flex', marginTop: 24 }} onClick={restart}>🔁 ทำใหม่</div>
        </div>
      </div>
    );
  }

  /* ===== QUIZ VIEW ===== */
  if (view === 'quiz') {
    const current = QUESTIONS[step];
    return (
      <div className="app" style={{ padding: '56px 24px 60px', maxWidth: 700, margin: '0 auto', textAlign: 'center' }}>
        <div className="eyebrow">Personal Color Check</div>
        <h1 style={{ fontSize: 26, marginTop: 12 }}>Q.{String(step + 1).padStart(2, '0')}</h1>
        <p style={{ color: 'var(--text-dim)', fontSize: 14, marginTop: 8, marginBottom: 28 }}>{current.q}</p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {current.options.map((opt, i) => (
            <div
              key={opt.label}
              onClick={() => !loading && pickQuizAnswer(opt.axis)}
              style={{
                display: 'flex', alignItems: 'center', gap: 20,
                border: '1.5px solid var(--line)', borderRadius: 14, padding: '18px 24px',
                cursor: loading ? 'default' : 'pointer', opacity: loading ? 0.5 : 1, textAlign: 'left',
              }}
            >
              <div style={{ fontFamily: "'Fraunces','Noto Serif Thai',serif", fontStyle: 'italic', fontWeight: 600, fontSize: 22, color: 'var(--ink)', width: 30, flex: 'none' }}>
                {i === 0 ? 'A' : 'B'}
              </div>
              <div style={{ flex: 1, fontSize: 14, fontWeight: 600 }}>{opt.label}</div>
              <div style={{ flex: 'none' }}>{opt.illustration}</div>
            </div>
          ))}
        </div>

        <p style={{ fontSize: 12.5, color: 'var(--text-dim)', marginTop: 20 }}>
          {step + 1 < QUESTIONS.length ? `เหลืออีก ${QUESTIONS.length - step - 1} คำถาม` : 'คำถามสุดท้าย'}
        </p>
        <div style={{ display: 'flex', gap: 6, justifyContent: 'center', marginTop: 8 }}>
          {QUESTIONS.map((_, i) => (
            <div key={i} style={{ width: 8, height: 8, borderRadius: '50%', background: i <= step ? 'var(--accent)' : 'var(--line)' }} />
          ))}
        </div>

        {step > 0 && !loading && (
          <div style={{ marginTop: 20, fontSize: 12.5, color: 'var(--text-dim)', cursor: 'pointer' }}
            onClick={() => { setStep(step - 1); setAnswers(answers.slice(0, -1)); }}>
            ‹ ย้อนกลับ
          </div>
        )}
        {step === 0 && (
          <div style={{ marginTop: 20, fontSize: 12.5, color: 'var(--text-dim)', cursor: 'pointer' }} onClick={() => setView('landing')}>
            ‹ กลับไปหน้าแรก
          </div>
        )}

        {loading && <p style={{ color: 'var(--text-dim)', fontSize: 13, marginTop: 16 }}>กำลังคำนวณผล...</p>}
        {error && <p style={{ color: '#b5652f', fontSize: 13, marginTop: 16 }}>{error}</p>}
      </div>
    );
  }

  /* ===== LANDING VIEW (4 season cards — decorative only, not clickable) ===== */
  return (
    <div className="app" style={{ padding: '56px 32px 0' }}>
      <div style={{ textAlign: 'center', maxWidth: 560, margin: '0 auto 40px' }}>
        <div className="eyebrow">Personal Color Check</div>
        <h1 style={{ fontSize: 30, marginTop: 12 }}>ค้นหาโทนสีที่ใช่สำหรับคุณ</h1>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, maxWidth: 960, margin: '0 auto 28px' }}>
        {SEASON_CARDS.map((s) => (
          <div
            key={s.key}
            style={{ borderRadius: 14, overflow: 'hidden', background: s.bg, minHeight: 140, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
              <div style={{ fontFamily: "'Fraunces','Noto Serif Thai',serif", fontStyle: 'italic', fontWeight: 500, fontSize: 22, color: 'var(--ink)' }}>{s.key}</div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5,1fr)', gap: 6 }}>
                {s.swatches.map((hex, i) => (
                  <span key={i} style={{ width: 16, height: 16, borderRadius: '50%', background: hex, boxShadow: 'inset 0 0 0 1px rgba(0,0,0,.08)' }} />
                ))}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', maxWidth: 960, margin: '0 auto 50px', border: '1px solid var(--line)' }}>
        <div
          onClick={() => { setView('quiz'); setStep(0); setAnswers([]); }}
          style={{ textAlign: 'center', padding: 20, fontSize: 13.5, fontWeight: 600, color: 'var(--ink)', cursor: 'pointer', borderRight: '1px solid var(--line)' }}
        >
          ทำแบบทดสอบ
        </div>
        <div
          onClick={() => navigate('/products')}
          style={{ textAlign: 'center', padding: 20, fontSize: 13.5, fontWeight: 600, color: 'var(--ink)', cursor: 'pointer' }}
        >
          ช้อปสินค้าตามโทนสี
        </div>
      </div>

      {error && <p style={{ color: '#b5652f', fontSize: 13, textAlign: 'center', marginBottom: 24 }}>{error}</p>}
    </div>
  );
}