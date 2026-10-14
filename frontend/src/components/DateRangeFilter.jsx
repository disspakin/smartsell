import { useEffect, useState } from 'react';

/**
 * ตัวเลือกช่วงเวลาของ Dashboard: ทั้งหมด / รายเดือน / รายปี / กำหนดเอง
 *
 * ทุกโหมดแปลงเป็น from/to (YYYY-MM-DD, รวมทั้งสองวัน) ก่อนส่ง backend — backend มีกฎเดียวคือช่วงวันที่
 * ค่าอยู่ใน URL (?mode=&from=&to=) refresh แล้วไม่หลุด และหน้า Product Analytics ใช้ช่วงเดียวกันต่อได้
 * ปีแสดงเป็น พ.ศ. แต่ค่าที่ส่งเป็น ค.ศ.
 */

const MONTHS = ['มกราคม', 'กุมภาพันธ์', 'มีนาคม', 'เมษายน', 'พฤษภาคม', 'มิถุนายน',
  'กรกฎาคม', 'สิงหาคม', 'กันยายน', 'ตุลาคม', 'พฤศจิกายน', 'ธันวาคม'];
const MONTHS_SHORT = ['ม.ค.', 'ก.พ.', 'มี.ค.', 'เม.ย.', 'พ.ค.', 'มิ.ย.',
  'ก.ค.', 'ส.ค.', 'ก.ย.', 'ต.ค.', 'พ.ย.', 'ธ.ค.'];
const MODES = [
  { key: 'all', label: 'ทั้งหมด' },
  { key: 'month', label: 'รายเดือน' },
  { key: 'year', label: 'รายปี' },
  { key: 'custom', label: 'กำหนดเอง' },
];
const BE_OFFSET = 543;

const pad = (n) => String(n).padStart(2, '0');
const isoDate = (y, m, d) => `${y}-${pad(m)}-${pad(d)}`;
const lastDayOf = (y, m) => new Date(y, m, 0).getDate(); // m = 1–12

function todayIso() {
  const t = new Date();
  return isoDate(t.getFullYear(), t.getMonth() + 1, t.getDate());
}

export function monthRange(y, m) {
  return { from: isoDate(y, m, 1), to: isoDate(y, m, lastDayOf(y, m)) };
}

export function yearRange(y) {
  return { from: isoDate(y, 1, 1), to: isoDate(y, 12, 31) };
}

/** "2026-10-01" → "1 ต.ค. 2569" */
function thaiDate(iso) {
  const [y, m, d] = iso.split('-').map(Number);
  return `${d} ${MONTHS_SHORT[m - 1]} ${y + BE_OFFSET}`;
}

/** อ่านช่วงเวลาจาก URL — ค่าไม่ครบหรือผิดรูปแบบถือเป็น "ทั้งหมด" */
export function periodFromSearchParams(params) {
  const mode = params.get('mode');
  const from = params.get('from');
  const to = params.get('to');
  const valid = /^\d{4}-\d{2}-\d{2}$/;
  if (!['month', 'year', 'custom'].includes(mode) || !valid.test(from || '') || !valid.test(to || '')) {
    return { mode: 'all', from: null, to: null };
  }
  return { mode, from, to };
}

/** ช่วงเวลา → query string สำหรับ URL ของหน้าเว็บ ("" เมื่อเป็นทั้งหมด) */
export function periodToSearch(period) {
  if (period.mode === 'all') return '';
  return `?mode=${period.mode}&from=${period.from}&to=${period.to}`;
}

const pillStyle = (active) => ({
  padding: '7px 14px', fontSize: '13px', fontWeight: 600, cursor: 'pointer', borderRadius: '8px',
  border: active ? '1px solid #0f172a' : '1px solid #e2e8f0',
  backgroundColor: active ? '#0f172a' : '#fff',
  color: active ? '#fff' : '#475569',
});
const selectStyle = {
  padding: '7px 10px', fontSize: '13px', borderRadius: '8px', border: '1px solid #e2e8f0',
  backgroundColor: '#fff', color: '#1f2937', cursor: 'pointer',
};
const arrowStyle = { ...selectStyle, padding: '7px 11px', fontWeight: 700 };

/**
 * @param period    { mode, from, to } ปัจจุบัน (จาก periodFromSearchParams)
 * @param onChange  เรียกเมื่อเลือกช่วงใหม่ที่ถูกต้องแล้วเท่านั้น
 * @param firstYear ปีแรกที่มีข้อมูล (ค.ศ.) — dropdown ปีเริ่มจากปีนี้ถึงปีปัจจุบัน
 */
export default function DateRangeFilter({ period, onChange, firstYear }) {
  const now = new Date();
  const thisYear = now.getFullYear();
  const startYear = Math.min(firstYear || thisYear, thisYear);
  const years = [];
  for (let y = thisYear; y >= startYear; y--) years.push(y);

  // ปี/เดือนที่กำลังเลือก — อ่านจาก from ของช่วงปัจจุบัน ถ้าไม่มีใช้เดือนนี้
  const [selYear, selMonth] = period.from
    ? period.from.split('-').map(Number)
    : [thisYear, now.getMonth() + 1];

  // โหมดกำหนดเอง: เก็บค่าที่กำลังพิมพ์ไว้ก่อน ส่งออกเมื่อครบและถูกต้อง
  const [draft, setDraft] = useState({ from: period.from || todayIso(), to: period.to || todayIso() });
  useEffect(() => {
    if (period.mode === 'custom') setDraft({ from: period.from, to: period.to });
  }, [period.mode, period.from, period.to]);
  const draftInvalid = draft.from && draft.to && draft.from > draft.to;

  function selectMode(mode) {
    if (mode === period.mode) return;
    if (mode === 'all') onChange({ mode: 'all', from: null, to: null });
    if (mode === 'month') onChange({ mode, ...monthRange(thisYear, now.getMonth() + 1) });
    if (mode === 'year') onChange({ mode, ...yearRange(thisYear) });
    if (mode === 'custom') {
      // เริ่มจากช่วงที่ดูอยู่ ถ้าดูทั้งหมดอยู่ เริ่มที่ต้นเดือนนี้ถึงวันนี้
      const start = period.from ? { from: period.from, to: period.to }
        : { from: isoDate(thisYear, now.getMonth() + 1, 1), to: todayIso() };
      onChange({ mode, ...start });
    }
  }

  function shiftMonth(delta) {
    const d = new Date(selYear, selMonth - 1 + delta, 1);
    onChange({ mode: 'month', ...monthRange(d.getFullYear(), d.getMonth() + 1) });
  }

  function updateDraft(key, value) {
    const next = { ...draft, [key]: value };
    setDraft(next);
    if (next.from && next.to && next.from <= next.to) onChange({ mode: 'custom', ...next });
  }

  const canGoBack = (y) => y > startYear;
  const canGoForward = (y, m) => (m === undefined ? y < thisYear : y < thisYear || m < now.getMonth() + 1);

  return (
    <div style={{
      backgroundColor: '#fff', border: '1px solid #e2e8f0', borderRadius: '16px',
      padding: '16px 20px', marginBottom: '24px',
    }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '12px', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '13px', fontWeight: 700, color: '#0f172a', marginRight: '4px' }}>ช่วงเวลา:</span>
          {MODES.map((m) => (
            <button key={m.key} type="button" style={pillStyle(period.mode === m.key)} onClick={() => selectMode(m.key)}>
              {m.label}
            </button>
          ))}
        </div>

        {period.mode === 'month' && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <button type="button" style={arrowStyle} aria-label="เดือนก่อนหน้า"
              disabled={!canGoBack(selYear) && selMonth === 1} onClick={() => shiftMonth(-1)}>‹</button>
            <select style={selectStyle} value={selMonth} aria-label="เดือน"
              onChange={(e) => onChange({ mode: 'month', ...monthRange(selYear, Number(e.target.value)) })}>
              {MONTHS.map((name, i) => <option key={name} value={i + 1}>{name}</option>)}
            </select>
            <select style={selectStyle} value={selYear} aria-label="ปี"
              onChange={(e) => onChange({ mode: 'month', ...monthRange(Number(e.target.value), selMonth) })}>
              {years.map((y) => <option key={y} value={y}>{y + BE_OFFSET}</option>)}
            </select>
            <button type="button" style={arrowStyle} aria-label="เดือนถัดไป"
              disabled={!canGoForward(selYear, selMonth)} onClick={() => shiftMonth(1)}>›</button>
          </div>
        )}

        {period.mode === 'year' && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <button type="button" style={arrowStyle} aria-label="ปีก่อนหน้า"
              disabled={!canGoBack(selYear)} onClick={() => onChange({ mode: 'year', ...yearRange(selYear - 1) })}>‹</button>
            <select style={selectStyle} value={selYear} aria-label="ปี"
              onChange={(e) => onChange({ mode: 'year', ...yearRange(Number(e.target.value)) })}>
              {years.map((y) => <option key={y} value={y}>{y + BE_OFFSET}</option>)}
            </select>
            <button type="button" style={arrowStyle} aria-label="ปีถัดไป"
              disabled={!canGoForward(selYear)} onClick={() => onChange({ mode: 'year', ...yearRange(selYear + 1) })}>›</button>
          </div>
        )}

        {period.mode === 'custom' && (
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '8px', fontSize: '13px', color: '#475569' }}>
            ตั้งแต่
            <input type="date" style={selectStyle} value={draft.from} max={draft.to || undefined}
              onChange={(e) => updateDraft('from', e.target.value)} />
            ถึง
            <input type="date" style={selectStyle} value={draft.to} min={draft.from || undefined}
              onChange={(e) => updateDraft('to', e.target.value)} />
          </div>
        )}
      </div>

      <div style={{ marginTop: '10px', fontSize: '12.5px', color: draftInvalid && period.mode === 'custom' ? '#b91c1c' : '#64748b' }}>
        {period.mode === 'custom' && draftInvalid
          ? '⚠️ วันเริ่มต้นต้องไม่อยู่หลังวันสิ้นสุด'
          : period.mode === 'all'
            ? 'แสดงข้อมูลทั้งหมด'
            : `แสดงข้อมูลวันที่ ${thaiDate(period.from)} – ${thaiDate(period.to)}`}
      </div>
    </div>
  );
}
