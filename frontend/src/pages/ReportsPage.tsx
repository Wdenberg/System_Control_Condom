import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import type { AccessReportRow, AccessReportSummary, GateReportRow, OccupancyReportRow } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import MovementDecisionBadge from '@/components/ui/MovementDecisionBadge';
import ParkingStatusBadge from '@/components/ui/ParkingStatusBadge';
import GateStatusBadge from '@/components/ui/GateStatusBadge';
import { getMovementDirectionLabel, getMovementStatusLabel, getAccessDecisionLabel } from '@/lib/presentation/movement';
import { getParkingTypeLabel } from '@/lib/presentation/parking-space';
import type { ParkingSpaceType } from '@/types/api';
import { getGateOperationStatusLabel } from '@/lib/presentation/gate';
import { messages } from '@/lib/presentation/messages';
import { labels } from '@/lib/presentation/labels';
import { formatDate, localIso, today } from '@/lib/presentation/common';
import { validateReportDateRange } from '@/lib/validation';

export default function ReportsPage() {
  const [tab, setTab] = useState<'access' | 'occupancy' | 'gates'>('access');
  const [from, setFrom] = useState(today());
  const [to, setTo] = useState(today());
  const [plate, setPlate] = useState('');
  const [direction, setDirection] = useState('');
  const [status, setStatus] = useState('');
  const [decision, setDecision] = useState('');
  const [access, setAccess] = useState<AccessReportRow[]>([]);
  const [summary, setSummary] = useState<AccessReportSummary | null>(null);
  const [occupancy, setOccupancy] = useState<OccupancyReportRow[]>([]);
  const [gates, setGates] = useState<GateReportRow[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [gateStatus, setGateStatus] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [exporting, setExporting] = useState(false);

  const accessParams = useMemo(() => ({
    from: localIso(from),
    to: localIso(to, true),
    plate: plate.trim() || undefined,
    direction: direction || undefined,
    status: status || undefined,
    decision: decision || undefined,
  }), [from, to, plate, direction, status, decision]);

  const loadAccess = useCallback(async () => {
    const dateErr = validateReportDateRange(from, to);
    if (dateErr) { setError(dateErr); return; }
    setLoading(true);
    try {
      const [rows, nextSummary] = await Promise.all([
        api.reports.access(accessParams, page, 30),
        api.reports.accessSummary(accessParams),
      ]);
      setAccess(rows.content);
      setTotalPages(rows.totalPages);
      setSummary(nextSummary);
      setError('');
    }     catch (err) { setError(getApiErrorMessage(err, messages.errors.loadAccessReport)); }
    finally { setLoading(false); }
  }, [accessParams, page]);

  const loadOccupancy = useCallback(async () => {
    setLoading(true);
    try { setOccupancy(await api.reports.occupancy()); setError(''); }
    catch (err) { setError(getApiErrorMessage(err, messages.errors.loadOccupancyReport)); }
    finally { setLoading(false); }
  }, []);

  const loadGates = useCallback(async () => {
    const dateErr = validateReportDateRange(from, to);
    if (dateErr) { setError(dateErr); return; }
    setLoading(true);
    try {
      const result = await api.reports.gates({ from: accessParams.from, to: accessParams.to, status: gateStatus || undefined }, page, 30);
      setGates(result.content); setTotalPages(result.totalPages); setError('');
    }     catch (err) { setError(getApiErrorMessage(err, messages.errors.loadGateReport)); }
    finally { setLoading(false); }
  }, [accessParams.from, accessParams.to, gateStatus, page]);

  useEffect(() => {
    setPage(0);
  }, [from, to, plate, direction, status, decision, gateStatus, tab]);

  useEffect(() => {
    if (tab === 'access') void loadAccess();
  }, [tab, loadAccess]);

  useEffect(() => {
    if (tab === 'occupancy') void loadOccupancy();
  }, [tab, loadOccupancy]);

  useEffect(() => {
    if (tab === 'gates') void loadGates();
  }, [tab, loadGates]);

  async function exportCsv() {
    setExporting(true);
    try {
      const blob = await api.reports.exportAccessCsv(accessParams);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'relatorio-acessos.csv';
      anchor.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    }     catch (err) { setError(getApiErrorMessage(err, messages.errors.exportCsv)); }
    finally { setExporting(false); }
  }

  return (
    <div className="page-content">
      <PageHeader
        title={labels.reports.title}
        description={labels.reports.description}
        action={tab === 'access' ? <button className="secondary-button" onClick={() => void exportCsv()} disabled={exporting}>{exporting ? labels.common.exporting : labels.common.exportCsv}</button> : undefined}
      />

      <div className="report-tabs" role="tablist" aria-label="Tipo de relatório">
        <button className={tab === 'access' ? 'report-tab active' : 'report-tab'} role="tab" aria-selected={tab === 'access'} onClick={() => setTab('access')}>{labels.reports.tabAccess}</button>
        <button className={tab === 'occupancy' ? 'report-tab active' : 'report-tab'} role="tab" aria-selected={tab === 'occupancy'} onClick={() => setTab('occupancy')}>{labels.reports.tabOccupancy}</button>
        <button className={tab === 'gates' ? 'report-tab active' : 'report-tab'} role="tab" aria-selected={tab === 'gates'} onClick={() => setTab('gates')}>{labels.reports.tabGate}</button>
      </div>

      {tab === 'access' && <>
        <section className="report-filter-panel panel">
          <div className="report-filter-grid">
            <label><span>{labels.reports.fromLabel}</span><input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
            <label><span>{labels.reports.toLabel}</span><input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
            <label><span>{labels.reports.plateLabel}</span><input value={plate} onChange={(e) => setPlate(e.target.value.toUpperCase())} placeholder="ABC1D23" /></label>
            <label><span>{labels.reports.directionLabel}</span><select value={direction} onChange={(e) => setDirection(e.target.value)}><option value="">{labels.reports.allDirections}</option><option value="ENTRY">{getMovementDirectionLabel('ENTRY')}</option><option value="EXIT">{getMovementDirectionLabel('EXIT')}</option></select></label>
            <label><span>{labels.reports.statusLabel}</span><select value={status} onChange={(e) => setStatus(e.target.value)}><option value="">{labels.reports.allStatus}</option><option value="AUTHORIZED">{getMovementStatusLabel('AUTHORIZED')}</option><option value="DENIED">{getMovementStatusLabel('DENIED')}</option><option value="OVERRIDDEN">{getMovementStatusLabel('OVERRIDDEN')}</option></select></label>
            <label><span>{labels.reports.decisionLabel}</span><select value={decision} onChange={(e) => setDecision(e.target.value)}><option value="">{labels.reports.allDirections}</option><option value="UNKNOWN_VEHICLE">{getAccessDecisionLabel('UNKNOWN_VEHICLE')}</option><option value="LOW_CONFIDENCE">{getAccessDecisionLabel('LOW_CONFIDENCE')}</option><option value="PARKING_SPACE_OCCUPIED">{getAccessDecisionLabel('PARKING_SPACE_OCCUPIED')}</option><option value="DUPLICATE_ENTRY">{getAccessDecisionLabel('DUPLICATE_ENTRY')}</option><option value="NO_PARKING_ASSIGNED">{getAccessDecisionLabel('NO_PARKING_ASSIGNED')}</option><option value="OVERRIDE_AUTHORIZED">{getAccessDecisionLabel('OVERRIDE_AUTHORIZED')}</option></select></label>
          </div>
        </section>

        {summary && <section className="report-summary-grid">
          <div className="report-kpi"><span>{labels.reports.statTotal}</span><strong>{summary.total}</strong></div>
          <div className="report-kpi"><span>{labels.reports.statEntries}</span><strong>{summary.entries}</strong></div>
          <div className="report-kpi"><span>{labels.reports.statExits}</span><strong>{summary.exits}</strong></div>
          <div className="report-kpi"><span>{labels.reports.statAuthorized}</span><strong>{summary.authorized}</strong></div>
          <div className="report-kpi"><span>{labels.reports.statDenied}</span><strong>{summary.denied}</strong></div>
          <div className="report-kpi"><span>{labels.reports.statOverridden}</span><strong>{summary.overridden}</strong></div>
        </section>}

        {summary && Object.keys(summary.decisions).length > 0 && <section className="panel report-decisions"><div className="panel-header"><div><h3>{labels.reports.decisionsTitle}</h3><span>{labels.reports.decisionsHint}</span></div></div><div className="decision-chip-list">{Object.entries(summary.decisions).map(([name, count]) => <span key={name} className="permission-chip">{getAccessDecisionLabel(name)} · {count}</span>)}</div></section>}

        {error && <div className="banner warning" role="alert">{error}</div>}
        <section className="panel">
          <div className="panel-header"><div><h3>{labels.reports.access.title}</h3><span>{labels.reports.access.periodLabel} {from} {labels.reports.access.periodSep} {to}</span></div></div>
          {loading ? <LoadingState label={labels.reports.loadingReport} /> : access.length === 0 ? (
            <EmptyState title={labels.reports.access.emptyTitle} description={labels.reports.access.emptyDesc} />
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>{labels.reports.access.thDate}</th><th>{labels.reports.access.thPlate}</th><th>{labels.reports.access.thDirection}</th><th>{labels.reports.access.thResident}</th><th>{labels.reports.access.thApartment}</th><th>{labels.reports.access.thSpace}</th><th>{labels.reports.access.thDecision}</th><th>{labels.reports.access.thCamera}</th></tr></thead>
                <tbody>
                  {access.map((row) => (
                    <tr key={row.movementId}>
                      <td>{formatDate(row.eventTime)}</td>
                      <td><strong className="plate-badge">{row.plate}</strong></td>
                      <td>{getMovementDirectionLabel(row.direction)}</td>
                      <td>{row.vehicleOwner || '—'}</td>
                      <td>{row.apartmentNumber || '—'}</td>
                      <td>{row.parkingSpaceNumber || '—'}</td>
                      <td><MovementDecisionBadge decision={row.decision} status={row.status} /></td>
                      <td>{row.cameraName || '—'}<small>{row.cameraIp || ''}</small></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
        {tab === 'access' && !loading && <div className="pager"><button className="secondary-button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>{labels.common.previous}</button><span>{labels.common.page} {page + 1} {labels.common.of} {Math.max(totalPages, 1)}</span><button className="secondary-button" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>{labels.common.next}</button></div>}
      </>}

      {tab === 'occupancy' && <>
        {error && <div className="banner warning" role="alert">{error}</div>}
        <section className="panel">
          <div className="panel-header"><div><h3>{labels.reports.occupancy.title}</h3><span>{labels.reports.occupancy.hint}</span></div></div>
          {loading ? <LoadingState /> : occupancy.length === 0 ? (
            <EmptyState title={labels.reports.occupancy.emptyTitle} description={labels.reports.occupancy.emptyDesc} />
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>{labels.reports.occupancy.thSpace}</th><th>{labels.reports.occupancy.thApartment}</th><th>{labels.reports.occupancy.thType}</th><th>{labels.reports.occupancy.thStatus}</th><th>{labels.reports.occupancy.thPlate}</th><th>{labels.reports.occupancy.thInside}</th></tr></thead>
                <tbody>
                  {occupancy.map((row) => (
                    <tr key={row.parkingSpaceId}>
                      <td><strong>{row.parkingSpaceNumber}</strong></td>
                      <td>{row.apartmentNumber || labels.common.commonSpace}</td>
                      <td>{getParkingTypeLabel(row.type as ParkingSpaceType)}</td>
                      <td><ParkingStatusBadge status={row.status} /></td>
                      <td>{row.assignedPlate || '—'}</td>
                      <td>{row.vehicleInside ? labels.common.yes : labels.common.no}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </>}

      {tab === 'gates' && <>
        <section className="report-filter-panel panel">
          <div className="report-filter-grid">
            <label><span>{labels.reports.fromLabel}</span><input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
            <label><span>{labels.reports.toLabel}</span><input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
            <label><span>{labels.reports.statusLabel}</span><select value={gateStatus} onChange={(e) => setGateStatus(e.target.value)}><option value="">{labels.reports.allStatus}</option><option value="SENT">{getGateOperationStatusLabel('SENT')}</option><option value="PENDING">{getGateOperationStatusLabel('PENDING')}</option><option value="EXHAUSTED">{getGateOperationStatusLabel('EXHAUSTED')}</option><option value="DISABLED">{getGateOperationStatusLabel('DISABLED')}</option></select></label>
          </div>
        </section>
        {error && <div className="banner warning" role="alert">{error}</div>}
        <section className="panel">
          <div className="panel-header"><div><h3>{labels.reports.gate.title}</h3><span>{labels.reports.gate.hint}</span></div></div>
          {loading ? <LoadingState /> : gates.length === 0 ? (
            <EmptyState title={labels.reports.gate.emptyTitle} description={labels.reports.gate.emptyDesc} />
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>{labels.reports.gate.thDate}</th><th>{labels.reports.gate.thPlate}</th><th>{labels.reports.gate.thDirection}</th><th>{labels.reports.gate.thDriver}</th><th>{labels.reports.gate.thStatus}</th><th>{labels.reports.gate.thAttempts}</th><th>{labels.reports.gate.thError}</th></tr></thead>
                <tbody>
                  {gates.map((row) => (
                    <tr key={row.id}>
                      <td>{formatDate(row.requestedAt)}</td>
                      <td><strong className="plate-badge">{row.plate}</strong></td>
                      <td>{getMovementDirectionLabel(row.direction)}</td>
                      <td>{row.driver}</td>
                      <td><GateStatusBadge status={row.status} /></td>
                      <td>{row.attempts}</td>
                      <td>{row.lastError || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
        {!loading && <div className="pager"><button className="secondary-button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>{labels.common.previous}</button><span>{labels.common.page} {page + 1} {labels.common.of} {Math.max(totalPages, 1)}</span><button className="secondary-button" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>{labels.common.next}</button></div>}
      </>}
    </div>
  );
}
