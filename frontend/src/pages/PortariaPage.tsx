import { useMemo, useState, type FormEvent } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import { useOperationalDashboard } from '@/hooks/useOperationalDashboard';
import { useRealtime } from '@/realtime/realtime-context';
import { useAuth } from '@/auth/auth-context';
import type { MovementResult } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import EmptyState from '@/components/EmptyState';
import LoadingState from '@/components/LoadingState';
import StatCard from '@/components/StatCard';
import { Camera, AlertTriangle } from 'lucide-react';
import MovementDirectionBadge from '@/components/ui/MovementDirectionBadge';
import MovementDecisionBadge from '@/components/ui/MovementDecisionBadge';
import { getMovementDirectionLabel } from '@/lib/presentation/movement';
import { messages } from '@/lib/presentation/messages';
import { labels } from '@/lib/presentation/labels';
import { formatDate } from '@/lib/presentation/common';
import { validatePlate, validateJustification, LIMITS } from '@/lib/validation';
import type { FieldErrors } from '@/lib/validation';
import MercosulPlate from '@/components/MercosulPlate';

function movementFromEvent(event: { type: string; data: unknown }): MovementResult | null {
  if (event.type !== 'movement.created' || !event.data || typeof event.data !== 'object') return null;
  const candidate = event.data as Partial<MovementResult>;
  return typeof candidate.movementId === 'number' && typeof candidate.plate === 'string'
    ? candidate as MovementResult
    : null;
}

export default function PortariaPage() {
  const { hasPermission } = useAuth();
  const { connected, events } = useRealtime();
  const { summary, activity, occupancy, cameras, alerts, loading, error, setError, gateStatus, setGateStatus, load } = useOperationalDashboard(true);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [manualPlate, setManualPlate] = useState('');
  const [manualDirection, setManualDirection] = useState<'ENTRY' | 'EXIT'>('ENTRY');
  const [justification, setJustification] = useState('');
  const [gateJustification, setGateJustification] = useState('');
  const [gateDirection, setGateDirection] = useState<'ENTRY' | 'EXIT'>('ENTRY');
  const [gateTesting, setGateTesting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  const submitGateTest = async (event: FormEvent) => {
    event.preventDefault();
    if (!gateStatus?.enabled) {
      setFeedback(messages.gate.disabled as string);
      return;
    }
    const errors: FieldErrors = {};
    const justErr = validateJustification(gateJustification, LIMITS.justificationGate.min, LIMITS.justificationGate.max);
    if (justErr) errors.gateJustification = justErr;
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;
    setGateTesting(true);
    try {
      const next = await api.gates.test(gateDirection, gateJustification.trim());
      setGateStatus(next);
      setFeedback((messages.gate.testSent as (dir: string) => string)(gateDirection));
      setGateJustification('');
      setFieldErrors({});
    } catch (err) {
      setFeedback(getApiErrorMessage(err));
    } finally {
      setGateTesting(false);
    }
  };

  const liveMovements = useMemo(() => {
    const byId = new Map<number, MovementResult>();
    for (const event of events) {
      const movement = movementFromEvent(event);
      if (movement) byId.set(movement.movementId, movement);
    }
    for (const item of activity) byId.set(item.movement.movementId, item.movement);
    return [...byId.values()].sort((a, b) => new Date(b.eventTime).getTime() - new Date(a.eventTime).getTime()).slice(0, 20);
  }, [activity, events]);

  const latestMovement = liveMovements[0] ?? null;

  async function submitManualMovement(event: FormEvent) {
    event.preventDefault();
    setFeedback('');
    setError('');
    const errors: FieldErrors = {};
    const plateErr = validatePlate(manualPlate);
    if (plateErr) errors.manualPlate = plateErr;
    const justErr = validateJustification(justification, LIMITS.justificationManual.min, LIMITS.justificationManual.max);
    if (justErr) errors.justification = justErr;
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;
    setSubmitting(true);
    try {
      const result = await api.movements.createManual({
        plate: manualPlate.replace(/[^A-Za-z0-9]/g, '').toUpperCase(),
        direction: manualDirection,
        justification: justification.trim(),
      });
      setFeedback((messages.movement.registered as (id: number, dir: string) => string)(result.movementId, getMovementDirectionLabel(result.direction)));
      setManualPlate('');
      setJustification('');
      setFieldErrors({});
      void load(false);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading || !summary) return <LoadingState label={labels.portaria.loading} />;

  return (
    <div className="page-content">
      <PageHeader
        title={labels.portaria.title}
        description={labels.portaria.description}
        action={<span className={`realtime-badge ${connected ? 'connected' : 'disconnected'}`}><span />{connected ? labels.portaria.realtimeActive : labels.portaria.reconnecting}</span>}
      />

      {error && <div className="banner warning" role="alert">{error}</div>}
      {feedback && <div className="banner success" role="status">{feedback}</div>}

      <section className="portaria-hero">
        <div className="panel last-access-card">
          <div className="panel-header"><div><h3>{labels.portaria.lastAccess}</h3><span>{labels.portaria.lastAccessHint}</span></div></div>
          {latestMovement ? (
            <div className="last-access-body">
              <MercosulPlate plate={latestMovement.plate} />
              <div className="last-access-details">
                <strong>{latestMovement.vehicleOwner || labels.portaria.vehicleUnknown}</strong>
                <span>{latestMovement.apartmentNumber ? `Apto ${latestMovement.apartmentNumber}` : labels.portaria.apartmentUnknown}</span>
                <span>{latestMovement.parkingSpaceNumber ? `Vaga ${latestMovement.parkingSpaceNumber}` : labels.portaria.noSpace}</span>
              </div>
              <div className="last-access-status">
                <MovementDecisionBadge decision={latestMovement.decision} status={latestMovement.status} />
                <small>{getMovementDirectionLabel(latestMovement.direction)} · {formatDate(latestMovement.eventTime)}</small>
              </div>
            </div>
          ) : <EmptyState title={labels.portaria.waitingRead} description={labels.portaria.waitingReadDesc} />}
        </div>

        <div className="portaria-stat-stack">
          <StatCard label={labels.portaria.vehiclesInside} value={summary.vehiclesInside} hint={`${summary.vehiclesOutside} fora`} tone="neutral" />
          <StatCard label={labels.portaria.freeSpaces} value={summary.freeParkingSpaces} hint={`${summary.occupiedParkingSpaces} ocupadas`} tone="success" />
        </div>
      </section>

      <div className="dashboard-grid portaria-grid">
        <section className="panel wide-panel">
          <div className="panel-header"><div><h3>{labels.portaria.accessFlow}</h3><span>{labels.portaria.accessFlowHint}</span></div></div>
          {liveMovements.length === 0 ? <EmptyState title={labels.portaria.noEvents} description={labels.portaria.noEventsDesc} /> : (
            <div className="table-wrap"><table><thead><tr><th>{labels.dashboard.thTime}</th><th>{labels.dashboard.thPlate}</th><th>{labels.dashboard.thDirection}</th><th>{labels.dashboard.thResident}</th><th>{labels.dashboard.thSpace}</th><th>{labels.portaria.result}</th></tr></thead>
              <tbody>{liveMovements.map((movement) => (
                <tr key={movement.movementId}>
                  <td>{formatDate(movement.eventTime)}</td>
                  <td><strong className="plate-badge">{movement.plate}</strong></td>
                  <td><MovementDirectionBadge direction={movement.direction} /></td>
                  <td>{movement.vehicleOwner || '—'}{movement.apartmentNumber ? <small>Apto {movement.apartmentNumber}</small> : null}</td>
                  <td>{movement.parkingSpaceNumber || '—'}</td>
                  <td><MovementDecisionBadge decision={movement.decision} status={movement.status} /></td>
                </tr>
              ))}</tbody>
            </table></div>
          )}
        </section>

        <section className="panel">
          <div className="panel-header"><div><h3>{labels.portaria.camerasTitle}</h3><span>{labels.portaria.camerasHint}</span></div></div>
          <div className="camera-list">{cameras.map((camera) => (
            <div className="camera-row" key={camera.id}>
              <div className={`camera-icon ${camera.enabled ? 'online' : 'offline'}`}><Camera className="h-4 w-4" /></div>
               <div className="camera-info"><strong>{camera.name}</strong><span>{camera.direction === 'ENTRY' ? labels.portaria.entry : labels.portaria.exit} · {camera.ipAddress}</span><small>Última placa: {camera.lastPlate || '—'} · {formatDate(camera.lastEventAt)}</small></div>
            </div>
          ))}</div>
        </section>

        <section className="panel">
          <div className="panel-header"><div><h3>{labels.portaria.alertsTitle}</h3><span>{labels.portaria.alertsHint}</span></div></div>
          {alerts.length === 0 ? <EmptyState title={labels.dashboard.normalOperation} description={labels.portaria.noAlerts} /> : <div className="alert-list">{alerts.slice(0, 8).map((alert) => (
            <article className={`alert-row ${alert.severity.toLowerCase()}`} key={alert.id}><div className="alert-marker"><AlertTriangle className="h-3.5 w-3.5" /></div><div><strong>{alert.title}</strong><p>{alert.message}</p><small>{formatDate(alert.occurredAt)}</small></div></article>
          ))}</div>}
        </section>

        <section className="panel">
          <div className="panel-header"><div><h3>{labels.portaria.occupancyTitle}</h3><span>{labels.portaria.occupancyHint}</span></div></div>
          <div className="space-grid">{occupancy.slice(0, 24).map((space) => <div key={space.parkingSpaceId} className={`space-box ${space.status.toLowerCase()}`} title={`${space.parkingSpaceNumber} · ${space.apartmentNumber || labels.common.commonSpace}`}><span>{space.parkingSpaceNumber}</span></div>)}</div>
        </section>

        {hasPermission('GATE_TEST') && gateStatus?.driver === 'SIMULATED' && (
          <section className="panel manual-access-panel">
            <div className="panel-header"><div><h3>{labels.portaria.gateControl}</h3><span>{gateStatus?.driver === 'SIMULATED' ? labels.portaria.gateSimulated : labels.portaria.gateConfigured}</span></div></div>
            <div className="camera-list">
              <div className="camera-row"><div className={`camera-icon ${gateStatus?.enabled ? 'online' : 'offline'}`}><Camera className="h-4 w-4" /></div><div className="camera-info"><strong>{labels.portaria.entry}</strong><span>{gateStatus?.driver || '—'} · {gateStatus?.entryState || '—'}</span><small>{gateStatus?.entryMessage || labels.portaria.noInfo}</small></div></div>
              <div className="camera-row"><div className={`camera-icon ${gateStatus?.enabled ? 'online' : 'offline'}`}><Camera className="h-4 w-4" /></div><div className="camera-info"><strong>{labels.portaria.exit}</strong><span>{gateStatus?.driver || '—'} · {gateStatus?.exitState || '—'}</span><small>{gateStatus?.exitMessage || labels.portaria.noInfo}</small></div></div>
            </div>
            <form className="manual-access-form" onSubmit={submitGateTest}>
              <label><span>{labels.portaria.directionLabel}</span><select value={gateDirection} onChange={(e) => setGateDirection(e.target.value as 'ENTRY' | 'EXIT')}><option value="ENTRY">{labels.portaria.entry}</option><option value="EXIT">{labels.portaria.exit}</option></select></label>
              <label className="manual-justification"><span>{labels.portaria.gateTestJustification}</span><textarea value={gateJustification} onChange={(e) => { setGateJustification(e.target.value); if (fieldErrors.gateJustification) setFieldErrors((p) => { const n = { ...p }; delete n.gateJustification; return n; }); }} maxLength={LIMITS.justificationGate.max} rows={2} placeholder={labels.portaria.gateTestPlaceholder} aria-invalid={Boolean(fieldErrors.gateJustification)} aria-describedby={fieldErrors.gateJustification ? 'gate-just-error' : undefined} />{fieldErrors.gateJustification && <small className="field-error" id="gate-just-error" role="alert">{fieldErrors.gateJustification}</small>}</label>
              <div className="modal-actions"><button className="primary-button" disabled={gateTesting || !gateStatus?.enabled}>{gateTesting ? labels.common.sending : gateStatus?.driver === 'SIMULATED' ? labels.portaria.testGateSimulated : labels.portaria.testGate}</button></div>
            </form>
          </section>
        )}

        {hasPermission('MOVEMENT_MANUAL_WRITE') && (
          <section className="panel manual-access-panel">
            <div className="panel-header"><div><h3>{labels.portaria.manualMovement}</h3><span>{labels.portaria.manualMovementHint}</span></div></div>
            <form className="manual-access-form" onSubmit={submitManualMovement}>
              <label><span>{labels.portaria.plateLabel}</span><input value={manualPlate} onChange={(e) => { setManualPlate(e.target.value.toUpperCase()); if (fieldErrors.manualPlate) setFieldErrors((p) => { const n = { ...p }; delete n.manualPlate; return n; }); }} placeholder={labels.portaria.platePlaceholder} maxLength={8} aria-invalid={Boolean(fieldErrors.manualPlate)} aria-describedby={fieldErrors.manualPlate ? 'manual-plate-error' : undefined} />{fieldErrors.manualPlate && <small className="field-error" id="manual-plate-error" role="alert">{fieldErrors.manualPlate}</small>}</label>
              <label><span>{labels.portaria.directionLabel}</span><select value={manualDirection} onChange={(e) => setManualDirection(e.target.value as 'ENTRY' | 'EXIT')}><option value="ENTRY">{labels.portaria.entry}</option><option value="EXIT">{labels.portaria.exit}</option></select></label>
              <label className="manual-justification"><span>{labels.portaria.justificationLabel}</span><textarea value={justification} onChange={(e) => { setJustification(e.target.value); if (fieldErrors.justification) setFieldErrors((p) => { const n = { ...p }; delete n.justification; return n; }); }} maxLength={LIMITS.justificationManual.max} rows={3} placeholder={labels.portaria.justificationPlaceholder} aria-invalid={Boolean(fieldErrors.justification)} aria-describedby={fieldErrors.justification ? 'manual-just-error' : undefined} />{fieldErrors.justification && <small className="field-error" id="manual-just-error" role="alert">{fieldErrors.justification}</small>}</label>
              <div className="modal-actions"><button className="primary-button" disabled={submitting}>{submitting ? labels.common.registering : labels.portaria.registerMovement}</button></div>
            </form>
          </section>
        )}
      </div>
    </div>
  );
}
