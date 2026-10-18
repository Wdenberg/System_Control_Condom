import PassageReconciliation from '@/components/PassageReconciliation';
import { useEffect, useState } from 'react';
import { api } from '@/services/api';
import type { MovementResult } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import MovementDirectionBadge from '@/components/ui/MovementDirectionBadge';
import MovementDecisionBadge from '@/components/ui/MovementDecisionBadge';
import { messages } from '@/lib/presentation/messages';
import { labels } from '@/lib/presentation/labels';
import { formatDate } from '@/lib/presentation/common';

export default function MovementsPage() {
  const [items, setItems] = useState<MovementResult[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    api.movements.list(page, 30)
      .then((result) => { setItems(result.content); setTotalPages(result.totalPages); })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : messages.errors.loadMovements))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div className="page-content">
      <PageHeader title={labels.movements.title} description={labels.movements.description} />
      {error && <div className="banner warning" role="alert">{error}</div>}
      <section className="panel">
        {loading ? <LoadingState /> : items.length === 0 ? (
          <EmptyState title={labels.movements.emptyTitle} description={labels.movements.emptyDesc} />
        ) : (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>{labels.movements.thDate}</th>
                    <th>{labels.movements.thPlate}</th>
                    <th>{labels.movements.thDirection}</th>
                    <th>{labels.movements.thResident}</th>
                    <th>{labels.movements.thApartment}</th>
                    <th>{labels.movements.thSpace}</th>
                    <th>{labels.movements.thDecision}</th>
                    <th>Passagem física</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.movementId}>
                      <td>{formatDate(item.eventTime)}</td>
                      <td><strong className="plate-badge">{item.plate}</strong></td>
                      <td><MovementDirectionBadge direction={item.direction} /></td>
                      <td>{item.vehicleOwner || '—'}</td>
                      <td>{item.apartmentNumber || '—'}</td>
                      <td>{item.parkingSpaceNumber || '—'}</td>
                      <td><MovementDecisionBadge decision={item.decision} status={item.status} /></td>
                      <td><PassageReconciliation movement={item} onUpdate={(updated) => setItems((current) => current.map((row) => row.movementId === updated.movementId ? updated : row))} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="pager">
              <button className="secondary-button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>{labels.common.previous}</button>
              <span>{labels.common.page} {page + 1} {labels.common.of} {Math.max(totalPages, 1)}</span>
              <button className="secondary-button" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>{labels.common.next}</button>
            </div>
          </>
        )}
      </section>
    </div>
  );
}
