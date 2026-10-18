import { Component, type ErrorInfo, type ReactNode } from 'react';
import { labels } from '@/lib/presentation/labels';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export default class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('[ErrorBoundary]', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;
      return (
        <div className="page-content" style={{ padding: '2rem', textAlign: 'center' }}>
          <div className="panel error-panel" style={{ maxWidth: 600, margin: '2rem auto' }}>
            <h2 style={{ marginBottom: '0.5rem' }}>{labels.errorBoundary.title}</h2>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>
              {labels.errorBoundary.description}
            </p>
            <details style={{ marginBottom: '1rem', textAlign: 'left' }}>
              <summary style={{ cursor: 'pointer', color: 'var(--text-secondary)' }}>{labels.errorBoundary.detailsLabel}</summary>
              <pre style={{ marginTop: '0.5rem', padding: '0.75rem', background: 'var(--surface-secondary)', borderRadius: 6, fontSize: '0.8rem', overflow: 'auto' }}>
                {this.state.error?.message}
              </pre>
            </details>
            <button className="primary-button" onClick={() => window.location.reload()}>
              {labels.errorBoundary.reloadButton}
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
