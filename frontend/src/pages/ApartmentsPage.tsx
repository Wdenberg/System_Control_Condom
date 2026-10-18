import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import { useAuth } from '@/auth/auth-context';
import type { Apartment, Resident } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import Modal from '@/components/Modal';
import SearchInput from '@/components/ui/SearchInput';
import ActiveStatusBadge from '@/components/ui/ActiveStatusBadge';
import { formatProperName, validateApartmentNumber, validateResidentName, LIMITS } from '@/lib/validation';
import type { FieldErrors } from '@/lib/validation';
import { labels } from '@/lib/presentation/labels';

type ApartmentForm = { number: string; active: boolean };
type ResidentForm = { fullName: string; active: boolean };

const emptyApartment: ApartmentForm = { number: '', active: true };
const emptyResident: ResidentForm = { fullName: '', active: true };

export default function ApartmentsPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('APARTMENT_WRITE');
  const [items, setItems] = useState<Apartment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Apartment | null>(null);
  const [residents, setResidents] = useState<Resident[]>([]);
  const [residentsLoading, setResidentsLoading] = useState(false);
  const [form, setForm] = useState<ApartmentForm>(emptyApartment);
  const [residentForm, setResidentForm] = useState<ResidentForm>(emptyResident);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingResidentId, setEditingResidentId] = useState<number | null>(null);
  const [modal, setModal] = useState<'apartment' | 'residents' | null>(null);
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const apartmentInputRef = useRef<HTMLInputElement>(null);
  const residentInputRef = useRef<HTMLInputElement>(null);

  const filtered = useMemo(() => items.filter((item) => item.number.toLowerCase().includes(query.trim().toLowerCase())), [items, query]);

  const load = useCallback(async () => {
    setLoading(true);
    try { setItems(await api.apartments.list()); setError(''); }
    catch (err) { setError(getApiErrorMessage(err)); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void load(); }, [load]);

  function openNew() {
    setEditingId(null);
    setForm(emptyApartment);
    setFieldErrors({});
    setError('');
    setModal('apartment');
    setTimeout(() => apartmentInputRef.current?.focus(), 50);
  }

  function openEdit(item: Apartment) {
    setEditingId(item.id);
    setForm({ number: item.number, active: item.active });
    setFieldErrors({});
    setError('');
    setModal('apartment');
    setTimeout(() => apartmentInputRef.current?.focus(), 50);
  }

  function validateApartmentForm(): boolean {
    const errors: FieldErrors = {};
    const numErr = validateApartmentNumber(form.number);
    if (numErr) errors.number = numErr;
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      apartmentInputRef.current?.focus();
      return false;
    }
    return true;
  }

  async function saveApartment() {
    if (!validateApartmentForm()) return;
    setSaving(true); setError('');
    try {
      const result = editingId
        ? await api.apartments.update(editingId, { number: form.number.trim(), active: form.active })
        : await api.apartments.create({ number: form.number.trim(), active: form.active });
      setItems((prev) => editingId
        ? prev.map((item) => item.id === result.id ? result : item).sort((a, b) => a.number.localeCompare(b.number))
        : [...prev, result].sort((a, b) => a.number.localeCompare(b.number)));
      setModal(null);
    }     catch (err) { setError(getApiErrorMessage(err)); }
    finally { setSaving(false); }
  }

  async function openResidents(item: Apartment) {
    setSelected(item); setModal('residents'); setResidentsLoading(true); setResidents([]); setError(''); setFieldErrors({});
    try { setResidents(await api.apartments.residents(item.id)); }
    catch (err) { setError(getApiErrorMessage(err)); }
    finally { setResidentsLoading(false); }
  }

  function startResident(item?: Resident) {
    setEditingResidentId(item?.id ?? null);
    setResidentForm(item ? { fullName: item.fullName, active: item.active } : emptyResident);
    setFieldErrors({});
    setTimeout(() => residentInputRef.current?.focus(), 50);
  }

  function handleResidentBlur() {
    const formatted = formatProperName(residentForm.fullName);
    if (formatted !== residentForm.fullName) setResidentForm((prev) => ({ ...prev, fullName: formatted }));
  }

  function validateResidentForm(): boolean {
    const errors: FieldErrors = {};
    const nameErr = validateResidentName(residentForm.fullName);
    if (nameErr) errors.residentName = nameErr;
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      residentInputRef.current?.focus();
      return false;
    }
    return true;
  }

  async function saveResident() {
    if (!selected) return;
    if (!validateResidentForm()) return;
    const formattedName = formatProperName(residentForm.fullName);
    setSaving(true); setError('');
    try {
      const result = editingResidentId
        ? await api.apartments.updateResident(selected.id, editingResidentId, { fullName: formattedName, active: residentForm.active })
        : await api.apartments.createResident(selected.id, { fullName: formattedName, active: residentForm.active });
      setResidents((prev) => editingResidentId ? prev.map((r) => r.id === result.id ? result : r) : [...prev, result].sort((a, b) => a.fullName.localeCompare(b.fullName)));
      setResidentForm(emptyResident); setEditingResidentId(null);
      setFieldErrors({});
    }     catch (err) { setError(getApiErrorMessage(err)); }
    finally { setSaving(false); }
  }

  return (
    <div className="page-content">
      <PageHeader title={labels.apartments.title} description={labels.apartments.description} action={canWrite ? <button className="primary-button" onClick={openNew}>{labels.apartments.newButton}</button> : undefined} />

      <div className="toolbar">
        <SearchInput value={query} onChange={setQuery} placeholder={labels.apartments.searchPlaceholder} label={labels.apartments.searchLabel} />
        <span>{filtered.length} de {items.length}</span>
      </div>

      {error && <div className="banner warning" role="alert">{error}</div>}

      <section className="panel">
        <div className="panel-header"><div><h3>{labels.apartments.listTitle}</h3><span>{labels.apartments.listHint}</span></div></div>
        {loading ? <LoadingState /> : filtered.length === 0 ? (
          <EmptyState title={labels.apartments.emptyTitle} description={labels.apartments.emptyDesc} />
        ) : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>{labels.apartments.thUnit}</th><th>{labels.apartments.thStatus}</th><th>{labels.apartments.thActions}</th></tr></thead>
              <tbody>
                {filtered.map((item) => (
                  <tr key={item.id}>
                    <td><strong>{labels.apartments.apartmentLabel} {item.number}</strong><small>{labels.apartments.apartmentLabel} {item.id}</small></td>
                    <td><ActiveStatusBadge active={item.active} /></td>
                    <td>
                      <div className="row-actions">
                        <button className="secondary-button small" onClick={() => void openResidents(item)}>{labels.apartments.residentsButton}</button>
                        {canWrite && <button className="secondary-button small" onClick={() => openEdit(item)}>{labels.common.edit}</button>}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {modal === 'apartment' && (
        <Modal title={editingId ? labels.apartments.editModalTitle : labels.apartments.newModalTitle} description={labels.apartments.modalDesc} onClose={() => setModal(null)}>
          <div className="form-grid">
            <label>
              <span>{labels.apartments.numberLabel}</span>
              <input
                ref={apartmentInputRef}
                value={form.number}
                onChange={(e) => { setForm({ ...form, number: e.target.value }); if (fieldErrors.number) setFieldErrors((p) => { const n = { ...p }; delete n.number; return n; }); }}
                maxLength={LIMITS.apartmentNumber.max}
                aria-invalid={Boolean(fieldErrors.number)}
                aria-describedby={fieldErrors.number ? 'apt-number-error' : undefined}
              />
              {fieldErrors.number && <small className="field-error" id="apt-number-error" role="alert">{fieldErrors.number}</small>}
            </label>
            <label className="checkbox-field"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /><span>{labels.apartments.activeLabel}</span></label>
          </div>
          {error && <div className="form-error" role="alert">{error}</div>}
          <div className="modal-actions">
            <button className="secondary-button" onClick={() => setModal(null)}>{labels.common.cancel}</button>
            <button className="primary-button" onClick={() => void saveApartment()} disabled={saving}>{saving ? labels.common.saving : labels.common.save}</button>
          </div>
        </Modal>
      )}

      {modal === 'residents' && selected && (
        <Modal title={`${labels.apartments.residentsTitle} ${selected.number}`} description={labels.apartments.residentsDesc} width="large" onClose={() => setModal(null)}>
          <div className="resident-form-row">
            <div style={{ flex: 1 }}>
              <input
                ref={residentInputRef}
                aria-label={labels.apartments.residentNameLabel}
                value={residentForm.fullName}
                onChange={(e) => { setResidentForm({ ...residentForm, fullName: e.target.value }); if (fieldErrors.residentName) setFieldErrors((p) => { const n = { ...p }; delete n.residentName; return n; }); }}
                onBlur={handleResidentBlur}
                placeholder={labels.apartments.residentNamePlaceholder}
                maxLength={LIMITS.residentName.max}
                aria-invalid={Boolean(fieldErrors.residentName)}
                aria-describedby={fieldErrors.residentName ? 'resident-name-error' : undefined}
              />
              {fieldErrors.residentName && <small className="field-error" id="resident-name-error" role="alert">{fieldErrors.residentName}</small>}
            </div>
            <label className="checkbox-field"><input type="checkbox" checked={residentForm.active} onChange={(e) => setResidentForm({ ...residentForm, active: e.target.checked })} /><span>{labels.apartments.activeLabel}</span></label>
            <button className="primary-button" onClick={() => void saveResident()} disabled={saving}>{editingResidentId ? labels.common.update : labels.apartments.addButton}</button>
            {editingResidentId && <button className="secondary-button" onClick={() => startResident()}>{labels.apartments.cancelEdit}</button>}
          </div>
          {error && <div className="form-error" role="alert">{error}</div>}
          {residentsLoading ? <LoadingState /> : residents.length === 0 ? (
            <EmptyState title={labels.apartments.noResidents} description={labels.apartments.noResidentsDesc} />
          ) : (
            <div className="table-wrap compact">
              <table>
                <thead><tr><th>{labels.apartments.thResident}</th><th>{labels.apartments.thStatus}</th><th></th></tr></thead>
                <tbody>
                  {residents.map((r) => (
                    <tr key={r.id}>
                      <td><strong>{r.fullName}</strong></td>
                      <td><ActiveStatusBadge active={r.active} /></td>
                      <td><button className="secondary-button small" onClick={() => startResident(r)}>{labels.common.edit}</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Modal>
      )}
    </div>
  );
}
