import { Link } from 'react-router-dom';

/**
 * การ์ดเครื่องมือในหน้าแรก (Personal Color / Size Chart / Body Analysis)
 * children = ตัวอย่าง UI ของเครื่องมือนั้น ๆ
 */
export default function ToolCard({ number, icon, title, description, linkText, to, children }) {
  return (
    <div className="tool-card">
      <div className="tool-card-head">
        <div className="tool-card-icon">{icon}</div>
        <span className="tool-card-num">{number}</span>
      </div>
      <h3>{title}</h3>
      <p className="tool-card-desc">{description}</p>
      <div className="tool-card-preview">{children}</div>
      <Link to={to} className="tool-card-link">{linkText} →</Link>
    </div>
  );
}
