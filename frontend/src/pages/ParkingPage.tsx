import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import { useAuth } from '@/auth/auth-context';
import type { Apartment, ParkingSpace, ParkingSpaceType } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import Modal from '@/components/Modal';
import SearchInput from '@/components/ui/SearchInput';
import FilterBar from '@/components/ui/FilterBar';
import ParkingStatusBadge from '@/components/ui/ParkingStatusBadge';
import ParkingTypeBadge from '@/components/ui/ParkingTypeBadge';
import ActiveStatusBadge from '@/components/ui/ActiveStatusBadge';
import { parkingSpaceTypeLabels, parkingStatusFilterOptions } from '@/lib/presentation/parking-space';
import { labels } from '@/lib/presentation/labels';
import { validateParkingNumber, validateParkingTypeRequiresApartment, LIMITS } from '@/lib/validation';
import type { FieldErrors } from '@/lib/validation';

const TYPES: ParkingSpaceType[] = ['ASSIGNED', 'RENTED', 'EXTRA', 'VISITOR'];
const emptyForm = { number: '', apartmentId: '', type: 'ASSIGNED' as ParkingSpaceType, active: true };

export default function ParkingPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('PARKING_WRITE');
  const [items, setItems] = useState<ParkingSpace[]>([]);
  const [apartments, setApartments] = useState<Apartment[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [filter, setFilter] = useState('ALL');
  const [query, setQuery] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const numberInputRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [spaces, apt] = await Promise.all([api.parking.list(), api.apartments.list()]);
      setItems(spaces);
      setApartments(apt);
      setError('');
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(
    () => items.filter((item) => (filter === 'ALL' || item.status === filter) && item.number.toLowerCase().includes(query.toLowerCase().trim())),
    [items, filter, query],
  );

  function openNew() { setEditingId(null); setForm(emptyForm); setError(''); setFieldErrors({}); setModal(true); setTimeout(() => numberInputRef.current?.focus(), 50); }
  function openEdit(item: ParkingSpace) {
    setEditingId(item.id);
    setForm({ number: item.number, apartmentId: item.apartmentId ? String(item.apartmentId) : '', type: item.type, active: item.active });
    setError(''); setFieldErrors({});
    setModal(true);
  }

  function validateForm(): boolean {
    const errors: FieldErrors = {};
    const numErr = validateParkingNumber(form.number);
    if (numErr) errors.number = numErr;
    const aptErr = validateParkingTypeRequiresApartment(form.type, form.apartmentId);
    if (aptErr) errors.apartmentId = aptErr;
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      numberInputRef.current?.focus();
      return false;
    }
    return true;
  }

  async function save() {
    if (!validateForm()) return;
    setSaving(true);
    setError('');
    try {
      const payload = { number: form.number.trim(), apartmentId: form.apartmentId ? Number(form.apartmentId) : null, type: form.type, active: form.active };
      const result = editingId ? await api.parking.update(editingId, payload) : await api.parking.create(payload);
      setItems((prev) => editingId ? prev.map((item) => item.id === result.id ? result : item) : [...prev, result].sort((a, b) => a.number.localeCompare(b.number)));
      setModal(false);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page-content">
      <PageHeader
        title={labels.parking.title}
        description={labels.parking.description}
        action={canWrite ? <button className="primary-button" onClick={openNew}>{labels.parking.newButton}</button> : undefined}
      />

      <FilterBar count={filtered.length} countLabel={labels.parking.countLabel}>
        <SearchInput value={query} onChange={setQuery} placeholder={labels.parking.searchPlaceholder} label="Buscar vaga" className="min-w-[240px]" />
        <select value={filter} onChange={(e) => setFilter(e.target.value)} aria-label={labels.parking.statusFilterLabel}>
          {parkingStatusFilterOptions.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
        </select>
      </FilterBar>

      {error && <div className="banner warning" role="alert">{error}</div>}

      <section className="panel">
        {loading ? <LoadingState /> : filtered.length === 0 ? (
          <EmptyState title={labels.parking.emptyTitle} description={labels.parking.emptyDesc} />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{labels.parking.thSpace}</th>
                  <th>{labels.parking.thApartment}</th>
                  <th>{labels.parking.thType}</th>
                  <th>{labels.parking.thStatus}</th>
                  <th>{labels.parking.thActive}</th>
                  <th>{labels.parking.thActions}</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((space) => (
                  <tr key={space.id}>
                    <td><strong className="plate-badge">{space.number}</strong></td>
                    <td>{space.apartmentNumber ? `${labels.parking.apartmentPrefix} ${space.apartmentNumber}` : labels.parking.commonSpace}</td>
                    <td><ParkingTypeBadge type={space.type} /></td>
                    <td><ParkingStatusBadge status={space.status} /></td>
                    <td><ActiveStatusBadge active={space.active} /></td>
                    <td>{canWrite && <button className="secondary-button small" onClick={() => openEdit(space)}>{labels.parking.editButton}</button>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {modal && (
        <Modal title={editingId ? labels.parking.editModalTitle : labels.parking.newModalTitle} description={labels.parking.modalDesc} onClose={() => setModal(false)}>
          <div className="form-grid">
            <label>
              <span>{labels.parking.numberLabel}</span>
              <input
                ref={numberInputRef}
                value={form.number}
                onChange={(e) => { setForm({ ...form, number: e.target.value }); if (fieldErrors.number) setFieldErrors((p) => { const n = { ...p }; delete n.number; return n; }); }}
                placeholder={labels.parking.numberPlaceholder}
                maxLength={LIMITS.parkingNumber.max}
                aria-invalid={Boolean(fieldErrors.number)}
                aria-describedby={fieldErrors.number ? 'parking-number-error' : undefined}
              />
              {fieldErrors.number && <small className="field-error" id="parking-number-error" role="alert">{fieldErrors.number}</small>}
            </label>
            <label>
              <span>{labels.parking.typeLabel}</span>
              <select value={form.type} onChange={(e) => { setForm({ ...form, type: e.target.value as ParkingSpaceType }); if (fieldErrors.apartmentId) setFieldErrors((p) => { const n = { ...p }; delete n.apartmentId; return n; }); }} aria-label={labels.parking.typeLabel}>
                {TYPES.map((type) => <option key={type} value={type}>{parkingSpaceTypeLabels[type]}</option>)}
              </select>
            </label>
            <label>
              <span>{labels.parking.apartmentLabel}</span>
              <select value={form.apartmentId} onChange={(e) => { setForm({ ...form, apartmentId: e.target.value }); if (fieldErrors.apartmentId) setFieldErrors((p) => { const n = { ...p }; delete n.apartmentId; return n; }); }} aria-label={labels.parking.apartmentLabel} aria-invalid={Boolean(fieldErrors.apartmentId)} aria-describedby={fieldErrors.apartmentId ? 'parking-apt-error' : undefined}>
                <option value="">{labels.parking.commonSpace}</option>
                {apartments.filter((a) => a.active).map((a) => <option key={a.id} value={a.id}>{labels.parking.apartmentPrefix} {a.number}</option>)}
              </select>
              {fieldErrors.apartmentId && <small className="field-error" id="parking-apt-error" role="alert">{fieldErrors.apartmentId}</small>}
            </label>
            <label className="checkbox-field">
              <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
              <span>{labels.parking.activeLabel}</span>
            </label>
          </div>
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
