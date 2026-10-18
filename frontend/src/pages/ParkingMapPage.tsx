import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import type { DashboardOccupancy, ParkingSpaceType, ParkingSpaceStatus, Vehicle } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import SearchInput from '@/components/ui/SearchInput';
import ParkingStatusBadge from '@/components/ui/ParkingStatusBadge';
import ParkingTypeBadge from '@/components/ui/ParkingTypeBadge';
import { messages } from '@/lib/presentation/messages';
import { labels } from '@/lib/presentation/labels';
import { parkingStatusFilterOptions, parkingTypeFilterOptions } from '@/lib/presentation/parking-space';
import { MapPin, X } from 'lucide-react';

interface MapSpace {
  id: number;
  number: string;
  apartmentId: number | null;
  apartmentNumber: string | null;
  type: string;
  status: ParkingSpaceStatus;
  active: boolean;
  assignedPlate: string | null;
  vehicleInside: boolean;
  residentNames: string[];
}

export default function ParkingMapPage() {
  const [occupancy, setOccupancy] = useState<DashboardOccupancy[]>([]);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [occ, vhs] = await Promise.all([
        api.dashboard.occupancy(),
        api.vehicles.list(),
      ]);
      setOccupancy(occ);
      setVehicles(vhs);
      setError('');
    } catch (err) {
      setError(getApiErrorMessage(err, messages.errors.loadParking));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const spaces = useMemo<MapSpace[]>(() => {
    return occupancy.map((occ) => {
      const plate = occ.assignedPlate;
      const matchedVehicles = plate ? vehicles.filter((v) => v.plate === plate) : [];
      const residentNames = matchedVehicles.flatMap((v) => v.residents.map((r) => r.fullName));
      return {
        id: occ.parkingSpaceId,
        number: occ.parkingSpaceNumber,
        apartmentId: occ.apartmentId,
        apartmentNumber: occ.apartmentNumber,
        type: occ.type,
        status: occ.status,
        active: occ.active,
        assignedPlate: occ.assignedPlate,
        vehicleInside: occ.vehicleInside,
        residentNames: [...new Set(residentNames)],
      };
    });
  }, [occupancy, vehicles]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return spaces.filter((s) => {
      if (statusFilter !== 'ALL' && s.status !== statusFilter) return false;
      if (typeFilter !== 'ALL' && s.type !== typeFilter) return false;
      if (q) {
        const matchesNumber = s.number.toLowerCase().includes(q);
        const matchesApt = s.apartmentNumber?.toLowerCase().includes(q);
        const matchesResident = s.residentNames.some((name) => name.toLowerCase().includes(q));
        if (!matchesNumber && !matchesApt && !matchesResident) return false;
      }
      return true;
    });
  }, [spaces, query, statusFilter, typeFilter]);

  const stats = useMemo(() => {
    const total = filtered.length;
    const occupied = filtered.filter((s) => s.status === 'OCCUPIED').length;
    const available = filtered.filter((s) => s.status === 'AVAILABLE').length;
    const blocked = filtered.filter((s) => s.status === 'BLOCKED').length;
    return { total, occupied, available, blocked };
  }, [filtered]);

  const hasActiveFilters = query || statusFilter !== 'ALL' || typeFilter !== 'ALL';

  function clearFilters() {
    setQuery('');
    setStatusFilter('ALL');
    setTypeFilter('ALL');
  }

  return (
    <div className="page-content">
      <PageHeader
        title={labels.parkingMap.title}
        description={labels.parkingMap.description}
        action={<button className="secondary-button" onClick={() => void load()}>{labels.common.update}</button>}
      />

      <section className="panel parking-map-filters">
        <div className="parking-map-filter-grid">
          <SearchInput
            value={query}
            onChange={setQuery}
            placeholder={labels.parkingMap.searchPlaceholder}
            label="Buscar"
            className="parking-map-search"
          />
          <label>
            <span>{labels.parkingMap.statusLabel}</span>
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} aria-label="Filtrar por status">
              {parkingStatusFilterOptions.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
            </select>
          </label>
          <label>
            <span>{labels.parkingMap.typeLabel}</span>
            <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} aria-label="Filtrar por tipo">
              {parkingTypeFilterOptions.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
            </select>
          </label>
          {hasActiveFilters && (
            <button className="secondary-button parking-map-clear" onClick={clearFilters}>
              <X className="h-3.5 w-3.5" /> {labels.parkingMap.clearFilters}
            </button>
          )}
        </div>
        <div className="parking-map-stats">
          <span>{stats.total} {labels.parkingMap.statTotal}</span>
          <span className="parking-map-stat"><span className="parking-map-dot occupied" /> {stats.occupied} {labels.parkingMap.statOccupied}</span>
          <span className="parking-map-stat"><span className="parking-map-dot available" /> {stats.available} {labels.parkingMap.statAvailable}</span>
          <span className="parking-map-stat"><span className="parking-map-dot blocked" /> {stats.blocked} {labels.parkingMap.statBlocked}</span>
        </div>
      </section>

      {error && <div className="banner warning" role="alert">{error}</div>}

      {loading ? <LoadingState label={labels.parkingMap.loading} /> : filtered.length === 0 ? (
        <EmptyState title={labels.parkingMap.emptyTitle} description={hasActiveFilters ? "Ajuste os filtros para visualizar as vagas." : "Não existem vagas cadastradas no sistema."} />
      ) : (
        <section className="parking-map-grid">
          {filtered.map((space) => (
            <article
              key={space.id}
              className={`space-card ${space.status.toLowerCase()} ${!space.active ? 'inactive' : ''}`}
              title={`${space.number} · ${space.apartmentNumber ? `${labels.parkingMap.apartmentPrefix} ${space.apartmentNumber}` : labels.common.commonSpace}`}
            >
              <div className="space-card-header">
                <strong>{space.number}</strong>
                <div className="space-card-badges">
                  <ParkingStatusBadge status={space.status as ParkingSpaceStatus} />
                  <ParkingTypeBadge type={space.type as ParkingSpaceType} />
                </div>
              </div>
              <div className="space-card-body">
                <div className="space-card-info">
                  <MapPin className="h-3 w-3" />
                  <span>{space.apartmentNumber ? `${labels.parkingMap.apartmentPrefix} ${space.apartmentNumber}` : labels.common.commonSpace}</span>
                </div>
                {space.assignedPlate && (
                  <div className="space-card-info">
                    <span className="plate-badge">{space.assignedPlate}</span>
                    {space.vehicleInside && <span className="space-card-inside">{labels.parkingMap.insideBadge}</span>}
                  </div>
                )}
                {space.residentNames.length > 0 && (
                  <div className="space-card-residents">
                    {space.residentNames.map((name) => (
                      <span key={name} className="space-card-resident">{name}</span>
                    ))}
                  </div>
                )}
              </div>
              {!space.active && <div className="space-card-footer"><small>{labels.parkingMap.inactiveBadge}</small></div>}
            </article>
          ))}
        </section>
      )}
    </div>
  );
}
