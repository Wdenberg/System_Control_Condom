import { useCallback, useEffect, useState } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import { env } from '@/config/env';
import type { ObservabilitySummary } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import { Heart, Activity, BarChart3, FileText, Hash, CheckCircle } from 'lucide-react';
import { labels } from '@/lib/presentation/labels';
import { messages } from '@/lib/presentation/messages';

export default function ObservabilityPage() {
  const [summary, setSummary] = useState<ObservabilitySummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      setSummary(await api.system.observability());
      setError('');
    } catch (err) {
      setError(getApiErrorMessage(err, messages.errors.loadObservability));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    const id = window.setInterval(() => void load(), 15000);
    return () => window.clearInterval(id);
  }, [load]);

  return <div className="page-content">
    <PageHeader
      title={labels.observability.title}
      description={labels.observability.description}
      action={<button className="secondary-button" onClick={() => void load()}>{labels.common.update}</button>}
    />

    {error && <div className="banner warning" role="alert">{error}</div>}
    {loading && !summary ? <LoadingState label={labels.observability.loading} /> : <>
      <section className="stat-grid">
        <div className={`stat-card ${summary?.status === 'UP' ? 'success' : 'warning'}`}>
          <span className="stat-label">{labels.observability.serverTitle}</span>
          <div className="stat-value">{summary?.status === 'NOT_CHECKED' ? 'Não verificado' : summary?.status ?? '—'}</div>
          <div className="stat-hint">{summary?.application ?? 'garage-control-backend'}</div>
        </div>
        <div className="stat-card">
          <span className="stat-label">{labels.observability.sseClients}</span>
          <div className="stat-value">{summary?.realtimeConnectedClients ?? 0}</div>
          <div className="stat-hint">{labels.observability.sseHint}</div>
        </div>
        <div className="stat-card">
          <span className="stat-label">{labels.observability.metricsTitle}</span>
          <div className="stat-value"><CheckCircle className="h-6 w-6 text-[var(--color-success)]" /></div>
          <div className="stat-hint">{labels.observability.metricsHint}</div>
        </div>
        <div className="stat-card">
          <span className="stat-label">{labels.observability.healthTitle}</span>
          <div className="stat-value"><CheckCircle className="h-6 w-6 text-[var(--color-success)]" /></div>
          <div className="stat-hint">{labels.observability.healthHint}</div>
        </div>
      </section>

      <section className="dashboard-grid">
        <article className="panel">
          <div className="panel-header"><div><h3>{labels.observability.endpointsTitle}</h3><span>{labels.observability.endpointsHint}</span></div></div>
          <div className="camera-list">
            <div className="camera-row"><div className="camera-icon online"><Heart className="h-4 w-4" /></div><div className="camera-info"><strong>Saúde</strong><span>Disponibilidade da aplicação e dependências.</span></div><span>Consulta restrita à administração</span></div>
            <div className="camera-row"><div className="camera-icon online"><Activity className="h-4 w-4" /></div><div className="camera-info"><strong>Prometheus</strong><span>Métricas em formato de coleta.</span></div>{env.prometheusUrl && <a className="secondary-button" href={env.prometheusUrl} target="_blank" rel="noreferrer">{labels.observability.healthButton}</a>}</div>
            <div className="camera-row"><div className="camera-icon online"><BarChart3 className="h-4 w-4" /></div><div className="camera-info"><strong>Grafana</strong><span>Painéis de observabilidade.</span></div>{env.grafanaUrl && <a className="secondary-button" href={env.grafanaUrl} target="_blank" rel="noreferrer">{labels.observability.healthButton}</a>}</div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-header"><div><h3>{labels.observability.modelTitle}</h3><span>{labels.observability.modelHint}</span></div></div>
          <div className="alert-list">
            <div className="alert-row"><div className="camera-icon online"><FileText className="h-4 w-4" /></div><div className="camera-info"><strong>{labels.observability.logsLabel}</strong><span>{labels.observability.logsHint}</span></div></div>
            <div className="alert-row"><div className="camera-icon online"><Activity className="h-4 w-4" /></div><div className="camera-info"><strong>{labels.observability.metricsLabel}</strong><span>{labels.observability.metricsHintModel}</span></div></div>
            <div className="alert-row"><div className="camera-icon online"><Hash className="h-4 w-4" /></div><div className="camera-info"><strong>{labels.observability.tracingLabel}</strong><span>{labels.observability.tracingHint}</span></div></div>
          </div>
        </article>
      </section>
    </>}
  </div>;
}
