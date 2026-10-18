import { useCallback, useEffect, useMemo, useState } from 'react';
import type { AuditLogRow } from '@/types/api';
import { api, getApiErrorMessage } from '@/services/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import type { PageResponse } from '@/types/api';
import { messages } from '@/lib/presentation/messages';
import { labels } from '@/lib/presentation/labels';
import { formatDate } from '@/lib/presentation/common';
import { validateDateRange } from '@/lib/validation';

function offsetToIso(value: string) {
  return value ? `${value}:00-03:00` : undefined;
}

function displayDate(value: string) {
  return formatDate(value);
}

export default function AuditPage() {
  const [rows, setRows] = useState<AuditLogRow[]>([]);
  const [pageInfo, setPageInfo] = useState<PageResponse<AuditLogRow> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filters, setFilters] = useState({ from: '', to: '', username: '', action: '', entityType: '', entityId: '', ipAddress: '', correlationId: '' });

  const params = useMemo(() => ({
    from: offsetToIso(filters.from),
    to: offsetToIso(filters.to),
    username: filters.username || undefined,
    action: filters.action || undefined,
    entityType: filters.entityType || undefined,
    entityId: filters.entityId || undefined,
    ipAddress: filters.ipAddress || undefined,
    correlationId: filters.correlationId || undefined,
  }), [filters]);

  const load = useCallback(async (nextPage = 0) => {
    const dateErr = validateDateRange(filters.from, filters.to);
    if (dateErr) { setError(dateErr); return; }
    setLoading(true);
    setError('');
    try {
      const result = await api.audit.list(params, nextPage, 30);
      setPageInfo(result);
      setRows(result.content);
      setCurrentPage(nextPage);
    } catch (e) {
      setError(getApiErrorMessage(e, messages.errors.loadAudit));
    } finally {
      setLoading(false);
    }
  }, [params]);

  useEffect(() => { void load(0); }, [load]);

  async function exportCsv() {
    try {
      const blob = await api.audit.csv(params);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = labels.audit.csvFilename;
      link.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(getApiErrorMessage(e, messages.errors.exportAudit));
    }
  }

  return (
    <div className="page-content">
      <PageHeader eyebrow={labels.audit.eyebrow} title={labels.audit.title} description={labels.audit.description} />

      <section className="panel audit-filter-panel">
        <div className="audit-filter-grid">
          <label><span>{labels.audit.fromLabel}</span><input type="datetime-local" value={filters.from} onChange={e => setFilters({ ...filters, from: e.target.value })} /></label>
          <label><span>{labels.audit.toLabel}</span><input type="datetime-local" value={filters.to} onChange={e => setFilters({ ...filters, to: e.target.value })} /></label>
          <label><span>{labels.audit.userLabel}</span><input placeholder={labels.audit.userPlaceholder} value={filters.username} onChange={e => setFilters({ ...filters, username: e.target.value })} /></label>
          <label><span>{labels.audit.actionLabel}</span><input placeholder={labels.audit.actionPlaceholder} value={filters.action} onChange={e => setFilters({ ...filters, action: e.target.value })} /></label>
          <label><span>{labels.audit.entityLabel}</span><input placeholder={labels.audit.entityPlaceholder} value={filters.entityType} onChange={e => setFilters({ ...filters, entityType: e.target.value })} /></label>
          <label><span>{labels.audit.entityIdLabel}</span><input placeholder={labels.audit.entityIdPlaceholder} value={filters.entityId} onChange={e => setFilters({ ...filters, entityId: e.target.value })} /></label>
          <label><span>{labels.audit.ipLabel}</span><input placeholder="192.168..." value={filters.ipAddress} onChange={e => setFilters({ ...filters, ipAddress: e.target.value })} /></label>
          <label><span>{labels.audit.correlationLabel}</span><input placeholder="UUID" value={filters.correlationId} onChange={e => setFilters({ ...filters, correlationId: e.target.value })} /></label>
        </div>
        <div className="audit-toolbar">
          <span>{pageInfo?.totalElements ?? 0} {labels.common.recordsFound}</span>
          <div>
            <button className="secondary-button" onClick={() => { setFilters({ from: '', to: '', username: '', action: '', entityType: '', entityId: '', ipAddress: '', correlationId: '' }); }}>{labels.audit.clearButton}</button>
            <button className="secondary-button" onClick={() => void exportCsv()}>{labels.common.exportCsv}</button>
            <button className="primary-button" onClick={() => void load(0)}>{labels.common.update}</button>
          </div>
        </div>
      </section>

      {error && <div className="banner warning" role="alert">{error}</div>}

      {loading ? <LoadingState label={labels.audit.loading} /> : rows.length === 0 ? (
        <EmptyState title={labels.audit.emptyTitle} description={labels.audit.emptyDesc} />
      ) : (
        <section className="panel table-wrap">
          <table>
            <thead><tr><th>{labels.audit.thDate}</th><th>{labels.audit.thUser}</th><th>{labels.audit.thAction}</th><th>{labels.audit.thEntity}</th><th>{labels.audit.thOrigin}</th><th>{labels.audit.thContext}</th><th>{labels.audit.thDetails}</th></tr></thead>
            <tbody>
              {rows.map(row => (
                <tr key={row.id}>
                  <td><strong>{displayDate(row.createdAt)}</strong><small>{row.correlationId ?? labels.common.noCorrelationId}</small></td>
                  <td>{row.username ?? labels.common.system}<small>{row.ipAddress ?? labels.common.ipUnavailable}</small></td>
                  <td><span className={`audit-outcome ${row.outcome === 'SUCCESS' ? 'success' : 'failure'}`}>{row.outcome === 'SUCCESS' ? labels.common.success : labels.common.failure}</span><small>{row.action}</small></td>
                  <td>{row.entityType}<small>{row.entityId ?? '—'}</small></td>
                  <td>{row.requestMethod ?? '—'}<small>{row.requestPath ?? '—'}</small></td>
                  <td><small>{row.userAgent ?? labels.common.userAgentUnavailable}</small></td>
                  <td>{row.justification || row.details || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {pageInfo && pageInfo.totalPages > 1 && <div className="pagination">
            <button className="secondary-button" disabled={pageInfo.first} onClick={() => void load(currentPage - 1)}>{labels.common.previous}</button>
            <span>{labels.common.page} {currentPage + 1} {labels.common.of} {pageInfo.totalPages}</span>
            <button className="secondary-button" disabled={pageInfo.last} onClick={() => void load(currentPage + 1)}>{labels.common.next}</button>
          </div>}
        </section>
      )}
    </div>
  );
}
