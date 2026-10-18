import StatCard from '@/components/StatCard';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import PageHeader from '@/components/PageHeader';
import { useOperationalDashboard } from '@/hooks/useOperationalDashboard';
import { Camera, AlertTriangle } from 'lucide-react';
import MovementDirectionBadge from '@/components/ui/MovementDirectionBadge';
import { messages } from '@/lib/presentation/messages';
import MovementDecisionBadge from '@/components/ui/MovementDecisionBadge';
import { formatDate } from '@/lib/presentation/common';
import { labels } from '@/lib/presentation/labels';

export default function DashboardPage() {
  const { summary, activity, occupancy, cameras, alerts, loading, refreshing, error, load, connected } = useOperationalDashboard();

  if (loading) return <LoadingState label={labels.dashboard.loading} />;
  if (!summary) return <div className="panel error-panel">{error || messages.dashboard.noData}</div>;

  return (
    <div className="page-content">
      <PageHeader
        title={labels.dashboard.title}
        description={`${labels.dashboard.lastUpdate}: ${formatDate(summary.generatedAt)} · ${connected ? labels.dashboard.realtimeActive : labels.dashboard.realtimeUnavailable}`}
        action={<button className="secondary-button" onClick={() => void load(false)} disabled={refreshing}>{refreshing ? labels.common.refreshing : labels.common.update}</button>}
      />

      {error && <div className="banner warning" role="alert">{error}</div>}

      <section className="stat-grid">
        <StatCard label={labels.dashboard.statFreeSpaces} value={summary.freeParkingSpaces} hint={`${summary.totalParkingSpaces} ${labels.dashboard.statTotalHint}`} tone="success" />
        <StatCard label={labels.dashboard.statOccupiedSpaces} value={summary.occupiedParkingSpaces} hint={`${summary.blockedParkingSpaces} ${labels.dashboard.statBlockedHint}`} tone="neutral" />
        <StatCard label={labels.dashboard.statVehiclesInside} value={summary.vehiclesInside} hint={`${summary.vehiclesOutside} ${labels.dashboard.statOutsideHint}`} tone="neutral" />
        <StatCard label={labels.dashboard.statMovements} value={summary.movementsToday} hint={`${summary.entriesToday} ${labels.dashboard.statEntriesHint} · ${summary.exitsToday} ${labels.dashboard.statExitsHint}`} tone="neutral" />
        <StatCard label={labels.dashboard.statAuthorized} value={summary.authorizedToday} hint={`${summary.deniedToday} ${labels.dashboard.statDeniedHint}`} tone="success" />
        <StatCard label={labels.dashboard.statPendingGate} value={summary.pendingGateOperations} hint={`${summary.exhaustedGateOperations} ${labels.dashboard.statExhaustedHint}`} tone={summary.pendingGateOperations > 0 || summary.exhaustedGateOperations > 0 ? 'warning' : 'neutral'} />
      </section>

      <div className="dashboard-grid">
        <section className="panel wide-panel">
          <div className="panel-header"><div><h3>{labels.dashboard.recentMovements}</h3><span>{labels.dashboard.recentMovementsHint}</span></div></div>
          {activity.length === 0 ? <EmptyState title={labels.dashboard.noMovements} description="O sistema ainda não registrou eventos recentes." /> : (
            <div className="table-wrap"><table><thead><tr><th>Horário</th><th>Placa</th><th>Direção</th><th>Morador</th><th>Vaga</th><th>Decisão</th></tr></thead>
              <tbody>{activity.map(({ movement }) => (
                <tr key={movement.movementId}>
                  <td>{formatDate(movement.eventTime)}</td>
                  <td><strong className="plate-badge">{movement.plate}</strong></td>
                  <td><MovementDirectionBadge direction={movement.direction} /></td>
                  <td>{movement.vehicleOwner || '—'}</td>
                  <td>{movement.parkingSpaceNumber || '—'}</td>
                  <td><MovementDecisionBadge decision={movement.decision} status={movement.status} /></td>
                </tr>
              ))}</tbody>
            </table></div>
          )}
        </section>

        <section className="panel">
          <div className="panel-header"><div><h3>{labels.dashboard.camerasTitle}</h3><span>{labels.dashboard.camerasHint}</span></div></div>
          <div className="camera-list">
            {cameras.length === 0 ? <EmptyState title={labels.dashboard.noCameras} description={labels.dashboard.noCamerasDesc} /> : cameras.map((camera) => (
              <div className="camera-row" key={camera.id}>
                <div className={`camera-icon ${camera.enabled ? 'online' : 'offline'}`}><Camera className="h-4 w-4" /></div>
                <div className="camera-info"><strong>{camera.name}</strong><span>{camera.ipAddress} · {camera.direction === 'ENTRY' ? labels.portaria.entry : labels.portaria.exit}</span><small>{labels.dashboard.lastPlate}: {camera.lastPlate || '—'} · {formatDate(camera.lastEventAt)}</small></div>
                <span className={camera.enabled ? 'online-label' : 'offline-label'}>{camera.enabled ? labels.common.connected : labels.common.disconnected}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="panel">
          <div className="panel-header"><div><h3>{labels.dashboard.occupancyTitle}</h3><span>{labels.dashboard.occupancyHint}</span></div></div>
          <div className="occupancy-summary">
            <div className="occupancy-track"><span style={{ width: `${summary.totalParkingSpaces ? (summary.occupiedParkingSpaces / summary.totalParkingSpaces) * 100 : 0}%` }} /></div>
            <div className="occupancy-meta"><strong>{summary.occupiedParkingSpaces} {labels.dashboard.occupied}</strong><span>{summary.freeParkingSpaces} {labels.dashboard.free}</span></div>
          </div>
          <div className="space-grid">
            {occupancy.slice(0, 24).map((space) => <div key={space.parkingSpaceId} title={`${space.parkingSpaceNumber} · ${space.apartmentNumber || labels.common.commonSpace}`} className={`space-box ${space.status.toLowerCase()}`}><span>{space.parkingSpaceNumber}</span></div>)}
          </div>
        </section>

        <section className="panel">
          <div className="panel-header"><div><h3>{labels.dashboard.alertsTitle}</h3><span>{labels.dashboard.alertsHint}</span></div></div>
          {alerts.length === 0 ? <EmptyState title={labels.dashboard.normalOperation} description={labels.dashboard.noAlerts} /> : <div className="alert-list">{alerts.map((alert) => (
            <article className={`alert-row ${alert.severity.toLowerCase()}`} key={alert.id}><div className="alert-marker"><AlertTriangle className="h-3.5 w-3.5" /></div><div><strong>{alert.title}</strong><p>{alert.message}</p><small>{formatDate(alert.occurredAt)}</small></div></article>
          ))}</div>}
        </section>
      </div>
    </div>
  );
}
