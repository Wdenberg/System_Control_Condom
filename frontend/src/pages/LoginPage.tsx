import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router';
import { useAuth } from '@/auth/auth-context';
import { messages } from '@/lib/presentation/messages';
import { labels } from '@/lib/presentation/labels';
import logo from '@/assets/favicon/web-app-manifest-512x512.png';

export default function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (isAuthenticated) return <Navigate to="/dashboard" replace />;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(username.trim(), password);
      const from = (location.state as { from?: string } | null)?.from || '/dashboard';
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? (err.message === 'UNAUTHORIZED' ? messages.auth.invalidCredentials : err.message) : messages.errors.login);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-visual">
        <div className="login-brand">
          <img src={logo} alt="Controle de Garagem" className="login-logo" />
          <span>{labels.login.brand}</span>
        </div>
        <div className="login-copy">
          <span className="eyebrow">{labels.login.eyebrow}</span>
          <h1>{labels.login.headline}</h1>
          <p>{labels.login.description}</p>
        </div>
        <div className="login-status"><span /> {labels.login.status}</div>
      </section>

      <section className="login-panel">
        <div className="login-card">
          <div className="login-heading">
            <img src={logo} alt="Controle de Garagem" className="login-logo-small" />
            <span className="eyebrow">{labels.login.accessTitle}</span>
            <h2>{labels.login.accessHeadline}</h2>
            <p>{labels.login.accessDescription}</p>
          </div>
          <form onSubmit={submit} className="form-stack">
            <label>
              {labels.login.usernameLabel}
              <input autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} placeholder={labels.login.usernamePlaceholder} required />
            </label>
            <label>
              {labels.login.passwordLabel}
              <input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder={labels.login.passwordPlaceholder} required />
            </label>
            {error && <div className="form-error" role="alert">{error}</div>}
            <button className="primary-button full" disabled={loading}>{loading ? labels.login.submitLoading : labels.login.submit}</button>
          </form>
          <small className="login-note">{labels.login.sessionNote}</small>
        </div>
      </section>
    </main>
  );
}
