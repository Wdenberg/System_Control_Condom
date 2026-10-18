export default function StatCard({ label, value, hint, tone = 'neutral' }: { label: string; value: number | string; hint?: string; tone?: 'neutral' | 'success' | 'warning' | 'danger' }) {
  return (
    <article className={`stat-card ${tone}`}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </article>
  );
}
