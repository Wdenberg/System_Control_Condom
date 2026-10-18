import { useCallback, useEffect, useRef, useState } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import { useRealtime } from '@/realtime/realtime-context';
import type { DashboardActivity, DashboardAlert, DashboardCamera, DashboardOccupancy, DashboardSummary, GateStatus } from '@/types/api';
export function useOperationalDashboard(includeGate = false) {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [activity, setActivity] = useState<DashboardActivity[]>([]);
  const [occupancy, setOccupancy] = useState<DashboardOccupancy[]>([]);
  const [cameras, setCameras] = useState<DashboardCamera[]>([]);
  const [alerts, setAlerts] = useState<DashboardAlert[]>([]);
  const [gateStatus, setGateStatus] = useState<GateStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const generation = useRef(0);
  const mounted = useRef(true);
  const { lastEvent, connected } = useRealtime();
  const load = useCallback(async (initial = false) => {
    const request = ++generation.current;
    initial ? setLoading(true) : setRefreshing(true);
    try {
      const [nextSummary, nextActivity, nextOccupancy, nextCameras, nextAlerts, nextGate] = await Promise.all([
        api.dashboard.summary(), api.dashboard.activity(12), api.dashboard.occupancy(), api.dashboard.cameras(), api.dashboard.alerts(8),
        includeGate ? api.gates.status() : Promise.resolve(null),
      ]);
      if (!mounted.current || request !== generation.current) return;
      setSummary(nextSummary); setActivity(nextActivity); setOccupancy(nextOccupancy); setCameras(nextCameras); setAlerts(nextAlerts); setGateStatus(nextGate); setError('');
    } catch (failure) { if (mounted.current && request === generation.current) setError(getApiErrorMessage(failure, 'Não foi possível atualizar a operação.')); }
    finally { if (mounted.current && request === generation.current) { setLoading(false); setRefreshing(false); } }
  }, [includeGate]);
  useEffect(() => {
    mounted.current = true; void load(true);
    const timer = window.setInterval(() => { void load(); }, 60_000);
    return () => { mounted.current = false; generation.current++; window.clearInterval(timer); };
  }, [load]);
  useEffect(() => {
    if (!lastEvent || lastEvent.type === 'heartbeat') return;
    const timer = window.setTimeout(() => { void load(); }, 250);
    return () => window.clearTimeout(timer);
  }, [lastEvent, load]);
  return { summary, activity, occupancy, cameras, alerts, loading, refreshing, error, setError, gateStatus, setGateStatus, load, connected };
}
