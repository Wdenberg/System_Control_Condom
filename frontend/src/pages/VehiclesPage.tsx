import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import { useAuth } from '@/auth/auth-context';
import type { Apartment, ParkingSpace, Resident, Vehicle } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import Modal from '@/components/Modal';
import SearchInput from '@/components/ui/SearchInput';
import FilterBar from '@/components/ui/FilterBar';
import ActiveStatusBadge from '@/components/ui/ActiveStatusBadge';
import { validatePlate, normalizePlate, LIMITS } from '@/lib/validation';
import type { FieldErrors } from '@/lib/validation';
import { messages } from '@/lib/presentation/messages';
import { labels } from '@/lib/presentation/labels';

const emptyForm = { plate: '', brand: '', model: '', color: '', residentIds: [] as number[], parkingSpaceId: '', active: true };

export default function VehiclesPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('VEHICLE_WRITE');
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [residents, setResidents] = useState<Resident[]>([]);
  const [apartments, setApartments] = useState<Apartment[]>([]);
  const [spaces, setSpaces] = useState<ParkingSpace[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('ALL');
  const [residentApartmentId, setResidentApartmentId] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const plateInputRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [aptList, vehicleList, spaceList] = await Promise.all([
        api.apartments.list(),
        api.vehicles.list(),
        api.parking.list(),
      ]);
      setApartments(aptList);
      setVehicles(vehicleList);
      setSpaces(spaceList);
      setError('');
    }     catch (err) { setError(getApiErrorMessage(err)); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (!residentApartmentId) { setResidents([]); return; }
    let cancelled = false;
    api.apartments.residents(Number(residentApartmentId))
      .then((list) => { if (!cancelled) setResidents(list.filter((r) => r.active)); })
      .catch(() => { if (!cancelled) setResidents([]); });
    return () => { cancelled = true; };
  }, [residentApartmentId]);

  const filtered = useMemo(() => vehicles.filter((v) => (filter === 'ALL' || (filter === 'ACTIVE' ? v.active : !v.active)) && v.plate.includes(query.trim().toUpperCase())), [vehicles, filter, query]);
  const residentOptions = residents.filter((r) => !residentApartmentId || String(r.apartmentId) === residentApartmentId);
  const selectedResidents = residents.filter((r) => form.residentIds.includes(r.id));
  const selectedApartmentIds = [...new Set(selectedResidents.map((r) => r.apartmentId))];
  const allowedSpaces = spaces.filter((s) => s.active && s.status === 'AVAILABLE' && (!residentApartmentId ? true : String(s.apartmentId) === residentApartmentId)).filter((s) => !selectedApartmentIds.length || selectedApartmentIds.includes(s.apartmentId ?? -1) || s.id === Number(form.parkingSpaceId));

  function openNew() { setEditingId(null); setForm(emptyForm); setResidentApartmentId(''); setError(''); setFieldErrors({}); setModal(true); setTimeout(() => plateInputRef.current?.focus(), 50); }
  function openEdit(v: Vehicle) {
    const apartmentId = String(v.apartmentId);
    setEditingId(v.id); setResidentApartmentId(apartmentId); setForm({ plate: v.plate, brand: v.brand ?? '', model: v.model ?? '', color: v.color ?? '', residentIds: v.residents.map((r) => r.id), parkingSpaceId: v.parkingSpaceId ? String(v.parkingSpaceId) : '', active: v.active }); setError(''); setFieldErrors({}); setModal(true);
  }

  function toggleResident(id: number) {
    setForm((prev) => ({ ...prev, residentIds: prev.residentIds.includes(id) ? prev.residentIds.filter((item) => item !== id) : [...prev.residentIds, id], parkingSpaceId: prev.parkingSpaceId }));
  }

  function validateForm(): boolean {
    const errors: FieldErrors = {};
    const plateErr = validatePlate(form.plate);
    if (plateErr) errors.plate = plateErr;
    if (!form.residentIds.length) errors.residents = messages.validation.selectResident;
    if (form.brand.length > LIMITS.brand.max) errors.brand = `Marca deve ter no máximo ${LIMITS.brand.max} caracteres.`;
    if (form.model.length > LIMITS.model.max) errors.model = `Modelo deve ter no máximo ${LIMITS.model.max} caracteres.`;
    if (form.color.length > LIMITS.color.max) errors.color = `Cor deve ter no máximo ${LIMITS.color.max} caracteres.`;
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      plateInputRef.current?.focus();
      return false;
    }
    return true;
  }

  async function save() {
    if (!validateForm()) return;
    setSaving(true); setError('');
    try {
      const payload = { plate: normalizePlate(form.plate), brand: form.brand.trim() || undefined, model: form.model.trim() || undefined, color: form.color.trim() || undefined, residentIds: form.residentIds, parkingSpaceId: form.parkingSpaceId ? Number(form.parkingSpaceId) : null, active: form.active };
      const result = editingId ? await api.vehicles.update(editingId, payload) : await api.vehicles.create(payload);
      setVehicles((prev) => editingId ? prev.map((v) => v.id === result.id ? result : v) : [result, ...prev]); setModal(false);
    }     catch (err) { setError(getApiErrorMessage(err)); }
    finally { setSaving(false); }
  }

  return (
    <div className="page-content">
      <PageHeader title={labels.vehicles.title} description={labels.vehicles.description} action={canWrite ? <button className="primary-button" onClick={openNew}>{labels.vehicles.newButton}</button> : undefined} />

      <FilterBar count={filtered.length} countLabel={labels.vehicles.countLabel}>
        <SearchInput value={query} onChange={setQuery} placeholder={labels.vehicles.searchPlaceholder} label="Buscar veículo" />
        <select aria-label={labels.vehicles.statusFilterLabel} value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="ALL">{labels.common.all}</option>
          <option value="ACTIVE">{labels.common.activeLabel}</option>
          <option value="INACTIVE">{labels.common.inactiveLabel}</option>
        </select>
      </FilterBar>

      {error && <div className="banner warning" role="alert">{error}</div>}

      <section className="panel">
        {loading ? <LoadingState /> : filtered.length === 0 ? (
          <EmptyState title={labels.vehicles.emptyTitle} description={labels.vehicles.emptyDesc} />
        ) : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>{labels.vehicles.thPlate}</th><th>{labels.vehicles.thVehicle}</th><th>{labels.vehicles.thResidents}</th><th>{labels.vehicles.thApartment}</th><th>{labels.vehicles.thSpace}</th><th>{labels.vehicles.thStatus}</th><th>{labels.vehicles.thActions}</th></tr></thead>
              <tbody>
                {filtered.map((v) => (
                  <tr key={v.id}>
                    <td><strong className="plate-badge">{v.plate}</strong></td>
                    <td>{[v.brand, v.model].filter(Boolean).join(' ') || '—'}<small>{v.color || ''}</small></td>
                    <td>{v.residents.map((r) => r.fullName).join(', ')}</td>
                    <td>{v.apartmentNumber}</td>
                    <td>{v.parkingSpaceNumber || '—'}</td>
                    <td><ActiveStatusBadge active={v.active} /></td>
                    <td>{canWrite && <button className="secondary-button small" onClick={() => openEdit(v)}>{labels.vehicles.editButton}</button>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {modal && (
        <Modal title={editingId ? labels.vehicles.editModalTitle : labels.vehicles.newModalTitle} description={labels.vehicles.modalDesc} width="large" onClose={() => setModal(false)}>
          <div className="form-grid three">
            <label>
              <span>{labels.vehicles.plateLabel}</span>
              <input
                ref={plateInputRef}
                value={form.plate}
                onChange={(e) => { setForm({ ...form, plate: e.target.value.toUpperCase() }); if (fieldErrors.plate) setFieldErrors((p) => { const n = { ...p }; delete n.plate; return n; }); }}
                placeholder="ABC1D23"
                maxLength={8}
                aria-invalid={Boolean(fieldErrors.plate)}
                aria-describedby={fieldErrors.plate ? 'plate-error' : undefined}
              />
              {fieldErrors.plate && <small className="field-error" id="plate-error" role="alert">{fieldErrors.plate}</small>}
            </label>
            <label>
              <span>{labels.vehicles.brandLabel}</span>
              <input value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} maxLength={LIMITS.brand.max} />
            </label>
            <label>
              <span>{labels.vehicles.modelLabel}</span>
              <input value={form.model} onChange={(e) => setForm({ ...form, model: e.target.value })} maxLength={LIMITS.model.max} />
            </label>
            <label>
              <span>{labels.vehicles.colorLabel}</span>
              <input value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} maxLength={LIMITS.color.max} />
            </label>
            <label>
              <span>{labels.vehicles.apartmentLabel}</span>
              <select value={residentApartmentId} onChange={(e) => { setResidentApartmentId(e.target.value); setForm({ ...form, residentIds: [], parkingSpaceId: '' }); }}><option value="">{labels.vehicles.apartmentPlaceholder}</option>{apartments.filter((a) => a.active).map((a) => <option key={a.id} value={a.id}>Apto {a.number}</option>)}</select>
            </label>
            <label>
              <span>{labels.vehicles.spaceLabel}</span>
              <select value={form.parkingSpaceId} onChange={(e) => setForm({ ...form, parkingSpaceId: e.target.value })}><option value="">{labels.vehicles.noSpace}</option>{allowedSpaces.map((s) => <option key={s.id} value={s.id}>Vaga {s.number} · Apto {s.apartmentNumber}</option>)}</select>
            </label>
            <label className="checkbox-field"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /><span>{labels.vehicles.activeLabel}</span></label>
          </div>
          <div className="multi-select">
            <div className="field-label">{labels.vehicles.linkedResidents}</div>
            {residentApartmentId ? (
              <div className="check-grid">{residentOptions.map((r) => <label key={r.id} className="check-option"><input type="checkbox" checked={form.residentIds.includes(r.id)} onChange={() => toggleResident(r.id)} /><span>{r.fullName}</span></label>)}</div>
            ) : (
              <div className="empty-inline">{labels.vehicles.selectApartmentHint}</div>
            )}
            {fieldErrors.residents && <small className="field-error" role="alert">{fieldErrors.residents}</small>}
          </div>
          {selectedResidents.length > 0 && <div className="selection-summary">{labels.vehicles.selectedLabel} {selectedResidents.map((r) => r.fullName).join(', ')}</div>}
          {error && <div className="form-error" role="alert">{error}</div>}
          <div className="modal-actions">
            <button className="secondary-button" onClick={() => setModal(false)}>{labels.common.cancel}</button>
            <button className="primary-button" onClick={() => void save()} disabled={saving}>{saving ? labels.common.saving : labels.common.save}</button>
          </div>
        </Modal>
      )}
    </div>
  );
}
